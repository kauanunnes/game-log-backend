package com.kauan.gamelog.recommendation;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.beta.messages.BetaJsonOutputFormat;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaTextBlock;
import com.anthropic.models.beta.messages.MessageCreateParams;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O Claude escolhe e explica as sugestões, com {@code game-log.ai.provider=claude}. Se o classificador de segurança
 * recusar, a API tenta de novo no modelo que ela indica para o caso.
 *
 * <p>O schema vai como mapa, e o JSON é lido em {@link ModelCurator}: o SDK geraria o schema a partir de classes com a
 * biblioteca victools 4, mas o Spring AI fixa a 5, que não é compatível.
 */
class ClaudeCurator extends ModelCurator {
    private static final Logger log = LoggerFactory.getLogger(ClaudeCurator.class);
    private static final String SERVER_FALLBACK = "server-side-fallback-2026-07-01";

    private static final BetaJsonOutputFormat.Schema JSON_SCHEMA = BetaJsonOutputFormat.Schema.builder()
            .additionalProperties(SCHEMA.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonValue.from(entry.getValue()))))
            .build();

    private final AiProperties.Claude properties;
    private final AnthropicClient client;

    ClaudeCurator(AiProperties.Claude properties, Duration timeout) {
        this.properties = properties;
        this.client = properties.enabled()
                ? AnthropicOkHttpClient.builder()
                        .apiKey(properties.apiKey())
                        .baseUrl(properties.baseUrl().toString())
                        .timeout(timeout)
                        .build()
                : null;
    }

    @Override
    public String name() {
        return "Claude";
    }

    @Override
    public boolean enabled() {
        return client != null;
    }

    @Override
    protected String ask(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(16_000L)
                .system(SYSTEM)
                .addBeta(SERVER_FALLBACK)
                .fallbacksDefault()
                .outputConfig(BetaOutputConfig.builder()
                        .effort(BetaOutputConfig.Effort.of(properties.effort()))
                        .format(BetaJsonOutputFormat.builder()
                                .schema(JSON_SCHEMA)
                                .build())
                        .build())
                .addUserMessage(prompt)
                .build();
        BetaMessage response = client.beta().messages().create(params);
        if (response.stopReason().map(BetaStopReason.REFUSAL::equals).orElse(false)) {
            log.warn("O Claude não escolheu as sugestões (recusa): {}", response.stopDetails());
            return null;
        }
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(BetaTextBlock::text)
                .collect(Collectors.joining());
    }
}
