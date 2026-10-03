package com.kauan.gamelog.catalog.igdb;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClientException;

class IgdbClientTests {
    private static final String TOKEN = "{\"access_token\": \"%s\", \"expires_in\": 3600, \"token_type\": \"bearer\"}";
    private static final String HADES = "[{\"id\": 113112, \"name\": \"Hades\", \"slug\": \"hades--1\"}]";

    @RegisterExtension
    static WireMockExtension igdb = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private IgdbClient client;

    @BeforeEach
    void setUp() {
        client = client(Duration.ofSeconds(2));
        igdb.stubFor(post("/oauth2/token")
                .inScenario("token")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson(TOKEN.formatted("token-1")))
                .willSetStateTo("renewed"));
        igdb.stubFor(post("/oauth2/token")
                .inScenario("token")
                .whenScenarioStateIs("renewed")
                .willReturn(okJson(TOKEN.formatted("token-2"))));
    }

    @Test
    void sendsTheCredentialsAndReusesTheToken() {
        igdb.stubFor(post("/games").willReturn(okJson(HADES)));

        assertThat(client.search("hades", 5)).extracting(IgdbGame::name).containsExactly("Hades");
        client.search("hades", 5);

        igdb.verify(1, postRequestedFor(urlEqualTo("/oauth2/token")));
        igdb.verify(
                2,
                postRequestedFor(urlEqualTo("/games"))
                        .withHeader("Client-ID", equalTo("client-id"))
                        .withHeader("Authorization", equalTo("Bearer token-1"))
                        .withRequestBody(
                                containing("search \"hades\"; where game_type = (0,2,4,8,9,10,11); limit 5;")));
    }

    @Test
    void reportsCredentialsRejectedByTwitch() {
        igdb.stubFor(post("/oauth2/token")
                .willReturn(jsonResponse("{\"status\": 400, \"message\": \"invalid client\"}", 400)));

        assertThatThrownBy(client::checkCredentials).hasMessageContaining("invalid client");
    }

    @Test
    void renewsTheTokenWhenTheApiRejectsIt() {
        igdb.stubFor(post("/games")
                .inScenario("games")
                .whenScenarioStateIs(STARTED)
                .willReturn(unauthorized())
                .willSetStateTo("authorized"));
        igdb.stubFor(post("/games")
                .inScenario("games")
                .whenScenarioStateIs("authorized")
                .willReturn(okJson(HADES)));

        assertThat(client.search("hades", 5)).hasSize(1);

        igdb.verify(2, postRequestedFor(urlEqualTo("/oauth2/token")));
        igdb.verify(1, postRequestedFor(urlEqualTo("/games")).withHeader("Authorization", equalTo("Bearer token-2")));
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 503})
    void retriesTemporaryFailures(int status) {
        igdb.stubFor(post("/games")
                .inScenario("games")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(status))
                .willSetStateTo("back"));
        igdb.stubFor(
                post("/games").inScenario("games").whenScenarioStateIs("back").willReturn(okJson(HADES)));

        assertThat(client.search("hades", 5)).hasSize(1);
        igdb.verify(2, postRequestedFor(urlEqualTo("/games")));
    }

    @Test
    void givesUpOnTimeoutWithoutRetrying() {
        igdb.stubFor(post("/games").willReturn(okJson(HADES).withFixedDelay(1_000)));
        IgdbClient impatient = client(Duration.ofMillis(200));

        assertThatThrownBy(() -> impatient.search("hades", 5))
                .isInstanceOf(RestClientException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
        igdb.verify(1, postRequestedFor(urlEqualTo("/games")));
    }

    @Test
    void escapesQuotesInTheSearchText() {
        igdb.stubFor(post("/games").willReturn(okJson("[]")));

        client.search("say \"hi\"", 5);

        igdb.verify(postRequestedFor(urlEqualTo("/games")).withRequestBody(containing("search \"say \\\"hi\\\"\";")));
    }

    private static IgdbClient client(Duration timeout) {
        return new IgdbClient(new IgdbProperties(
                "client-id",
                "client-secret",
                URI.create(igdb.baseUrl()),
                URI.create(igdb.baseUrl() + "/oauth2/token"),
                100,
                timeout,
                0));
    }
}
