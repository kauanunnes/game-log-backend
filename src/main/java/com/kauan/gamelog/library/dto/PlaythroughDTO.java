package com.kauan.gamelog.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * @param platformId qualquer plataforma do catálogo, não só as oficiais do jogo (RN07)
 * @param completed "zerou"
 */
public record PlaythroughDTO(
        Long platformId,

        @PositiveOrZero(message = "não pode ser negativo") @Max(value = 100_000, message = "valor alto demais")
        Integer hoursPlayed,

        @PastOrPresent(message = "não pode estar no futuro") LocalDate startedOn,
        @PastOrPresent(message = "não pode estar no futuro") LocalDate finishedOn,
        Boolean completed) {

    public boolean blank() {
        return platformId == null
                && hoursPlayed == null
                && startedOn == null
                && finishedOn == null
                && completed == null;
    }
}
