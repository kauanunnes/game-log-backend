package com.kauan.gamelog.user.dto;

import com.kauan.gamelog.user.ProfileVisibility;
import jakarta.validation.constraints.Pattern;

/** Campos {@code null} ficam como estão. */
public record UpdateSettingsRequest(
        ProfileVisibility profileVisibility,
        Boolean showSpending,

        @Pattern(regexp = "[A-Z]{3}", message = "use o código ISO 4217 da moeda, como BRL")
        String defaultCurrency) {}
