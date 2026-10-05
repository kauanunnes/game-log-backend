package com.kauan.gamelog.recommendation.dto;

import java.util.List;

/**
 * "Você poderá gostar" (RF61).
 *
 * @param personalized {@code false} quando a biblioteca ainda não diz nada do gosto e as sugestões são só os populares
 * @param source {@code SEARCH} (a busca, com motivo por template) ou {@code CLAUDE} (escolhidas e explicadas por ele)
 * @param curating o Claude está escolhendo em segundo plano; vale perguntar de novo em alguns segundos
 */
public record RecommendationsDTO(
        List<SuggestionDTO> suggestions, boolean personalized, Source source, boolean curating) {

    public enum Source {
        SEARCH,
        CLAUDE
    }
}
