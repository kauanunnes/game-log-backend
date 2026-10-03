package com.kauan.gamelog.catalog.igdb;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/** Consultas à API do IGDB (linguagem Apicalypse), com limite de taxa e novas tentativas. */
@Component
public class IgdbClient {
    static final String FIELDS = "fields name, slug, summary, first_release_date, game_type, cover.image_id,"
            + " genres.name, genres.slug, platforms.name, platforms.abbreviation, platforms.slug,"
            + " themes.name, keywords.name, game_modes.name, player_perspectives.name, franchises.name,"
            + " involved_companies.company.name, involved_companies.developer, involved_companies.publisher,"
            + " total_rating, total_rating_count, similar_games; ";

    /** Jogo principal, expansão, expansão independente, remake, remaster, jogo expandido e port. */
    static final String SUPPORTED_TYPES = "game_type = (0,2,4,8,9,10,11)";

    private static final ParameterizedTypeReference<List<IgdbGame>> GAME_LIST = new ParameterizedTypeReference<>() {};

    private final IgdbProperties properties;
    private final RestClient restClient;
    private final TwitchTokenProvider tokens;
    private final IgdbRateLimiter rateLimiter;
    private final RetryTemplate retry;

    IgdbClient(IgdbProperties properties) {
        // HttpURLConnection: bloqueante e sem threads próprias, o bastante para poucas chamadas por segundo.
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.properties = properties;
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(properties.baseUrl().toString())
                .build();
        this.tokens = new TwitchTokenProvider(properties, restClient);
        this.rateLimiter = new IgdbRateLimiter(properties.requestsPerSecond());
        this.retry = new RetryTemplate(RetryPolicy.builder()
                .maxRetries(2)
                .delay(Duration.ofMillis(500))
                .multiplier(2)
                .predicate(IgdbClient::isTransient)
                .build());
    }

    /** Pede um token à Twitch; lança exceção se ela recusar as credenciais. */
    void checkCredentials() {
        tokens.token();
    }

    public List<IgdbGame> search(String text, int limit) {
        return query(FIELDS + "search \"" + escape(text) + "\"; where " + SUPPORTED_TYPES + "; limit " + limit + ";");
    }

    public Optional<IgdbGame> findById(long id) {
        return query(FIELDS + "where id = " + id + ";").stream().findFirst();
    }

    public List<IgdbGame> popular(int limit, int offset) {
        return query(FIELDS + "where " + SUPPORTED_TYPES + " & total_rating_count > 0;"
                + " sort total_rating_count desc; limit " + limit + "; offset " + offset + ";");
    }

    private List<IgdbGame> query(String body) {
        List<IgdbGame> games = retry.invoke(() -> {
            rateLimiter.acquire();
            try {
                return restClient
                        .post()
                        .uri("/games")
                        .header("Client-ID", properties.clientId())
                        .headers(headers -> headers.setBearerAuth(tokens.token()))
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(body)
                        .retrieve()
                        .body(GAME_LIST);
            } catch (HttpClientErrorException.Unauthorized e) {
                tokens.invalidate();
                throw e;
            }
        });
        return games == null ? List.of() : games;
    }

    /**
     * Repete 401 (a nova tentativa sai com outro token), 429 e 5xx. Timeout e erro de rede não repetem,
     * para uma busca não ficar presa esperando o IGDB.
     */
    private static boolean isTransient(Throwable error) {
        return error instanceof HttpServerErrorException
                || error instanceof HttpClientErrorException.TooManyRequests
                || error instanceof HttpClientErrorException.Unauthorized;
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
