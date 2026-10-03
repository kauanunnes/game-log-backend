package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.CommunityDTO;

/**
 * Números da comunidade de um jogo. Quem implementa é a biblioteca: assim o catálogo não depende dela, e a
 * dependência entre os módulos continua num sentido só.
 */
public interface GameCommunity {
    CommunityDTO of(long gameId);
}
