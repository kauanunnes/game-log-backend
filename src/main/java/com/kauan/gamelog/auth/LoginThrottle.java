package com.kauan.gamelog.auth;

import com.kauan.gamelog.shared.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Limite de tentativas de login (RNF04): 5 erros com o mesmo login, vindos do mesmo IP, bloqueiam esse par por
 * 15 minutos contados do primeiro erro. A chave inclui o IP para um atacante não conseguir trancar a conta de
 * outra pessoa.
 */
@Component
class LoginThrottle {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED = 10_000;

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();

    void check(String key) {
        Failures entry = failures.get(key);
        if (entry != null && entry.blocked()) {
            throw new TooManyRequestsException(
                    "Muitas tentativas erradas. Espere alguns minutos e tente de novo.", entry.retryAfter());
        }
    }

    void failed(String key) {
        if (failures.size() >= MAX_TRACKED) {
            failures.clear();
        }
        failures.merge(key, Failures.first(), (old, first) -> old.expired() ? first : old.plusOne());
    }

    void succeeded(String key) {
        failures.remove(key);
    }

    private record Failures(int count, Instant since) {
        static Failures first() {
            return new Failures(1, Instant.now());
        }

        Failures plusOne() {
            return new Failures(count + 1, since);
        }

        boolean expired() {
            return Instant.now().isAfter(since.plus(WINDOW));
        }

        boolean blocked() {
            return count >= MAX_FAILURES && !expired();
        }

        Duration retryAfter() {
            return Duration.between(Instant.now(), since.plus(WINDOW));
        }
    }
}
