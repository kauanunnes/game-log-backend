package com.kauan.gamelog.recommendation;

import com.kauan.gamelog.catalog.GameService;
import com.kauan.gamelog.catalog.dto.GameProfile;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.recommendation.dto.SimilarGamesDTO;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jogos parecidos (RF60). Pelo conteúdo, são os vizinhos mais próximos no espaço de embeddings, menos as expansões e
 * edições do próprio jogo. Ao lado vão os {@code similar_games} do IGDB, para comparar, e eles seguram a seção quando o
 * jogo não tem vetor (em produção, enquanto os embeddings estão desligados).
 */
@Service
public class SimilarGames {
    private static final int SIZE = 12;
    /** Vizinhos a mais, para sobrarem {@link #SIZE} depois dos filtros. */
    private static final int CANDIDATES = 40;

    private final GameService games;
    private final GameEmbeddings embeddings;

    SimilarGames(GameService games, GameEmbeddings embeddings) {
        this.games = games;
        this.embeddings = embeddings;
    }

    @Transactional(readOnly = true)
    public SimilarGamesDTO of(String slug) {
        long id = games.idOf(slug);
        GameProfile game = games.profiles(List.of(id)).getFirst();
        List<GameSummaryDTO> byIgdb = games.summariesByIgdbIds(game.metadata().similarGames()).stream()
                .limit(SIZE)
                .toList();
        List<Long> nearest = embeddings.nearest(id, CANDIDATES);
        if (nearest.isEmpty()) {
            return new SimilarGamesDTO(null, byIgdb);
        }
        List<Long> byContent = games.profiles(nearest).stream()
                .filter(candidate -> game.igdbId() == null
                        || !Objects.equals(candidate.metadata().parentGame(), game.igdbId()))
                .sorted(Comparator.comparingInt(candidate -> nearest.indexOf(candidate.id())))
                .limit(SIZE)
                .map(GameProfile::id)
                .toList();
        return new SimilarGamesDTO(games.summaries(byContent), byIgdb);
    }
}
