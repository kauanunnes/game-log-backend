package com.kauan.gamelog.user.dto;

import com.kauan.gamelog.user.Gender;
import com.kauan.gamelog.user.ProfileVisibility;
import com.kauan.gamelog.user.Role;
import com.kauan.gamelog.user.User;
import java.time.Instant;

public record MeDTO(
        Long id,
        String username,
        String email,
        boolean emailVerified,
        String displayName,
        String bio,
        Gender gender,
        Role role,
        ProfileVisibility profileVisibility,
        boolean showSpending,
        String defaultCurrency,
        Instant createdAt) {

    public static MeDTO from(User user) {
        return new MeDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getEmailVerifiedAt() != null,
                user.getDisplayName(),
                user.getBio(),
                user.getGender(),
                user.getRole(),
                user.getProfileVisibility(),
                user.isShowSpending(),
                user.getDefaultCurrency(),
                user.getCreatedAt());
    }
}
