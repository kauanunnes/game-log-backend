package com.kauan.gamelog.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "informe a senha") String password) {}
