package com.kauan.gamelog.recommendation;

import com.kauan.gamelog.catalog.dto.GameProfile;
import java.math.BigDecimal;
import java.util.List;

/** Escolhe e explica, entre os candidatos da busca, as sugestões para a pessoa (RF62). */
public interface Curator {
    /** Sem chave da API, as sugestões ficam só com a busca. */
    boolean enabled();

    /** @return as escolhas em ordem, só entre os candidatos; vazio quando não deu para escolher */
    List<Pick> curate(Input input);

    record Pick(long gameId, String reason) {}

    /**
     * O que vai para o modelo: nada de e-mail, username ou gênero (06 · Recomendações).
     *
     * @param liked os jogos de que a pessoa mais gostou, do mais forte para o mais fraco
     * @param disliked os títulos dos jogos de que ela não gostou
     * @param candidates os candidatos da busca, já filtrados, na ordem da busca
     */
    record Input(List<Liked> liked, List<String> disliked, List<GameProfile> candidates) {}

    /** @param review um trecho da avaliação da própria pessoa, ou {@code null} */
    record Liked(String title, boolean favorite, BigDecimal rating, Boolean recommends, String review) {}
}
