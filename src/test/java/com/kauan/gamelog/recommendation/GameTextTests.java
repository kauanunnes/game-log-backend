package com.kauan.gamelog.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.kauan.gamelog.catalog.GameKind;
import com.kauan.gamelog.catalog.GameMetadata;
import com.kauan.gamelog.catalog.dto.GameProfile;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameTextTests {
    @Test
    void describesTheGameWithTheMostTellingPartsFirst() {
        var metadata = new GameMetadata(
                List.of("Action", "Fantasy"),
                List.of("metroidvania"),
                List.of("Single player"),
                List.of("Side view"),
                List.of("Team Cherry"),
                List.of("Team Cherry"),
                List.of(),
                List.of(),
                List.of("Hollow Knight"),
                null);
        var game = new GameProfile(
                1,
                14593L,
                "Hollow Knight",
                LocalDate.of(2017, 2, 24),
                GameKind.MAIN,
                " Um reino de insetos. ",
                List.of("Adventure"),
                metadata);

        // Nem o título nem o resumo: as palavras do nome puxariam jogos sem relação
        assertThat(GameText.of(game))
                .isEqualTo("Released in 2017. Genres: Adventure. Themes: Action, Fantasy. Keywords: metroidvania."
                        + " Modes: Single player. Perspective: Side view. Developer: Team Cherry."
                        + " Series: Hollow Knight.");
    }

    @Test
    void withoutThemesOrKeywordsTheSummaryDescribesTheGame() {
        var sparse = new GameMetadata(null, null, null, null, null, null, List.of("Portal"), null, null, null);
        var game = new GameProfile(2, null, "Portal", null, GameKind.MAIN, "a".repeat(700), List.of(), sparse);

        assertThat(GameText.of(game)).isEqualTo("Series: Portal. Summary: " + "a".repeat(600) + "...");
    }
}
