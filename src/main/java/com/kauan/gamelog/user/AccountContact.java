package com.kauan.gamelog.user;

/** Para quem e como escrever: o nome de exibição, ou o username se não houver. */
public record AccountContact(long id, String email, String name, boolean verified) {
    static AccountContact of(User user) {
        return new AccountContact(
                user.getId(),
                user.getEmail(),
                user.getDisplayName() == null ? user.getUsername() : user.getDisplayName(),
                user.getEmailVerifiedAt() != null);
    }
}
