package com.kauan.gamelog.lists.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Os itens na ordem em que ficam; a lista inteira é trocada de uma vez. */
public record ListItemsRequest(
        @NotNull(message = "envie os itens") @Size(max = 100, message = "use no máximo 100 jogos")
        List<@Valid Item> items) {

    public record Item(
            @NotNull(message = "informe o jogo") Long gameId,

            @Size(max = 300, message = "use no máximo 300 caracteres")
            String note) {

        public Item {
            note = note == null || note.isBlank() ? null : note.strip();
        }
    }
}
