package com.kauan.games_list.repositories;

import com.kauan.games_list.entities.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {

}
