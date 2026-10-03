package com.kauan.gamelog.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Números da comunidade de um jogo (RF23), só com perfis públicos (RN11).
 *
 * @param ratingDistribution uma faixa a cada meia estrela, de 0 a 5; 4,75 conta na faixa 4,5
 * @param recommendPercent entre quem respondeu "recomenda"; {@code null} se ninguém respondeu
 * @param playersCount quem está jogando, jogou ou abandonou
 * @param wantToPlayCount quem marcou "quero jogar" ou "lista de desejos"
 */
public record CommunityDTO(
        BigDecimal averageRating,
        long ratingsCount,
        List<RatingCount> ratingDistribution,
        Integer recommendPercent,
        long playersCount,
        long wantToPlayCount) {

    public record RatingCount(double stars, long count) {}
}
