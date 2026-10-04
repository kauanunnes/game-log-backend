package com.kauan.gamelog.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "informe o token") String token,

        @NotBlank(message = "informe a nova senha") @Size(min = 8, max = 64, message = "use de 8 a 64 caracteres")
        String newPassword) {}
