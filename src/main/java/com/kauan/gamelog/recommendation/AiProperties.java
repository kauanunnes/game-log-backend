package com.kauan.gamelog.recommendation;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** A chamada ao Claude que escolhe e explica as sugestões (3.5). */
@ConfigurationProperties("game-log.ai")
public record AiProperties(
        String apiKey,
        @DefaultValue("https://api.anthropic.com") URI baseUrl,
        @DefaultValue("claude-opus-5-5") String model,
        @DefaultValue("medium") String effort,
        @DefaultValue("60s") Duration timeout) {

    /** Sem chave, as sugestões ficam só com a busca. */
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
