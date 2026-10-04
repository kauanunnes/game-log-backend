package com.kauan.gamelog.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.user.Gender;
import com.kauan.gamelog.user.PublicUser;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/** Cabeçalho do perfil. Num perfil privado, só username, nome e {@code "private": true} (RN10). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileDTO(
        String username,
        String displayName,
        @JsonProperty("private") boolean privateProfile,
        String bio,
        Gender gender,
        LocalDate memberSince,
        ProfileCounts counts,
        List<GameSummaryDTO> featured) {

    /** @param featured os favoritos em destaque, em ordem (RF38) */
    public static ProfileDTO of(PublicUser user, ProfileCounts counts, List<GameSummaryDTO> featured) {
        return new ProfileDTO(
                user.username(),
                user.displayName(),
                user.privateProfile(),
                user.bio(),
                user.gender(),
                LocalDate.ofInstant(user.createdAt(), ZoneOffset.UTC),
                counts,
                featured);
    }

    public static ProfileDTO privateHeader(PublicUser user) {
        return new ProfileDTO(user.username(), user.displayName(), true, null, null, null, null, null);
    }
}
