package com.kauan.gamelog.library.dto;

import com.kauan.gamelog.library.EntryStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Filtros da biblioteca. Listas vêm separadas por vírgula: {@code ?status=PLAYED,DROPPED}.
 *
 * @param platformId plataforma em que a pessoa jogou
 * @param completed "zerou"; {@code false} também traz quem não respondeu
 * @param q parte do título do jogo
 */
public record LibraryFilter(
        List<EntryStatus> status,
        Boolean favorite,
        Long genreId,
        Long platformId,

        @DecimalMin(value = "0", message = "use de 0 a 5") @DecimalMax(value = "5", message = "use de 0 a 5")
        BigDecimal minRating,

        Boolean recommends,
        Boolean completed,

        @Size(max = 100, message = "use no máximo 100 caracteres")
        String q) {}
