package com.kauan.gamelog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Embeddings sem modelo de verdade: cada palavra soma 1 numa das 384 posições, e o vetor é normalizado. É
 * determinístico e instantâneo, e textos com palavras em comum ficam próximos.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeEmbeddingModel implements EmbeddingModel {
    private static final int DIMENSIONS = 384;

    private final List<String> embedded = new CopyOnWriteArrayList<>();

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> results = new ArrayList<>();
        for (String text : request.getInstructions()) {
            embedded.add(text);
            results.add(new Embedding(vector(text), results.size()));
        }
        return new EmbeddingResponse(results);
    }

    @Override
    public float[] embed(Document document) {
        return vector(document.getText());
    }

    /** Quantas vezes este texto passou pelo modelo. */
    public long timesEmbedded(String text) {
        return embedded.stream().filter(text::equals).count();
    }

    private static float[] vector(String text) {
        float[] vector = new float[DIMENSIONS];
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (!word.isEmpty()) {
                vector[Math.floorMod(word.hashCode(), DIMENSIONS)] += 1;
            }
        }
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        for (int i = 0; norm > 0 && i < DIMENSIONS; i++) {
            vector[i] /= (float) Math.sqrt(norm);
        }
        return vector;
    }
}
