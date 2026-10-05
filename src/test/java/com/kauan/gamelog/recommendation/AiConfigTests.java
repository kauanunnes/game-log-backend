package com.kauan.gamelog.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** O modelo vem de game-log.ai.provider, e cada um só liga com a própria chave. */
class AiConfigTests {
    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(Setup.class);

    @Test
    void geminiByDefault() {
        context.withPropertyValues("game-log.ai.gemini.api-key=chave", "game-log.ai.claude.api-key=outra")
                .run(app -> {
                    Curator curator = app.getBean(Curator.class);
                    assertThat(curator).isInstanceOf(GeminiCurator.class);
                    assertThat(curator.enabled()).isTrue();
                });
    }

    @Test
    void claudeWhenChosen() {
        context.withPropertyValues("game-log.ai.provider=claude", "game-log.ai.claude.api-key=chave")
                .run(app -> {
                    Curator curator = app.getBean(Curator.class);
                    assertThat(curator).isInstanceOf(ClaudeCurator.class);
                    assertThat(curator.enabled()).isTrue();
                });
    }

    @Test
    void theChosenModelWithoutItsOwnKeyStaysOff() {
        context.withPropertyValues("game-log.ai.claude.api-key=chave")
                .run(app -> assertThat(app.getBean(Curator.class).enabled()).isFalse());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiProperties.class)
    @Import(AiConfig.class)
    static class Setup {}
}
