package com.kauan.gamelog.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Refresh tokens opacos. O banco guarda só o SHA-256; a rotação marca o token usado e cria outro na mesma
 * família, e um token já usado que reaparece derruba a família inteira.
 */
@Repository
class RefreshTokens {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcClient jdbc;

    RefreshTokens(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    record Owner(long userId, UUID familyId) {}

    /** @return o valor que vai no cookie */
    String create(long userId, UUID familyId, Duration ttl) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.sql("""
                        INSERT INTO refresh_tokens (user_id, token_hash, family_id, expires_at)
                        VALUES (:userId, :hash, :familyId, now() + :seconds * interval '1 second')
                        """)
                .param("userId", userId)
                .param("hash", hash(token))
                .param("familyId", familyId)
                .param("seconds", ttl.toSeconds())
                .update();
        return token;
    }

    /** Marca o token como usado numa única instrução, então duas trocas simultâneas não passam juntas. */
    Optional<Owner> consume(String token) {
        return jdbc.sql("""
                        UPDATE refresh_tokens SET revoked_at = now()
                        WHERE token_hash = :hash AND revoked_at IS NULL AND expires_at > now()
                        RETURNING user_id, family_id
                        """)
                .param("hash", hash(token))
                .query((rs, row) -> new Owner(rs.getLong("user_id"), rs.getObject("family_id", UUID.class)))
                .optional();
    }

    Optional<UUID> familyOf(String token) {
        return jdbc.sql("SELECT family_id FROM refresh_tokens WHERE token_hash = :hash")
                .param("hash", hash(token))
                .query(UUID.class)
                .optional();
    }

    /** Família de um token que já foi usado (ou revogado): se ele reaparece, alguém o copiou. */
    Optional<UUID> familyOfUsed(String token) {
        return jdbc.sql("SELECT family_id FROM refresh_tokens WHERE token_hash = :hash AND revoked_at IS NOT NULL")
                .param("hash", hash(token))
                .query(UUID.class)
                .optional();
    }

    void revokeFamily(UUID familyId) {
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = now() WHERE family_id = :familyId AND revoked_at IS NULL")
                .param("familyId", familyId)
                .update();
    }

    void revokeAll(long userId) {
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = now() WHERE user_id = :userId AND revoked_at IS NULL")
                .param("userId", userId)
                .update();
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
