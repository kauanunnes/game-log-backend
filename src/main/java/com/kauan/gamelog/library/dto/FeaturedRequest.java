package com.kauan.gamelog.library.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Os favoritos em destaque, na ordem em que aparecem; lista vazia tira todos. */
public record FeaturedRequest(
        @NotNull(message = "envie os jogos") @Size(max = 5, message = "escolha no máximo 5 jogos")
        List<@NotNull(message = "informe o jogo") Long> gameIds) {}
