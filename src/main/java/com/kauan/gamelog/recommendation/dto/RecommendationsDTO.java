package com.kauan.gamelog.recommendation.dto;

import java.util.List;

/**
 * "Você poderá gostar" (RF61).
 *
 * @param personalized {@code false} quando a biblioteca ainda não diz nada do gosto e as sugestões são só os populares
 * @param source {@code SEARCH} (a busca, com motivo por template) ou {@code AI} (escolhidas e explicadas pelo modelo)
 * @param curator o modelo que escolhe, "Gemini" ou "Claude"; {@code null} sem a chave da API
 * @param curating o modelo está escolhendo em segundo plano; vale perguntar de novo em alguns segundos
 */
public record RecommendationsDTO(
        List<SuggestionDTO> suggestions, boolean personalized, Source source, String curator, boolean curating) {

    public enum Source {
        SEARCH,
        AI
    }
}
