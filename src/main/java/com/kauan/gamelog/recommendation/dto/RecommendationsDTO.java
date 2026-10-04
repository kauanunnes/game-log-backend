package com.kauan.gamelog.recommendation.dto;

import java.util.List;

/**
 * "Você poderá gostar" (RF61).
 *
 * @param personalized {@code false} quando a biblioteca ainda não diz nada do gosto e as sugestões são só os populares
 */
public record RecommendationsDTO(List<SuggestionDTO> suggestions, boolean personalized) {}
