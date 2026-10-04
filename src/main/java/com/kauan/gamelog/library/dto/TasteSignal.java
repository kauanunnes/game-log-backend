package com.kauan.gamelog.library.dto;

import com.kauan.gamelog.library.EntryStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Uma entrada da biblioteca vista como sinal de gosto, para as recomendações. */
public record TasteSignal(
        long gameId, EntryStatus status, boolean favorite, BigDecimal rating, Boolean recommends, Instant updatedAt) {}
