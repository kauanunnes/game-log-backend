package com.kauan.gamelog.auth;

import java.time.Duration;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Tokens de uso único dos links por e-mail: confirmar o e-mail e redefinir a senha. */
@Repository
class AccountTokens {
    enum Purpose {
        VERIFY_EMAIL,
        RESET_PASSWORD
    }

    private final JdbcClient jdbc;

    AccountTokens(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Invalida os anteriores do mesmo tipo. @return o token, que só vai no link */
    String create(long userId, Purpose purpose, Duration ttl) {
        jdbc.sql("""
                        UPDATE account_tokens SET used_at = now()
                        WHERE user_id = :userId AND purpose = :purpose AND used_at IS NULL
                        """).param("userId", userId).param("purpose", purpose.name()).update();
        String token = Tokens.random();
        jdbc.sql("""
                        INSERT INTO account_tokens (user_id, purpose, token_hash, expires_at)
                        VALUES (:userId, :purpose, :hash, now() + :seconds * interval '1 second')
                        """)
                .param("userId", userId)
                .param("purpose", purpose.name())
                .param("hash", Tokens.hash(token))
                .param("seconds", ttl.toSeconds())
                .update();
        return token;
    }

    /** Usa o token numa instrução só, então ele vale uma vez. @return o dono, se o token vale */
    Optional<Long> consume(String token, Purpose purpose) {
        return jdbc.sql("""
                        UPDATE account_tokens SET used_at = now()
                        WHERE token_hash = :hash AND purpose = :purpose AND used_at IS NULL AND expires_at > now()
                        RETURNING user_id
                        """)
                .param("hash", Tokens.hash(token))
                .param("purpose", purpose.name())
                .query(Long.class)
                .optional();
    }

    boolean sentWithin(long userId, Purpose purpose, Duration window) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM account_tokens WHERE user_id = :userId AND purpose = :purpose
                                       AND created_at > now() - :seconds * interval '1 second')
                        """)
                .param("userId", userId)
                .param("purpose", purpose.name())
                .param("seconds", window.toSeconds())
                .query(Boolean.class)
                .single();
    }
}
