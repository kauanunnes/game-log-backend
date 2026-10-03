package com.kauan.gamelog.dto;

import com.kauan.gamelog.entities.Game;
import com.kauan.gamelog.projections.GameMinProjection;

public class GameMinDTO {
    private Long id;
    private Integer year;
    private String title, imgUrl;
    private String shortDescription;

    public GameMinDTO() { }

    public Long getId() {
        return id;
    }

    public Integer getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public GameMinDTO(Game entity) {
        id = entity.getId();
        year = entity.getYear();
        title = entity.getTitle();
        imgUrl = entity.getImgUrl();
        shortDescription = entity.getShortDescription();
    }
    public GameMinDTO(GameMinProjection projection) {
        id = projection.getId();
        year = projection.getGameYear();
        title = projection.getTitle();
        imgUrl = projection.getImgUrl();
        shortDescription = projection.getShortDescription();
    }
}
