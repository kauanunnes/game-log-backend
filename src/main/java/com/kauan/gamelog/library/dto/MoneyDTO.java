package com.kauan.gamelog.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** O valor sai como string no JSON para o JavaScript não perder centavos. */
public record MoneyDTO(
        @NotNull(message = "informe o valor")
        @DecimalMin(value = "0", message = "não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "use no máximo duas casas decimais")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal amount,

        @NotNull(message = "informe a moeda") @Pattern(regexp = "[A-Z]{3}", message = "use o código ISO 4217, como BRL")
        String currency) {}
