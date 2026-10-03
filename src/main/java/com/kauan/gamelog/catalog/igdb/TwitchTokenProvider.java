package com.kauan.gamelog.catalog.igdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/** Token de app da Twitch (client credentials), guardado em memória até perto de expirar. */
final class TwitchTokenProvider {
    private final IgdbProperties properties;
    private final RestClient restClient;
    private String token;
    private Instant expiresAt = Instant.EPOCH;

    TwitchTokenProvider(IgdbProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    synchronized String token() {
        if (token == null || Instant.now().isAfter(expiresAt)) {
            refresh();
        }
        return token;
    }

    synchronized void invalidate() {
        token = null;
    }

    private void refresh() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("grant_type", "client_credentials");

        TokenResponse response = restClient
                .post()
                .uri(properties.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        token = response.accessToken();
        expiresAt = Instant.now().plusSeconds(Math.max(0, response.expiresIn() - 60));
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {}
}
