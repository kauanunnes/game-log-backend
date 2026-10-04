package com.kauan.gamelog.library;

import java.time.Instant;

/**
 * Publicado quando uma entrada é criada ({@code before} nulo), alterada ou removida ({@code after} nulo). O feed
 * ouve para registrar a atividade (RF51); as recomendações (Fase 3) também vão ouvir.
 */
public record LibraryEntryChanged(long userId, long gameId, long entryId, State before, State after) {

    /**
     * O que interessa a quem ouve; loja e valor pago ficam de fora.
     *
     * @param reviewed tem nota ou texto
     * @param hasText tem texto, então aparece nas listas e pode ser curtida
     * @param reviewedAt muda só quando a avaliação muda
     */
    public record State(
            EntryStatus status,
            boolean favorite,
            boolean completed,
            boolean reviewed,
            boolean hasText,
            Instant reviewedAt) {}
}
