package com.kauan.gamelog.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kauan.gamelog.library.EntryStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Estatísticas da biblioteca (RF43). Com {@code year}, contam os jogos terminados no ano e, nos gastos, as compras
 * feitas no ano.
 *
 * @param byGenre e {@code byPlatform}: os 10 primeiros; gêneros ignoram a lista de desejos, plataforma é onde jogou
 * @param ratingDistribution uma faixa a cada meia estrela; 4,75 conta na faixa 4,5
 * @param spending por moeda, sem conversão (RN05); ausente quando o dono esconde os gastos
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StatsDTO(
        Integer year,
        long total,
        Map<EntryStatus, Long> byStatus,
        List<YearCount> finishedByYear,
        List<NamedCount> byGenre,
        List<NamedCount> byPlatform,
        List<RatingBucket> ratingDistribution,
        BigDecimal averageRating,
        long hoursPlayed,
        List<Spending> spending) {

    public record YearCount(int year, long count) {}

    public record NamedCount(long id, String name, long count) {}

    public record RatingBucket(double stars, long count) {}

    /** @param purchases quantas entradas têm valor pago nessa moeda */
    public record Spending(
            String currency,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total,
            long purchases,
            List<StoreTotal> byStore,
            List<YearTotal> byYear) {}

    /** @param storeId {@code null} para compras sem loja informada */
    public record StoreTotal(
            Long storeId,
            String name,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total) {}

    public record YearTotal(
            int year,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total) {}

    public StatsDTO withoutSpending() {
        return new StatsDTO(
                year,
                total,
                byStatus,
                finishedByYear,
                byGenre,
                byPlatform,
                ratingDistribution,
                averageRating,
                hoursPlayed,
                null);
    }
}
