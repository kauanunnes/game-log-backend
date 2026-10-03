package com.kauan.gamelog.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** @param login username ou e-mail */
public record LoginRequest(
        @NotBlank(message = "informe o username ou o e-mail")
        String login,

        @NotBlank(message = "informe a senha") String password) {}
