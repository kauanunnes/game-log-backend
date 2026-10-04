package com.kauan.gamelog.recommendation;

import com.kauan.gamelog.catalog.GameKind;
import com.kauan.gamelog.catalog.GameMetadata;
import com.kauan.gamelog.catalog.GameService;
import com.kauan.gamelog.catalog.dto.GameProfile;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.library.EntryStatus;
import com.kauan.gamelog.library.LibraryService;
import com.kauan.gamelog.library.dto.TasteSignal;
import com.kauan.gamelog.recommendation.dto.RecommendationsDTO;
import com.kauan.gamelog.recommendation.dto.SuggestionDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Você poderá gostar" só com busca (RF61). Os jogos de que a pessoa mais gostou viram sementes; os vizinhos de cada
 * uma se juntam por Reciprocal Rank Fusion, pesados pelo gosto. Saem o que já está na biblioteca, o que fica mais perto
 * de um jogo de que ela não gostou do que da semente que o trouxe, expansões, edições do que ela já tem e jogos não
 * lançados, e entram no máximo dois por série.
 * Sem vetores, os {@code similar_games} do IGDB fazem o papel dos vizinhos. Ver 06 · Recomendações.
 */
@Service
public class Recommendations {
    static final int SIZE = 20;
    private static final int SEEDS = 10;
    private static final int NEIGHBORS = 40;
    /** A constante do Reciprocal Rank Fusion: com ela, o primeiro lugar não pesa demais. */
    private static final int RRF_K = 60;

    private static final int PER_SERIES = 2;
    /** Com menos sementes que isso, a biblioteca diz pouco do gosto, e os populares completam a lista. */
    private static final int MIN_SEEDS = 3;

    private final LibraryService library;
    private final GameService games;
    private final GameEmbeddings embeddings;

    Recommendations(LibraryService library, GameService games, GameEmbeddings embeddings) {
        this.library = library;
        this.games = games;
        this.embeddings = embeddings;
    }

    @Transactional(readOnly = true)
    public RecommendationsDTO forUser(long userId) {
        List<TasteSignal> taste = library.taste(userId);
        Set<Long> owned = taste.stream().map(TasteSignal::gameId).collect(Collectors.toSet());
        Map<Long, GameProfile> mine = byId(games.profiles(owned));
        List<TasteSignal> seeds = taste.stream()
                .filter(signal -> weight(signal) > 0)
                .sorted(Comparator.comparingInt(Recommendations::weight)
                        .thenComparing(TasteSignal::updatedAt)
                        .reversed())
                .limit(SEEDS)
                .toList();

        // Reciprocal Rank Fusion: cada semente soma peso / (k + posição) aos vizinhos dela
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Integer> closestToSeed = new HashMap<>();
        Map<Long, Double> strongest = new HashMap<>();
        Map<Long, TasteSignal> because = new HashMap<>();
        for (TasteSignal seed : seeds) {
            List<Long> neighbors = neighbors(seed.gameId(), mine.get(seed.gameId()));
            for (int rank = 0; rank < neighbors.size(); rank++) {
                long candidate = neighbors.get(rank);
                double contribution = (double) weight(seed) / (RRF_K + rank + 1);
                scores.merge(candidate, contribution, Double::sum);
                closestToSeed.merge(candidate, rank, Math::min);
                if (contribution > strongest.getOrDefault(candidate, 0.0)) {
                    strongest.put(candidate, contribution);
                    because.put(candidate, seed);
                }
            }
        }
        // Sai quem fica mais perto de um jogo de que a pessoa não gostou do que da semente que o trouxe
        Map<Long, Integer> closestToDisliked = new HashMap<>();
        taste.stream().filter(Recommendations::disliked).forEach(signal -> {
            List<Long> neighbors = neighbors(signal.gameId(), mine.get(signal.gameId()));
            for (int rank = 0; rank < neighbors.size(); rank++) {
                closestToDisliked.merge(neighbors.get(rank), rank, Math::min);
            }
        });
        Set<Long> ownedIgdbIds = mine.values().stream()
                .map(GameProfile::igdbId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Long> ranked = scores.keySet().stream()
                .filter(candidate -> !owned.contains(candidate)
                        && closestToDisliked.getOrDefault(candidate, Integer.MAX_VALUE) >= closestToSeed.get(candidate))
                .sorted(Comparator.comparing((Long candidate) -> scores.get(candidate))
                        .reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
        Map<Long, String> reasons = new LinkedHashMap<>();
        Map<String, Integer> perSeries = new HashMap<>();
        for (GameProfile candidate : inOrder(ranked)) {
            if (reasons.size() == SIZE) {
                break;
            }
            if (suggestable(candidate, ownedIgdbIds) && fitsSeries(candidate, perSeries)) {
                TasteSignal seed = because.get(candidate.id());
                reasons.put(candidate.id(), reason(mine.get(seed.gameId()).title(), seed));
            }
        }
        if (seeds.size() < MIN_SEEDS || reasons.isEmpty()) {
            for (GameProfile game : inOrder(games.popularIds(SIZE * 5))) {
                if (reasons.size() == SIZE) {
                    break;
                }
                if (!owned.contains(game.id())
                        && !reasons.containsKey(game.id())
                        && suggestable(game, ownedIgdbIds)
                        && fitsSeries(game, perSeries)) {
                    reasons.put(game.id(), "Entre os mais populares do IGDB.");
                }
            }
        }
        List<SuggestionDTO> suggestions = new ArrayList<>();
        for (GameSummaryDTO game : games.summaries(List.copyOf(reasons.keySet()))) {
            suggestions.add(new SuggestionDTO(game, reasons.get(game.id())));
        }
        return new RecommendationsDTO(suggestions, !seeds.isEmpty());
    }

    /** Quanto a entrada diz do gosto: 0 quando não diz nada de bom (ver os pesos em 06 · Recomendações). */
    static int weight(TasteSignal signal) {
        if (disliked(signal)) {
            return 0;
        }
        // Jogar, ter jogado ou querer jogar já diz algo; abandonar, não
        int weight = signal.status() == EntryStatus.DROPPED ? 0 : 1;
        if (signal.favorite()) {
            weight += 3;
        }
        if (atLeast(signal.rating(), "4.5")) {
            weight += 2;
        } else if (atLeast(signal.rating(), "3.5")) {
            weight += 1;
        }
        if (Boolean.TRUE.equals(signal.recommends())) {
            weight += 1;
        }
        return weight;
    }

    static boolean disliked(TasteSignal signal) {
        return (signal.rating() != null && !atLeast(signal.rating(), "2.25"))
                || Boolean.FALSE.equals(signal.recommends());
    }

    /** O motivo cita o sinal mais forte da semente que mais pesou para a sugestão. */
    static String reason(String title, TasteSignal seed) {
        String why;
        if (seed.favorite()) {
            why = "que você favoritou";
        } else if (atLeast(seed.rating(), "4.5")) {
            why = "que você avaliou com " + stars(seed.rating());
        } else if (Boolean.TRUE.equals(seed.recommends())) {
            why = "que você recomenda";
        } else if (atLeast(seed.rating(), "3.5")) {
            why = "que você avaliou com " + stars(seed.rating());
        } else {
            why = switch (seed.status()) {
                case PLAYING -> "que você está jogando";
                case PLAYED -> "que você jogou";
                default -> "que você quer jogar";
            };
        }
        return "Parecido com " + title + ", " + why + ".";
    }

    /** Sem vetor (em produção, enquanto os embeddings estão desligados), valem os similar_games do IGDB. */
    private List<Long> neighbors(long gameId, GameProfile game) {
        List<Long> nearest = embeddings.nearest(gameId, NEIGHBORS);
        if (!nearest.isEmpty() || game == null) {
            return nearest;
        }
        return games.summariesByIgdbIds(game.metadata().similarGames()).stream()
                .map(GameSummaryDTO::id)
                .toList();
    }

    /** Fora: expansões, edições e ports do que a pessoa já tem e jogos ainda não lançados. */
    private static boolean suggestable(GameProfile game, Set<Long> ownedIgdbIds) {
        return game.kind() != GameKind.EXPANSION
                && !ownedIgdbIds.contains(game.metadata().parentGame())
                && game.releaseDate() != null
                && !game.releaseDate().isAfter(LocalDate.now());
    }

    /** No máximo {@link #PER_SERIES} jogos da mesma série, para não sugerir cinco Final Fantasy. */
    private static boolean fitsSeries(GameProfile game, Map<String, Integer> perSeries) {
        GameMetadata metadata = game.metadata();
        List<String> series = metadata.series().isEmpty() ? metadata.franchises() : metadata.series();
        if (series.isEmpty()) {
            return true;
        }
        return perSeries.merge(series.getFirst(), 1, Integer::sum) <= PER_SERIES;
    }

    private List<GameProfile> inOrder(List<Long> ids) {
        Map<Long, GameProfile> found = byId(games.profiles(ids));
        return ids.stream().map(found::get).filter(Objects::nonNull).toList();
    }

    private static Map<Long, GameProfile> byId(List<GameProfile> profiles) {
        return profiles.stream().collect(Collectors.toMap(GameProfile::id, Function.identity()));
    }

    private static boolean atLeast(BigDecimal rating, String value) {
        return rating != null && rating.compareTo(new BigDecimal(value)) >= 0;
    }

    private static String stars(BigDecimal rating) {
        String value = rating.stripTrailingZeros().toPlainString().replace('.', ',');
        return value + (rating.compareTo(BigDecimal.ONE) == 0 ? " estrela" : " estrelas");
    }
}
