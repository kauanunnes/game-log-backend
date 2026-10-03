package com.kauan.gamelog.library.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * @param rating de 0 a 5 em passos de 0,25 (RN03); {@code null} é "sem nota", diferente de zero
 * @param text texto puro de até 2.000 caracteres (RN04); o front nunca o trata como HTML
 */
public record ReviewDTO(
        @DecimalMin(value = "0", message = "use de 0 a 5") @DecimalMax(value = "5", message = "use de 0 a 5")
        BigDecimal rating,

        Boolean recommends,

        @Size(max = 2000, message = "use no máximo 2.000 caracteres")
        String text,

        Boolean hasSpoilers) {

    public ReviewDTO {
        text = text == null || text.isBlank() ? null : text.strip();
        hasSpoilers = Boolean.TRUE.equals(hasSpoilers);
    }

    /** Sem nota, sem texto e sem "recomenda": não é uma avaliação. */
    public boolean blank() {
        return rating == null && recommends == null && text == null;
    }
}
