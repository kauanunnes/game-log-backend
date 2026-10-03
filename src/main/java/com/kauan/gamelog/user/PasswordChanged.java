package com.kauan.gamelog.user;

/** Publicado ao trocar a senha; a autenticação encerra as sessões abertas. */
public record PasswordChanged(long userId) {}
