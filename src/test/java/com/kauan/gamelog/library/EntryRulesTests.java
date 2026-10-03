package com.kauan.gamelog.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.kauan.gamelog.library.EntryStatus.Part;
import com.kauan.gamelog.library.dto.AcquisitionDTO;
import com.kauan.gamelog.library.dto.LibraryEntryRequest;
import com.kauan.gamelog.library.dto.MoneyDTO;
import com.kauan.gamelog.library.dto.PlaythroughDTO;
import com.kauan.gamelog.library.dto.ReviewDTO;
import com.kauan.gamelog.shared.FieldIssue;
import com.kauan.gamelog.shared.UnprocessableException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EntryRulesTests {
    private static final LocalDate JAN = LocalDate.of(2026, 1, 10);
    private static final LocalDate FEB = LocalDate.of(2026, 2, 10);

    /** A tabela da RN02 de 01-requisitos.md, célula por célula: cada linha é uma parte, cada coluna um status. */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            # parte      | WISHLIST | BACKLOG | PLAYING | PLAYED | DROPPED
            REVIEW       | false    | false   | true    | true   | true
            PLAYTHROUGH  | false    | false   | true    | true   | true
            FINISHED_ON  | false    | false   | false   | true   | true
            COMPLETED    | false    | false   | false   | true   | false
            FAVORITE     | false    | false   | true    | true   | false
            ACQUISITION  | false    | true    | true    | true   | true
            """)
    void followsTheRn02Table(
            Part part, boolean wishlist, boolean backlog, boolean playing, boolean played, boolean dropped) {
        Map<EntryStatus, Boolean> table = Map.of(
                EntryStatus.WISHLIST, wishlist,
                EntryStatus.BACKLOG, backlog,
                EntryStatus.PLAYING, playing,
                EntryStatus.PLAYED, played,
                EntryStatus.DROPPED, dropped);

        table.forEach((status, allowed) -> {
            LibraryEntryRequest entry = entryWith(part, status);
            if (allowed) {
                assertThatCode(() -> EntryRules.check(entry))
                        .as("%s em %s", part, status)
                        .doesNotThrowAnyException();
            } else {
                assertThat(issuesOf(entry))
                        .as("%s em %s", part, status)
                        .containsExactly(new FieldIssue(part.field(), part.onlyAllowedMessage()));
            }
        });
    }

    @Test
    void explainsWhereEachPartIsAllowed() {
        assertThat(Part.REVIEW.onlyAllowedMessage()).isEqualTo("Avaliação só vale em Jogando, Jogado ou Abandonado.");
        assertThat(Part.COMPLETED.onlyAllowedMessage()).isEqualTo("Zerou só vale em Jogado.");
    }

    @Test
    void acceptsRatingsInQuarterSteps() {
        assertThatCode(() -> EntryRules.check(played(review("4.75")))).doesNotThrowAnyException();
        assertThatCode(() -> EntryRules.check(played(review("0")))).doesNotThrowAnyException();
        assertThat(issuesOf(played(review("4.8"))))
                .extracting(FieldIssue::field)
                .containsExactly("review.rating");
    }

    @Test
    void refusesAFinishBeforeTheStart() {
        var playthrough = new PlaythroughDTO(null, null, FEB, JAN, null);

        assertThat(issuesOf(new LibraryEntryRequest(EntryStatus.PLAYED, false, null, playthrough, null)))
                .containsExactly(new FieldIssue("playthrough.finishedOn", "o término não pode ser antes do início"));
    }

    @Test
    void allowsAPriceOnlyForPurchasesWithAKnownCurrency() {
        var gift = new AcquisitionDTO(AcquisitionMethod.GIFT, null, money("10.00", "BRL"), null);
        var unknownCurrency = new AcquisitionDTO(AcquisitionMethod.PURCHASED, null, money("10.00", "XYZ"), null);
        var purchase = new AcquisitionDTO(AcquisitionMethod.PURCHASED, null, money("46.99", "BRL"), null);

        assertThat(issuesOf(backlog(gift))).extracting(FieldIssue::field).containsExactly("acquisition.price");
        assertThat(issuesOf(backlog(unknownCurrency)))
                .extracting(FieldIssue::field)
                .containsExactly("acquisition.price.currency");
        assertThatCode(() -> EntryRules.check(backlog(purchase))).doesNotThrowAnyException();
    }

    @Test
    void treatsEmptyPartsAsAbsent() {
        var entry = new LibraryEntryRequest(
                EntryStatus.WISHLIST,
                null,
                new ReviewDTO(null, null, "   ", null),
                new PlaythroughDTO(null, null, null, null, null),
                null);

        assertThat(entry.review()).isNull();
        assertThat(entry.playthrough()).isNull();
        assertThatCode(() -> EntryRules.check(entry)).doesNotThrowAnyException();
    }

    @Test
    void reportsEveryPartThatDoesNotFitTheStatus() {
        var entry = new LibraryEntryRequest(EntryStatus.WISHLIST, true, review("5"), null, null);

        assertThat(issuesOf(entry)).extracting(FieldIssue::field).containsExactly("review", "favorite");
    }

    private static LibraryEntryRequest entryWith(Part part, EntryStatus status) {
        return switch (part) {
            case REVIEW -> new LibraryEntryRequest(status, false, new ReviewDTO(null, true, null, null), null, null);
            case PLAYTHROUGH ->
                new LibraryEntryRequest(status, false, null, new PlaythroughDTO(1L, 10, JAN, null, null), null);
            case FINISHED_ON ->
                new LibraryEntryRequest(status, false, null, new PlaythroughDTO(null, null, null, FEB, null), null);
            case COMPLETED ->
                new LibraryEntryRequest(status, false, null, new PlaythroughDTO(null, null, null, null, true), null);
            case FAVORITE -> new LibraryEntryRequest(status, true, null, null, null);
            case ACQUISITION ->
                new LibraryEntryRequest(
                        status, false, null, null, new AcquisitionDTO(AcquisitionMethod.GIFT, null, null, null));
        };
    }

    private static List<FieldIssue> issuesOf(LibraryEntryRequest entry) {
        try {
            EntryRules.check(entry);
        } catch (UnprocessableException e) {
            @SuppressWarnings("unchecked")
            List<FieldIssue> issues =
                    (List<FieldIssue>) e.getBody().getProperties().get("errors");
            return issues;
        }
        throw new AssertionError("a entrada devia ter sido recusada");
    }

    private static ReviewDTO review(String rating) {
        return new ReviewDTO(new BigDecimal(rating), null, null, null);
    }

    private static LibraryEntryRequest played(ReviewDTO review) {
        return new LibraryEntryRequest(EntryStatus.PLAYED, false, review, null, null);
    }

    private static LibraryEntryRequest backlog(AcquisitionDTO acquisition) {
        return new LibraryEntryRequest(EntryStatus.BACKLOG, false, null, null, acquisition);
    }

    private static MoneyDTO money(String amount, String currency) {
        return new MoneyDTO(new BigDecimal(amount), currency);
    }
}
