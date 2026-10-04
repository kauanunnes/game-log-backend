package com.kauan.gamelog.shared;

/** Os caches em memória (Caffeine), declarados em {@code spring.cache.cache-names}. */
public final class Caches {
    /** Gêneros, plataformas e lojas; uma importação do IGDB limpa. */
    public static final String LOOKUPS = "lookups";

    /** As estatísticas do ano todo de cada pessoa; qualquer mudança na biblioteca dela limpa. */
    public static final String STATS = "stats";

    private Caches() {}
}
