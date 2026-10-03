package com.kauan.gamelog.catalog.dto;

import com.kauan.gamelog.catalog.Game;

public record GameMinDTO(Long id, Integer year, String title, String imgUrl, String shortDescription) {
    public GameMinDTO(Game entity) {
        this(entity.getId(), entity.getYear(), entity.getTitle(), entity.getImgUrl(), entity.getShortDescription());
    }
}
