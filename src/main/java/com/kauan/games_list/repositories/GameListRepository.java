package com.kauan.games_list.repositories;

import com.kauan.games_list.entities.GameList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameListRepository extends JpaRepository<GameList, Long> {

}
