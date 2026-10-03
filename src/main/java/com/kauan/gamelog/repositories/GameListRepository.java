package com.kauan.gamelog.repositories;

import com.kauan.gamelog.entities.GameList;
import org.springframework.data.jpa.repository.JpaRepository;


public interface GameListRepository extends JpaRepository<GameList, Long> {

}
