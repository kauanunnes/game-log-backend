package com.kauan.gamelog.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kauan.gamelog.library.dto.LibraryCounts;
import com.kauan.gamelog.user.Gender;
import com.kauan.gamelog.user.PublicUser;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Cabeçalho do perfil. Num perfil privado, só username, nome e {@code "private": true} (RN10). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileDTO(
        String username,
        String displayName,
        @JsonProperty("private") boolean privateProfile,
        String bio,
        Gender gender,
        LocalDate memberSince,
        LibraryCounts counts) {

    public static ProfileDTO of(PublicUser user, LibraryCounts counts) {
        return new ProfileDTO(
                user.username(),
                user.displayName(),
                false,
                user.bio(),
                user.gender(),
                LocalDate.ofInstant(user.createdAt(), ZoneOffset.UTC),
                counts);
    }

    public static ProfileDTO privateHeader(PublicUser user) {
        return new ProfileDTO(user.username(), user.displayName(), true, null, null, null, null);
    }
}
