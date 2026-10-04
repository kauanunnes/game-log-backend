package com.kauan.gamelog.lists.dto;

import com.kauan.gamelog.lists.ListVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Título, descrição e visibilidade; o {@code PATCH} chega como JSON Merge Patch sobre este formato. */
public record ListForm(
        @NotBlank(message = "dê um título à lista") @Size(max = 80, message = "use no máximo 80 caracteres")
        String title,

        @Size(max = 500, message = "use no máximo 500 caracteres")
        String description,

        ListVisibility visibility) {

    public ListForm {
        title = title == null ? null : title.strip();
        description = description == null || description.isBlank() ? null : description.strip();
        visibility = visibility == null ? ListVisibility.PUBLIC : visibility;
    }
}
