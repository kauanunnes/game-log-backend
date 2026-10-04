package com.kauan.gamelog.recommendation;

import com.kauan.gamelog.catalog.GameMetadata;
import com.kauan.gamelog.catalog.dto.GameProfile;
import java.util.List;

/**
 * O texto que representa o jogo no embedding. Fica em inglês, como os dados do IGDB e o modelo, com o que mais diz
 * sobre o jogo primeiro: o modelo lê até 256 tokens.
 */
final class GameText {
    private static final int MAX_KEYWORDS = 15;
    private static final int MAX_SUMMARY = 600;

    private GameText() {}

    static String of(GameProfile game) {
        StringBuilder text = new StringBuilder(game.title());
        if (game.releaseDate() != null) {
            text.append(" (").append(game.releaseDate().getYear()).append(')');
        }
        text.append('.');
        GameMetadata metadata = game.metadata();
        part(text, "Genres", game.genres());
        part(text, "Themes", metadata.themes());
        part(text, "Keywords", metadata.keywords().stream().limit(MAX_KEYWORDS).toList());
        part(text, "Modes", metadata.modes());
        part(text, "Perspective", metadata.perspectives());
        part(text, "Developer", metadata.developers());
        part(text, "Series", metadata.series().isEmpty() ? metadata.franchises() : metadata.series());
        if (game.summary() != null && !game.summary().isBlank()) {
            String summary = game.summary().strip();
            text.append(" Summary: ")
                    .append(summary.length() > MAX_SUMMARY ? summary.substring(0, MAX_SUMMARY) + "..." : summary);
        }
        return text.toString();
    }

    private static void part(StringBuilder text, String label, List<String> values) {
        if (!values.isEmpty()) {
            text.append(' ')
                    .append(label)
                    .append(": ")
                    .append(String.join(", ", values))
                    .append('.');
        }
    }
}
