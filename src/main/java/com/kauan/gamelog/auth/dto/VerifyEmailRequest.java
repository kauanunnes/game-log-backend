package com.kauan.gamelog.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "informe o token") String token) {}
