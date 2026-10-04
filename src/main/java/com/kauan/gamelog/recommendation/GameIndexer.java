package com.kauan.gamelog.recommendation;

import com.kauan.gamelog.catalog.GameService;
import com.kauan.gamelog.catalog.dto.GameProfile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Gera e guarda os embeddings dos jogos (Fase 3). Junto do vetor ficam o hash do texto e o nome do modelo, então um
 * jogo só é recalculado quando o texto dele ou o modelo mudou.
 */
@Service
public class GameIndexer {
    private final EmbeddingModel model;
    private final String modelName;
    private final GameService games;
    private final GameEmbeddings embeddings;

    GameIndexer(
            ObjectProvider<EmbeddingModel> models,
            @Value("${game-log.embeddings.model}") String modelName,
            GameService games,
            GameEmbeddings embeddings) {
        this.model = models.getIfAvailable();
        this.modelName = modelName;
        this.games = games;
        this.embeddings = embeddings;
    }

    /** Sem modelo ({@code spring.ai.model.embedding=none}), nada é indexado. */
    public boolean enabled() {
        return model != null;
    }

    /** @return quantos jogos ganharam um vetor novo */
    public int index(Collection<Long> gameIds) {
        if (model == null || gameIds.isEmpty()) {
            return 0;
        }
        Map<Long, String> stored = embeddings.hashes(gameIds, modelName);
        List<Long> ids = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        for (GameProfile game : games.profiles(gameIds)) {
            String text = GameText.of(game);
            String hash = hash(text);
            if (!hash.equals(stored.get(game.id()))) {
                ids.add(game.id());
                texts.add(text);
                hashes.add(hash);
            }
        }
        if (ids.isEmpty()) {
            return 0;
        }
        List<float[]> vectors = model.embed(texts);
        for (int i = 0; i < ids.size(); i++) {
            embeddings.save(ids.get(i), vectors.get(i), hashes.get(i), modelName);
        }
        return ids.size();
    }

    static String hash(String text) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
