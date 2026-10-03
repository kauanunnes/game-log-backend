package com.kauan.gamelog.auth;

import com.kauan.gamelog.auth.dto.TokenResponse;
import com.kauan.gamelog.shared.security.AuthProperties;
import com.kauan.gamelog.user.AuthUser;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Access token de curta duração: {@code sub} é o id do usuário, e {@code roles} vira a autorização. */
@Component
class AccessTokens {
    private final JwtEncoder encoder;
    private final AuthProperties properties;

    AccessTokens(JwtEncoder encoder, AuthProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    TokenResponse issue(AuthUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("game-log")
                .subject(Long.toString(user.id()))
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim("username", user.username())
                .claim("roles", List.of(user.role().name()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, properties.accessTokenTtl().toSeconds());
    }
}
