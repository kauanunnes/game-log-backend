package com.kauan.gamelog.auth.dto;

/** @param expiresIn validade do access token, em segundos */
public record TokenResponse(String accessToken, long expiresIn) {}
