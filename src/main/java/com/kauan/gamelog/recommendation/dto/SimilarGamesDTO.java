package com.kauan.gamelog.recommendation.dto;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import java.util.List;

/**
 * Jogos parecidos de dois jeitos, para comparar (RF60).
 *
 * @param byContent os vizinhos no espaço de embeddings; {@code null} quando o jogo ainda não tem vetor
 * @param byIgdb os {@code similar_games} do IGDB que estão no catálogo, na ordem do IGDB
 */
public record SimilarGamesDTO(List<GameSummaryDTO> byContent, List<GameSummaryDTO> byIgdb) {}
