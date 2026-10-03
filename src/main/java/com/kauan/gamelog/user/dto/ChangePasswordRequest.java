package com.kauan.gamelog.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "informe a senha atual") String currentPassword,

        @NotBlank(message = "informe a nova senha") @Size(min = 8, max = 64, message = "use de 8 a 64 caracteres")
        String newPassword) {}
