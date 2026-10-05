package com.kauan.gamelog.recommendation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Quem escolhe as sugestões vem de {@code game-log.ai.provider}: o Gemini, por padrão, ou o Claude. */
@Configuration
class AiConfig {

    @Bean
    Curator curator(AiProperties properties) {
        return switch (properties.provider()) {
            case GEMINI -> new GeminiCurator(properties.gemini(), properties.timeout());
            case CLAUDE -> new ClaudeCurator(properties.claude(), properties.timeout());
        };
    }
}
