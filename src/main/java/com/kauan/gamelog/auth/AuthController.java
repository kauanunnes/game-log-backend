package com.kauan.gamelog.auth;

import static com.kauan.gamelog.shared.WebConfig.API_PREFIX;

import com.kauan.gamelog.auth.AuthService.Session;
import com.kauan.gamelog.auth.dto.LoginRequest;
import com.kauan.gamelog.auth.dto.RegisterRequest;
import com.kauan.gamelog.auth.dto.TokenResponse;
import com.kauan.gamelog.shared.security.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O refresh token vive num cookie {@code HttpOnly} que o navegador só manda para {@code /api/v1/auth}: o
 * JavaScript não lê o cookie, e as outras rotas não o recebem.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final AuthProperties properties;

    AuthController(AuthService authService, AuthProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        Session session = authService.register(request);
        return ResponseEntity.created(URI.create(API_PREFIX + "/me"))
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), properties.refreshTokenTtl()))
                .body(session.token());
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return withCookie(authService.login(request, http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return withCookie(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO))
                .build();
    }

    private ResponseEntity<TokenResponse> withCookie(Session session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), properties.refreshTokenTtl()))
                .body(session.token());
    }

    private static String refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(API_PREFIX + "/auth")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
