package com.kauan.gamelog.library;

import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Ordem das avaliações de um jogo (RF24): as mais recentes ou as mais curtidas (RF52). */
public enum ReviewSort {
    RECENT("e.reviewed_at DESC, e.id DESC"),
    LIKES("likes DESC, e.reviewed_at DESC, e.id DESC");

    private final String orderBy;

    ReviewSort(String orderBy) {
        this.orderBy = orderBy;
    }

    String orderBy() {
        return orderBy;
    }

    /** Aceita {@code ?sort=likes} além de {@code ?sort=LIKES}. */
    @Component
    static class FromRequestParam implements Converter<String, ReviewSort> {
        @Override
        public ReviewSort convert(String source) {
            return valueOf(source.trim().toUpperCase(Locale.ROOT));
        }
    }
}
