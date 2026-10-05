package com.kauan.gamelog.recommendation;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** O modelo que escolhe e explica as sugestões (3.5): o Gemini, por padrão, ou o Claude. */
@ConfigurationProperties("game-log.ai")
public record AiProperties(
        @DefaultValue("gemini") Provider provider,
        @DefaultValue("60s") Duration timeout,
        @DefaultValue Gemini gemini,
        @DefaultValue Claude claude) {

    public enum Provider {
        GEMINI,
        CLAUDE
    }

    public record Gemini(
            String apiKey,

            @DefaultValue("https://generativelanguage.googleapis.com")
            URI baseUrl,

            @DefaultValue("gemini-3.8-flash") String model) {

        /** Sem chave, as sugestões ficam só com a busca. */
        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Claude(
            String apiKey,
            @DefaultValue("https://api.anthropic.com") URI baseUrl,
            @DefaultValue("claude-opus-5-5") String model,
            @DefaultValue("medium") String effort) {

        /** Sem chave, as sugestões ficam só com a busca. */
        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
