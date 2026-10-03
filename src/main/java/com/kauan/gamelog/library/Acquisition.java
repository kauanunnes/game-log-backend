package com.kauan.gamelog.library;

import com.kauan.gamelog.library.dto.AcquisitionDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.LocalDate;

@Embeddable
record Acquisition(
        @Column(name = "acquisition") @Enumerated(EnumType.STRING)
        AcquisitionMethod method,

        Long storeId,
        @Column(name = "price_paid") BigDecimal price,
        String currency,
        LocalDate acquiredOn) {

    static Acquisition from(AcquisitionDTO dto) {
        if (dto == null) {
            return null;
        }
        return new Acquisition(
                dto.method(),
                dto.storeId(),
                dto.price() == null ? null : dto.price().amount(),
                dto.price() == null ? null : dto.price().currency(),
                dto.acquiredOn());
    }
}
