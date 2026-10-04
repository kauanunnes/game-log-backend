package com.kauan.gamelog.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

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
                "Hollow Knight",
                LocalDate.of(2017, 2, 24),
                " Um reino de insetos. ",
                List.of("Adventure"),
                metadata);

        assertThat(GameText.of(game))
                .isEqualTo("Hollow Knight (2017). Genres: Adventure. Themes: Action, Fantasy. Keywords: metroidvania."
                        + " Modes: Single player. Perspective: Side view. Developer: Team Cherry."
                        + " Series: Hollow Knight. Summary: Um reino de insetos.");
    }

    @Test
    void leavesOutWhatTheGameLacksAndShortensLongSummaries() {
        var empty = new GameMetadata(null, null, null, null, null, null, List.of("Portal"), null, null, null);
        var game = new GameProfile(2, "Portal", null, "a".repeat(700), List.of(), empty);

        assertThat(GameText.of(game)).isEqualTo("Portal. Series: Portal. Summary: " + "a".repeat(600) + "...");
    }
}
