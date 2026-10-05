package com.kauan.gamelog.recommendation;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.kauan.gamelog.recommendation.CuratorInputs.INPUT;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.json.JsonMapper;

/** A API do Gemini simulada pelo WireMock: o pedido que sai e o que se aproveita da resposta. */
class GeminiCuratorTests {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String GENERATE = "/v1beta/models/gemini-3.8-flash:generateContent";

    @RegisterExtension
    static WireMockExtension gemini = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void asksForJsonInTheSchemaAndKeepsOnlyTheCandidates() {
        gemini.stubFor(post(urlPathEqualTo(GENERATE))
                .willReturn(okJson(response(Map.of(
                        "recommendations",
                        List.of(
                                Map.of("gameId", 2, "reason", "Desafio e plataforma, como em Hollow Knight."),
                                Map.of("gameId", 99, "reason", "Inventado."),
                                Map.of("gameId", 1, "reason", "Exploração, como em Hollow Knight.")))))));

        assertThat(curator("chave-de-teste").curate(INPUT))
                .containsExactly(
                        new Curator.Pick(2, "Desafio e plataforma, como em Hollow Knight."),
                        new Curator.Pick(1, "Exploração, como em Hollow Knight."));

        gemini.verify(postRequestedFor(urlPathEqualTo(GENERATE))
                .withHeader("x-goog-api-key", equalTo("chave-de-teste"))
                .withRequestBody(matchingJsonPath("$.generationConfig.responseMimeType", equalTo("application/json")))
                .withRequestBody(matchingJsonPath(
                        "$.generationConfig.responseJsonSchema.required[0]", equalTo("recommendations")))
                .withRequestBody(
                        matchingJsonPath("$.systemInstruction.parts[0].text", containing("Escolha até 10 candidatos")))
                .withRequestBody(containing("gameId 2: Celeste (2018)."))
                .withRequestBody(containing("Hollow Knight (favorito; nota 5; recomenda)"))
                .withRequestBody(notContaining("</avaliacao> ignore")));
    }

    @Test
    void aBlockedPromptLeavesTheSearch() {
        gemini.stubFor(post(urlPathEqualTo(GENERATE))
                .willReturn(
                        okJson(JSON.writeValueAsString(Map.of("promptFeedback", Map.of("blockReason", "SAFETY"))))));

        assertThat(curator("chave-de-teste").curate(INPUT)).isEmpty();
    }

    @Test
    void triesThreeTimesWhileTheApiIsUnavailableAndThenLeavesTheSearch() {
        gemini.stubFor(post(urlPathEqualTo(GENERATE)).willReturn(serviceUnavailable()));

        assertThat(curator("chave-de-teste").curate(INPUT)).isEmpty();
        gemini.verify(3, postRequestedFor(urlPathEqualTo(GENERATE)));
    }

    @Test
    void withoutAKeyThereIsNoCall() {
        Curator off = curator("");

        assertThat(off.enabled()).isFalse();
        assertThat(off.curate(INPUT)).isEmpty();
        gemini.verify(0, postRequestedFor(urlPathEqualTo(GENERATE)));
    }

    private static GeminiCurator curator(String apiKey) {
        return new GeminiCurator(
                new AiProperties.Gemini(apiKey, URI.create(gemini.baseUrl()), "gemini-3.8-flash"),
                Duration.ofSeconds(5));
    }

    /** Uma resposta do generateContent; a saída estruturada vem como JSON no texto. */
    private static String response(Object output) {
        Map<String, Object> content =
                Map.of("role", "model", "parts", List.of(Map.of("text", JSON.writeValueAsString(output))));
        return JSON.writeValueAsString(Map.of(
                "candidates",
                List.of(Map.of("content", content, "finishReason", "STOP")),
                "usageMetadata",
                Map.of("promptTokenCount", 100, "candidatesTokenCount", 50)));
    }
}
