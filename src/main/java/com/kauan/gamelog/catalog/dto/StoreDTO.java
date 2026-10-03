package com.kauan.gamelog.catalog.dto;

import com.kauan.gamelog.catalog.Store;

public record StoreDTO(Long id, String name, String slug) {
    public static StoreDTO from(Store store) {
        return new StoreDTO(store.getId(), store.getName(), store.getSlug());
    }
}
