package com.kauan.gamelog.social;

/** O que a moderação faz com a avaliação denunciada. */
public enum ReportDecision {
    KEEP,
    /** Tira o texto; a nota e o resto da entrada ficam. */
    REMOVE
}
