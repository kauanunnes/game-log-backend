package com.kauan.gamelog.user;

/** O que o módulo de autenticação precisa saber de quem entrou. */
public record AuthUser(long id, String username, Role role) {
    static AuthUser of(User user) {
        return new AuthUser(user.getId(), user.getUsername(), user.getRole());
    }
}
