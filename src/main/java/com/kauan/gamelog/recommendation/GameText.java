package com.kauan.gamelog.recommendation;

import com.kauan.gamelog.catalog.GameMetadata;
import com.kauan.gamelog.catalog.dto.GameProfile;
import java.util.List;

/**
 * O texto que representa o jogo no embedding, em inglês, como os dados do IGDB e o modelo. Ficam de fora o título e o
 * resumo: com eles, palavras do nome puxavam jogos sem relação ("Stardew" levava a "StarCraft"). Num catálogo de 2 mil
 * jogos, só com os metadados, 36% dos {@code similar_games} do IGDB aparecem entre os 12 vizinhos; com o texto todo,
 * 28%.
 */
final class GameText {
    private static final int MAX_KEYWORDS = 15;
    private static final int MAX_SUMMARY = 600;

    private GameText() {}

    static String of(GameProfile game) {
        StringBuilder text = new StringBuilder();
        if (game.releaseDate() != null) {
            text.append("Released in ").append(game.releaseDate().getYear()).append('.');
        }
        GameMetadata metadata = game.metadata();
        part(text, "Genres", game.genres());
        part(text, "Themes", metadata.themes());
        part(text, "Keywords", metadata.keywords().stream().limit(MAX_KEYWORDS).toList());
        part(text, "Modes", metadata.modes());
        part(text, "Perspective", metadata.perspectives());
        part(text, "Developer", metadata.developers());
        part(text, "Series", metadata.series().isEmpty() ? metadata.franchises() : metadata.series());
        // Sem temas nem palavras-chave (um jogo que não veio do IGDB), o resumo descreve o jogo
        boolean described = !metadata.themes().isEmpty() || !metadata.keywords().isEmpty();
        if (!described && game.summary() != null && !game.summary().isBlank()) {
            String summary = game.summary().strip();
            text.append(" Summary: ")
                    .append(summary.length() > MAX_SUMMARY ? summary.substring(0, MAX_SUMMARY) + "..." : summary);
        }
        return text.toString().strip();
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
