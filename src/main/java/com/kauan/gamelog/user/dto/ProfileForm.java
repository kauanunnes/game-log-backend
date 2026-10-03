package com.kauan.gamelog.user.dto;

import com.kauan.gamelog.user.Gender;
import com.kauan.gamelog.user.User;
import com.kauan.gamelog.user.ValidUsername;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Campos editáveis do perfil. O {@code PATCH /me} chega como JSON Merge Patch sobre este formato. */
public record ProfileForm(
        @NotBlank(message = "o username não pode ficar vazio") @ValidUsername
        String username,

        @Size(max = 50, message = "use no máximo 50 caracteres")
        String displayName,

        @Size(max = 300, message = "use no máximo 300 caracteres")
        String bio,

        Gender gender) {

    public static ProfileForm of(User user) {
        return new ProfileForm(user.getUsername(), user.getDisplayName(), user.getBio(), user.getGender());
    }
}
