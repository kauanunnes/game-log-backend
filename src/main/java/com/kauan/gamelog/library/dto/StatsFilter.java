package com.kauan.gamelog.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** @param year opcional: só os jogos terminados (e as compras feitas) nesse ano */
public record StatsFilter(
        @Min(value = 1950, message = "use um ano entre 1950 e 2100")
        @Max(value = 2100, message = "use um ano entre 1950 e 2100")
        Integer year) {}
