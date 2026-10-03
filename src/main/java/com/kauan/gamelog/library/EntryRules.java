package com.kauan.gamelog.library;

import static com.kauan.gamelog.library.EntryStatus.Part.ACQUISITION;
import static com.kauan.gamelog.library.EntryStatus.Part.COMPLETED;
import static com.kauan.gamelog.library.EntryStatus.Part.FAVORITE;
import static com.kauan.gamelog.library.EntryStatus.Part.FINISHED_ON;
import static com.kauan.gamelog.library.EntryStatus.Part.PLAYTHROUGH;
import static com.kauan.gamelog.library.EntryStatus.Part.REVIEW;

import com.kauan.gamelog.library.EntryStatus.Part;
import com.kauan.gamelog.library.dto.AcquisitionDTO;
import com.kauan.gamelog.library.dto.LibraryEntryRequest;
import com.kauan.gamelog.library.dto.PlaythroughDTO;
import com.kauan.gamelog.library.dto.ReviewDTO;
import com.kauan.gamelog.shared.FieldIssue;
import com.kauan.gamelog.shared.UnprocessableException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Regras de negócio da entrada inteira (RN02, RN03, RN05 e RN06), checadas depois da validação de formato. Vale o
 * estado final: um campo que não cabe no status é recusado, nunca apagado sem aviso.
 */
final class EntryRules {
    private static final BigDecimal QUARTER = new BigDecimal("0.25");

    private EntryRules() {}

    static void check(LibraryEntryRequest entry) {
        EntryStatus status = entry.status();
        List<FieldIssue> notAllowed = partsIn(entry).stream()
                .filter(part -> !status.allows(part))
                .map(part -> new FieldIssue(part.field(), part.onlyAllowedMessage()))
                .toList();
        if (!notAllowed.isEmpty()) {
            throw new UnprocessableException(
                    "INVALID_FIELDS_FOR_STATUS",
                    "A entrada não é válida para o status " + status.label() + ".",
                    notAllowed);
        }

        List<FieldIssue> issues = new ArrayList<>();
        ReviewDTO review = entry.review();
        if (review != null
                && review.rating() != null
                && review.rating().remainder(QUARTER).signum() != 0) {
            issues.add(new FieldIssue("review.rating", "use passos de 0,25, como 4,5 ou 4,75"));
        }
        PlaythroughDTO playthrough = entry.playthrough();
        if (playthrough != null
                && playthrough.startedOn() != null
                && playthrough.finishedOn() != null
                && playthrough.finishedOn().isBefore(playthrough.startedOn())) {
            issues.add(new FieldIssue("playthrough.finishedOn", "o término não pode ser antes do início"));
        }
        AcquisitionDTO acquisition = entry.acquisition();
        if (acquisition != null && acquisition.price() != null) {
            if (acquisition.method() != AcquisitionMethod.PURCHASED) {
                issues.add(new FieldIssue("acquisition.price", "valor pago só existe numa compra"));
            } else if (!knownCurrency(acquisition.price().currency())) {
                issues.add(new FieldIssue("acquisition.price.currency", "moeda desconhecida"));
            }
        }
        if (!issues.isEmpty()) {
            throw new UnprocessableException("INVALID_FIELDS", "Confira os campos indicados.", issues);
        }
    }

    /** Partes que a entrada preenche; cada uma depende do status. */
    static Set<Part> partsIn(LibraryEntryRequest entry) {
        Set<Part> parts = EnumSet.noneOf(Part.class);
        if (entry.review() != null) {
            parts.add(REVIEW);
        }
        PlaythroughDTO playthrough = entry.playthrough();
        if (playthrough != null) {
            if (playthrough.platformId() != null
                    || playthrough.hoursPlayed() != null
                    || playthrough.startedOn() != null) {
                parts.add(PLAYTHROUGH);
            }
            if (playthrough.finishedOn() != null) {
                parts.add(FINISHED_ON);
            }
            if (playthrough.completed() != null) {
                parts.add(COMPLETED);
            }
        }
        if (entry.favorite()) {
            parts.add(FAVORITE);
        }
        if (entry.acquisition() != null) {
            parts.add(ACQUISITION);
        }
        return parts;
    }

    private static boolean knownCurrency(String code) {
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
