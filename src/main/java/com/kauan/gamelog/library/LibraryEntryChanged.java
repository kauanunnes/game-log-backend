package com.kauan.gamelog.library;

/**
 * Publicado quando uma entrada é criada, alterada ou removida ({@code status} nulo). Ainda sem ouvintes: prepara o
 * feed (Fase 2) e as recomendações (Fase 3).
 */
public record LibraryEntryChanged(long userId, long gameId, EntryStatus status) {}
