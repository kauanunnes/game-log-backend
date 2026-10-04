package com.kauan.gamelog.catalog;

/** Um jogo entrou no catálogo ou foi atualizado pelo IGDB, às vezes com gêneros e plataformas novos. */
public record GameImported(long gameId) {}
