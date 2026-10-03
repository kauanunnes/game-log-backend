package com.kauan.gamelog.shared;

/** Um campo inválido, na lista {@code errors} do Problem Details. */
public record FieldIssue(String field, String message) {}
