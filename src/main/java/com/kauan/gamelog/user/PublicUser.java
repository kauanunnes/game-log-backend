package com.kauan.gamelog.user;

import java.time.Instant;

/** O que o perfil público pode usar; o módulo profile decide o que aparece (RN10, RN14). */
public record PublicUser(
        long id,
        String username,
        String displayName,
        String bio,
        Gender gender,
        boolean privateProfile,
        boolean showSpending,
        Instant createdAt) {

    static PublicUser of(User user) {
        return new PublicUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getGender(),
                user.getProfileVisibility() == ProfileVisibility.PRIVATE,
                user.isShowSpending(),
                user.getCreatedAt());
    }
}
