package com.kauan.games_list.dto;

import com.kauan.games_list.entities.GameList;

public class GameListDTO {
    private Long id;
    private String name;

    public GameListDTO() {
    }

    public GameListDTO(GameList list) {
        id = list.getId();
        name = list.getName();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
