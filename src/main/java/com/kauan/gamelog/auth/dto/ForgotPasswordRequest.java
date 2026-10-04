package com.kauan.gamelog.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "informe o e-mail") @Email(message = "e-mail inválido")
        String email) {}
