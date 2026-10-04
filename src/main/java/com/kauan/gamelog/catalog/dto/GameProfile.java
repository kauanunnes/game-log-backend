package com.kauan.gamelog.catalog.dto;

import com.kauan.gamelog.catalog.GameKind;
import com.kauan.gamelog.catalog.GameMetadata;
import java.time.LocalDate;
import java.util.List;

/** O que descreve um jogo, para quem lê o catálogo em lote, como os embeddings da Fase 3. */
public record GameProfile(
        long id,
        Long igdbId,
        String title,
        LocalDate releaseDate,
        GameKind kind,
        String summary,
        List<String> genres,
        GameMetadata metadata) {}
