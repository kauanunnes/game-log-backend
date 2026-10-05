package com.kauan.gamelog.recommendation;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.FinishReason;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.Part;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O Gemini escolhe e explica as sugestões, o padrão de {@code game-log.ai.provider}. A resposta sai em JSON, presa ao
 * schema de {@link ModelCurator}.
 */
class GeminiCurator extends ModelCurator {
    private static final Logger log = LoggerFactory.getLogger(GeminiCurator.class);
    /** Tenta de novo em 429 e 5xx, com espera crescente: no nível gratuito, o limite por minuto é baixo. */
    private static final int ATTEMPTS = 3;

    private static final GenerateContentConfig CONFIG = GenerateContentConfig.builder()
            .systemInstruction(Content.fromParts(Part.fromText(SYSTEM)))
            .responseMimeType("application/json")
            .responseJsonSchema(SCHEMA)
            .build();

    private final AiProperties.Gemini properties;
    private final Client client;

    GeminiCurator(AiProperties.Gemini properties, Duration timeout) {
        this.properties = properties;
        this.client = properties.enabled()
                ? Client.builder()
                        .apiKey(properties.apiKey())
                        // A API do Gemini, e não a Vertex AI, mesmo com GOOGLE_GENAI_USE_VERTEXAI no ambiente
                        .vertexAI(false)
                        .httpOptions(HttpOptions.builder()
                                .baseUrl(properties.baseUrl().toString())
                                .timeout(Math.toIntExact(timeout.toMillis()))
                                .retryOptions(HttpRetryOptions.builder().attempts(ATTEMPTS))
                                .build())
                        .build()
                : null;
    }

    @Override
    public String name() {
        return "Gemini";
    }

    @Override
    public boolean enabled() {
        return client != null;
    }

    @Override
    protected String ask(String prompt) {
        GenerateContentResponse response = client.models.generateContent(properties.model(), prompt, CONFIG);
        // Um pedido bloqueado volta sem candidatos, e o motivo fica em promptFeedback
        FinishReason finish = response.finishReason();
        if (finish.knownEnum() != FinishReason.Known.STOP) {
            log.warn(
                    "O Gemini não escolheu as sugestões ({}): {}",
                    finish,
                    response.promptFeedback().orElse(null));
            return null;
        }
        return response.text();
    }
}
