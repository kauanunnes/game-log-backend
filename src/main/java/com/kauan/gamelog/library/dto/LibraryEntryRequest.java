package com.kauan.gamelog.library.dto;

import com.kauan.gamelog.library.EntryStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo do {@code PUT /me/library/{gameId}}, que é também o formato sobre o qual o PATCH (JSON Merge Patch) é
 * aplicado. Objetos vazios, como {@code "review": {}}, valem como ausentes.
 */
public record LibraryEntryRequest(
        @NotNull(message = "informe o status") EntryStatus status,
        Boolean favorite,
        @Valid ReviewDTO review,
        @Valid PlaythroughDTO playthrough,
        @Valid AcquisitionDTO acquisition) {

    public LibraryEntryRequest {
        favorite = Boolean.TRUE.equals(favorite);
        review = review == null || review.blank() ? null : review;
        playthrough = playthrough == null || playthrough.blank() ? null : playthrough;
        acquisition = acquisition == null || acquisition.blank() ? null : acquisition;
    }

    public static LibraryEntryRequest of(LibraryEntryDTO entry) {
        return new LibraryEntryRequest(
                entry.status(), entry.favorite(), entry.review(), entry.playthrough(), entry.acquisition());
    }
}
