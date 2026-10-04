package com.kauan.gamelog.catalog.igdb;

import com.kauan.gamelog.catalog.GameImported;
import com.kauan.gamelog.catalog.GameKind;
import com.kauan.gamelog.catalog.GameMetadata;
import com.kauan.gamelog.shared.TextNormalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Grava jogos do IGDB no catálogo: insere ou atualiza pelo {@code igdb_id}. Linhas criadas antes sem
 * {@code igdb_id} (o seed de desenvolvimento) são adotadas pelo slug.
 */
@Component
public class IgdbImporter {
    private static final Map<Integer, GameKind> KINDS = Map.of(
            0, GameKind.MAIN,
            2, GameKind.EXPANSION,
            4, GameKind.STANDALONE,
            8, GameKind.REMAKE,
            9, GameKind.REMASTER,
            10, GameKind.EXPANDED,
            11, GameKind.PORT);

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;
    private final ApplicationEventPublisher events;

    IgdbImporter(JdbcClient jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    @Transactional
    public int importGames(List<IgdbGame> games) {
        int imported = 0;
        for (IgdbGame game : games) {
            if (importGame(game).isPresent()) {
                imported++;
            }
        }
        return imported;
    }

    /** @return o id local do jogo, ou vazio se o tipo dele fica fora do catálogo */
    @Transactional
    public Optional<Long> importGame(IgdbGame game) {
        GameKind kind = KINDS.get(game.gameType());
        if (kind == null || game.slug() == null || game.name() == null) {
            return Optional.empty();
        }
        long gameId = upsertGame(game, kind);
        replaceLinks(
                "game_genres",
                "genre_id",
                gameId,
                orEmpty(game.genres()).stream().map(this::upsertGenre).toList());
        replaceLinks(
                "game_platforms",
                "platform_id",
                gameId,
                orEmpty(game.platforms()).stream().map(this::upsertPlatform).toList());
        events.publishEvent(new GameImported(gameId));
        return Optional.of(gameId);
    }

    /** Quantos jogos do catálogo vieram do IGDB. */
    long importedCount() {
        return jdbc.sql("SELECT count(*) FROM games WHERE igdb_id IS NOT NULL")
                .query(Long.class)
                .single();
    }

    /** Ids no IGDB dos jogos sincronizados há mais de {@code days} dias, dos mais antigos aos mais novos. */
    List<Long> staleIgdbIds(int days, int limit) {
        return jdbc.sql("""
                        SELECT igdb_id FROM games
                        WHERE igdb_id IS NOT NULL
                          AND (synced_at IS NULL OR synced_at < now() - make_interval(days => :days))
                        ORDER BY synced_at NULLS FIRST
                        LIMIT :limit
                        """)
                .param("days", days)
                .param("limit", limit)
                .query(Long.class)
                .list();
    }

    void markSynced(Collection<Long> igdbIds) {
        jdbc.sql("UPDATE games SET synced_at = now() WHERE igdb_id IN (:igdbIds)")
                .param("igdbIds", igdbIds)
                .update();
    }

    private long upsertGame(IgdbGame game, GameKind kind) {
        adoptBySlug("games", game.id(), game.slug());
        return jdbc.sql("""
                        INSERT INTO games (igdb_id, slug, title, title_normalized, summary, release_date, kind,
                                           cover_image_id, metadata, igdb_rating, igdb_rating_count, synced_at)
                        VALUES (:igdbId,
                                CASE WHEN EXISTS (SELECT 1 FROM games
                                                  WHERE slug = :slug AND igdb_id IS DISTINCT FROM :igdbId)
                                     THEN :slug || '-' || :igdbId ELSE :slug END,
                                :title, :titleNormalized, :summary, :releaseDate, :kind,
                                :coverImageId, CAST(:metadata AS jsonb), :rating, :ratingCount, now())
                        ON CONFLICT (igdb_id) DO UPDATE SET
                            slug = EXCLUDED.slug, title = EXCLUDED.title, title_normalized = EXCLUDED.title_normalized,
                            summary = EXCLUDED.summary, release_date = EXCLUDED.release_date, kind = EXCLUDED.kind,
                            cover_image_id = EXCLUDED.cover_image_id, metadata = EXCLUDED.metadata,
                            igdb_rating = EXCLUDED.igdb_rating, igdb_rating_count = EXCLUDED.igdb_rating_count,
                            synced_at = now(), updated_at = now()
                        RETURNING id
                        """)
                .param("igdbId", game.id())
                .param("slug", game.slug())
                .param("title", game.name())
                .param("titleNormalized", TextNormalizer.normalize(game.name()))
                .param("summary", game.summary())
                .param("releaseDate", releaseDate(game))
                .param("kind", kind.name())
                .param(
                        "coverImageId",
                        game.cover() == null ? null : game.cover().imageId())
                .param("metadata", metadata(game))
                .param("rating", game.totalRating())
                .param("ratingCount", game.totalRatingCount())
                .query(Long.class)
                .single();
    }

    private long upsertGenre(IgdbGame.Named genre) {
        adoptBySlug("genres", genre.id(), genre.slug());
        return jdbc.sql("""
                        INSERT INTO genres (igdb_id, name, slug) VALUES (:igdbId, :name, :slug)
                        ON CONFLICT (igdb_id) DO UPDATE SET name = EXCLUDED.name, slug = EXCLUDED.slug
                        RETURNING id
                        """)
                .param("igdbId", genre.id())
                .param("name", genre.name())
                .param("slug", genre.slug())
                .query(Long.class)
                .single();
    }

    private long upsertPlatform(IgdbGame.Platform platform) {
        adoptBySlug("platforms", platform.id(), platform.slug());
        return jdbc.sql("""
                        INSERT INTO platforms (igdb_id, name, abbreviation, slug)
                        VALUES (:igdbId, :name, :abbreviation, :slug)
                        ON CONFLICT (igdb_id) DO UPDATE SET
                            name = EXCLUDED.name, abbreviation = EXCLUDED.abbreviation, slug = EXCLUDED.slug
                        RETURNING id
                        """)
                .param("igdbId", platform.id())
                .param("name", platform.name())
                .param("abbreviation", platform.abbreviation())
                .param("slug", platform.slug())
                .query(Long.class)
                .single();
    }

    /** {@code table} é sempre uma constante desta classe, nunca entrada de usuário. */
    private void adoptBySlug(String table, long igdbId, String slug) {
        jdbc.sql("UPDATE " + table + " SET igdb_id = :igdbId WHERE slug = :slug AND igdb_id IS NULL"
                        + " AND NOT EXISTS (SELECT 1 FROM " + table + " WHERE igdb_id = :igdbId)")
                .param("igdbId", igdbId)
                .param("slug", slug)
                .update();
    }

    private void replaceLinks(String table, String column, long gameId, List<Long> ids) {
        jdbc.sql("DELETE FROM " + table + " WHERE game_id = :gameId")
                .param("gameId", gameId)
                .update();
        for (Long id : ids) {
            jdbc.sql("INSERT INTO " + table + " (game_id, " + column + ") VALUES (:gameId, :id) ON CONFLICT DO NOTHING")
                    .param("gameId", gameId)
                    .param("id", id)
                    .update();
        }
    }

    private static LocalDate releaseDate(IgdbGame game) {
        return game.firstReleaseDate() == null
                ? null
                : LocalDate.ofInstant(Instant.ofEpochSecond(game.firstReleaseDate()), ZoneOffset.UTC);
    }

    private static String metadata(IgdbGame game) {
        return JSON.writeValueAsString(new GameMetadata(
                names(game.themes()),
                names(game.keywords()),
                names(game.gameModes()),
                names(game.playerPerspectives()),
                companies(game, IgdbGame.InvolvedCompany::developer),
                companies(game, IgdbGame.InvolvedCompany::publisher),
                names(game.franchises()),
                orEmpty(game.similarGames()),
                names(game.collections()),
                game.parentGame() != null ? game.parentGame() : game.versionParent()));
    }

    private static List<String> names(List<IgdbGame.Named> items) {
        return orEmpty(items).stream().map(IgdbGame.Named::name).toList();
    }

    private static List<String> companies(IgdbGame game, Predicate<IgdbGame.InvolvedCompany> role) {
        return orEmpty(game.involvedCompanies()).stream()
                .filter(involved -> involved.company() != null && role.test(involved))
                .map(involved -> involved.company().name())
                .distinct()
                .toList();
    }

    private static <T> List<T> orEmpty(List<T> items) {
        return items == null ? List.of() : items;
    }
}
