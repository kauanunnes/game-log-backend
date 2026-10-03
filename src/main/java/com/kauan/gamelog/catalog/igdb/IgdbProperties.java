package com.kauan.gamelog.catalog.igdb;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("game-log.igdb")
public record IgdbProperties(
        String clientId,
        String clientSecret,
        @DefaultValue("https://api.igdb.com/v4") URI baseUrl,
        @DefaultValue("https://id.twitch.tv/oauth2/token") URI tokenUrl,
        @DefaultValue("4") int requestsPerSecond,
        @DefaultValue("5s") Duration timeout,
        @DefaultValue("0") int bootstrapLimit) {

    /** Sem credenciais, a aplicação funciona só com o catálogo local. */
    public boolean enabled() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
