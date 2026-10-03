package com.kauan.gamelog.library.dto;

/** Contadores das abas do perfil. */
public record LibraryCounts(
        long played, long playing, long backlog, long wishlist, long dropped, long favorites, long reviews) {}
