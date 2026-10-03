package com.kauan.gamelog.library;

import com.kauan.gamelog.library.dto.StatsDTO;
import com.kauan.gamelog.library.dto.StatsDTO.NamedCount;
import com.kauan.gamelog.library.dto.StatsDTO.RatingBucket;
import com.kauan.gamelog.library.dto.StatsDTO.Spending;
import com.kauan.gamelog.library.dto.StatsDTO.StoreTotal;
import com.kauan.gamelog.library.dto.StatsDTO.YearCount;
import com.kauan.gamelog.library.dto.StatsDTO.YearTotal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Estatísticas calculadas na consulta; pré-calcular fica para quando o volume pedir (ver arquitetura). */
@Repository
class LibraryStats {
    private final JdbcClient jdbc;

    LibraryStats(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    StatsDTO of(long userId, Integer year) {
        Map<String, Object> params = new HashMap<>(Map.of("userId", userId));
        String finished = "";
        String acquired = "";
        if (year != null) {
            params.put("from", LocalDate.of(year, 1, 1));
            params.put("to", LocalDate.of(year + 1, 1, 1));
            finished = " AND e.finished_on >= :from AND e.finished_on < :to";
            acquired = " AND e.acquired_on >= :from AND e.acquired_on < :to";
        }
        String mine = " FROM library_entries e WHERE e.user_id = :userId" + finished;

        Map<EntryStatus, Long> byStatus = new EnumMap<>(EntryStatus.class);
        for (EntryStatus status : EntryStatus.values()) {
            byStatus.put(status, 0L);
        }
        jdbc.sql("SELECT e.status, count(*) AS total" + mine + " GROUP BY e.status")
                .params(params)
                .query((RowCallbackHandler)
                        rs -> byStatus.put(EntryStatus.valueOf(rs.getString("status")), rs.getLong("total")));

        List<YearCount> finishedByYear = jdbc.sql(
                        "SELECT extract(year FROM e.finished_on)::int AS year, count(*) AS total" + mine
                                + " AND e.finished_on IS NOT NULL GROUP BY 1 ORDER BY 1")
                .params(params)
                .query((rs, row) -> new YearCount(rs.getInt("year"), rs.getLong("total")))
                .list();

        List<NamedCount> byGenre = jdbc.sql(
                        """
                        SELECT g.id, g.name, count(*) AS total
                        FROM library_entries e
                        JOIN game_genres gg ON gg.game_id = e.game_id
                        JOIN genres g ON g.id = gg.genre_id
                        WHERE e.user_id = :userId AND e.status <> 'WISHLIST'""" + finished + " GROUP BY g.id, g.name ORDER BY total DESC, g.name LIMIT 10")
                .params(params)
                .query((rs, row) -> new NamedCount(rs.getLong("id"), rs.getString("name"), rs.getLong("total")))
                .list();

        List<NamedCount> byPlatform = jdbc.sql(
                        """
                        SELECT p.id, p.name, count(*) AS total
                        FROM library_entries e JOIN platforms p ON p.id = e.played_platform_id
                        WHERE e.user_id = :userId""" + finished + " GROUP BY p.id, p.name ORDER BY total DESC, p.name LIMIT 10")
                .params(params)
                .query((rs, row) -> new NamedCount(rs.getLong("id"), rs.getString("name"), rs.getLong("total")))
                .list();

        Map<Double, Long> ratings = new HashMap<>();
        jdbc.sql("SELECT floor(e.rating * 2) / 2 AS stars, count(*) AS total" + mine
                        + " AND e.rating IS NOT NULL GROUP BY 1")
                .params(params)
                .query((RowCallbackHandler) rs -> ratings.put(rs.getDouble("stars"), rs.getLong("total")));
        List<RatingBucket> distribution = IntStream.rangeClosed(0, 10)
                .mapToObj(half -> new RatingBucket(half / 2.0, ratings.getOrDefault(half / 2.0, 0L)))
                .toList();

        record Totals(BigDecimal averageRating, long hoursPlayed) {}
        Totals totals = jdbc.sql(
                        "SELECT round(avg(e.rating), 2) AS average, coalesce(sum(e.hours_played), 0) AS hours" + mine)
                .params(params)
                .query((rs, row) -> new Totals(rs.getBigDecimal("average"), rs.getLong("hours")))
                .single();

        return new StatsDTO(
                year,
                byStatus.values().stream().mapToLong(Long::longValue).sum(),
                byStatus,
                finishedByYear,
                byGenre,
                byPlatform,
                distribution,
                totals.averageRating(),
                totals.hoursPlayed(),
                spending(params, acquired));
    }

    private List<Spending> spending(Map<String, Object> params, String acquired) {
        String purchases = " FROM library_entries e LEFT JOIN stores s ON s.id = e.store_id"
                + " WHERE e.user_id = :userId AND e.price_paid IS NOT NULL" + acquired;

        Map<String, List<StoreTotal>> byStore = new LinkedHashMap<>();
        jdbc.sql("SELECT e.currency, s.id, s.name, sum(e.price_paid) AS total" + purchases
                        + " GROUP BY e.currency, s.id, s.name ORDER BY total DESC, s.name")
                .params(params)
                .query((RowCallbackHandler) rs -> byStore.computeIfAbsent(
                                rs.getString("currency"), c -> new ArrayList<>())
                        .add(new StoreTotal(
                                rs.getObject("id", Long.class), rs.getString("name"), rs.getBigDecimal("total"))));

        Map<String, List<YearTotal>> byYear = new LinkedHashMap<>();
        jdbc.sql("SELECT e.currency, extract(year FROM e.acquired_on)::int AS year, sum(e.price_paid) AS total"
                        + purchases + " AND e.acquired_on IS NOT NULL GROUP BY 1, 2 ORDER BY 2")
                .params(params)
                .query((RowCallbackHandler)
                        rs -> byYear.computeIfAbsent(rs.getString("currency"), c -> new ArrayList<>())
                                .add(new YearTotal(rs.getInt("year"), rs.getBigDecimal("total"))));

        return jdbc.sql("SELECT e.currency, sum(e.price_paid) AS total, count(*) AS purchases" + purchases
                        + " GROUP BY e.currency ORDER BY e.currency")
                .params(params)
                .query((rs, row) -> {
                    String currency = rs.getString("currency");
                    return new Spending(
                            currency,
                            rs.getBigDecimal("total"),
                            rs.getLong("purchases"),
                            byStore.getOrDefault(currency, List.of()),
                            byYear.getOrDefault(currency, List.of()));
                })
                .list();
    }
}
