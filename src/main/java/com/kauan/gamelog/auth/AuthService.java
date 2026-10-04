package com.kauan.gamelog.auth;

import com.kauan.gamelog.auth.dto.LoginRequest;
import com.kauan.gamelog.auth.dto.RegisterRequest;
import com.kauan.gamelog.auth.dto.TokenResponse;
import com.kauan.gamelog.shared.UnauthorizedException;
import com.kauan.gamelog.shared.security.AuthProperties;
import com.kauan.gamelog.user.AuthUser;
import com.kauan.gamelog.user.PasswordChanged;
import com.kauan.gamelog.user.UserService;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserService users;
    private final AccessTokens accessTokens;
    private final RefreshTokens refreshTokens;
    private final LoginThrottle throttle;
    private final AuthProperties properties;
    private final AccountEmails emails;

    AuthService(
            UserService users,
            AccessTokens accessTokens,
            RefreshTokens refreshTokens,
            LoginThrottle throttle,
            AuthProperties properties,
            AccountEmails emails) {
        this.users = users;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.throttle = throttle;
        this.properties = properties;
        this.emails = emails;
    }

    /** O access token vai no corpo da resposta; o refresh token, num cookie. */
    public record Session(TokenResponse token, String refreshToken) {}

    @Transactional
    public Session register(RegisterRequest request) {
        AuthUser user = users.register(request.username(), request.email(), request.password());
        emails.sendVerification(user.id());
        return start(user);
    }

    @Transactional
    public Session login(LoginRequest request, String clientIp) {
        String key = clientIp + "|" + request.login().trim().toLowerCase(Locale.ROOT);
        throttle.check(key);
        Optional<AuthUser> user = users.authenticate(request.login(), request.password());
        if (user.isEmpty()) {
            throttle.failed(key);
            throw new UnauthorizedException("Username, e-mail ou senha incorretos.");
        }
        throttle.succeeded(key);
        return start(user.get());
    }

    /** Rotação: o token usado é revogado e outro da mesma família toma o lugar dele. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Session refresh(String refreshToken) {
        if (refreshToken == null) {
            throw sessionExpired();
        }
        Optional<RefreshTokens.Owner> owner = refreshTokens.consume(refreshToken);
        if (owner.isEmpty()) {
            // Token já usado reapareceu: alguém o copiou. Derruba a sessão inteira (e o commit precisa acontecer).
            refreshTokens.familyOfUsed(refreshToken).ifPresent(refreshTokens::revokeFamily);
            throw sessionExpired();
        }
        AuthUser user = users.findAuthUser(owner.get().userId()).orElseThrow(AuthService::sessionExpired);
        String next = refreshTokens.create(user.id(), owner.get().familyId(), properties.refreshTokenTtl());
        return new Session(accessTokens.issue(user), next);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokens.familyOf(refreshToken).ifPresent(refreshTokens::revokeFamily);
        }
    }

    @EventListener
    void onPasswordChanged(PasswordChanged event) {
        refreshTokens.revokeAll(event.userId());
    }

    private Session start(AuthUser user) {
        String refreshToken = refreshTokens.create(user.id(), UUID.randomUUID(), properties.refreshTokenTtl());
        return new Session(accessTokens.issue(user), refreshToken);
    }

    private static UnauthorizedException sessionExpired() {
        return new UnauthorizedException("Sua sessão expirou. Entre de novo.");
    }
}
