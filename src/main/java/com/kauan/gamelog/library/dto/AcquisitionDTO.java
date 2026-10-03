package com.kauan.gamelog.library.dto;

import com.kauan.gamelog.library.AcquisitionMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/** @param price só existe numa compra (RN05) */
public record AcquisitionDTO(
        @NotNull(message = "informe como conseguiu o jogo") AcquisitionMethod method,
        Long storeId,
        @Valid MoneyDTO price,
        @PastOrPresent(message = "não pode estar no futuro") LocalDate acquiredOn) {

    public boolean blank() {
        return method == null && storeId == null && price == null && acquiredOn == null;
    }
}
