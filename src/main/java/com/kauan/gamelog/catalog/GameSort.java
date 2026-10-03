package com.kauan.gamelog.catalog;

import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public enum GameSort {
    RELEVANCE("word_similarity(:q, g.title_normalized) DESC, g.igdb_rating_count DESC NULLS LAST, g.title"),
    POPULAR("g.igdb_rating_count DESC NULLS LAST, g.title"),
    RATING("g.igdb_rating DESC NULLS LAST, g.title"),
    RELEASE("g.release_date DESC NULLS LAST, g.title"),
    TITLE("g.title");

    private final String orderBy;

    GameSort(String orderBy) {
        this.orderBy = orderBy;
    }

    String orderBy() {
        return orderBy;
    }

    /** Aceita {@code ?sort=popular} além de {@code ?sort=POPULAR}. */
    @Component
    static class FromRequestParam implements Converter<String, GameSort> {
        @Override
        public GameSort convert(String source) {
            return valueOf(source.trim().toUpperCase(Locale.ROOT));
        }
    }
}
