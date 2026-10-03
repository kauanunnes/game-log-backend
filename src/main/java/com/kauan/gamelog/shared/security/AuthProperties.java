package com.kauan.gamelog.shared.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param jwtPrivateKey chave RSA privada em PEM (PKCS#8); a pública é derivada dela. Vazia, a aplicação gera
 *     uma chave na subida, e os access tokens deixam de valer quando ela reinicia
 */
@ConfigurationProperties("game-log.auth")
public record AuthProperties(
        String jwtPrivateKey,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("30d") Duration refreshTokenTtl,
        @DefaultValue("12") int bcryptStrength) {}
