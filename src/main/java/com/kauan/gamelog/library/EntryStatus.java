package com.kauan.gamelog.library;

import static com.kauan.gamelog.library.EntryStatus.Part.ACQUISITION;
import static com.kauan.gamelog.library.EntryStatus.Part.FAVORITE;
import static com.kauan.gamelog.library.EntryStatus.Part.FINISHED_ON;
import static com.kauan.gamelog.library.EntryStatus.Part.PLAYTHROUGH;
import static com.kauan.gamelog.library.EntryStatus.Part.REVIEW;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Os status de uma entrada e, como na tabela da RN02, as partes que cada um aceita. */
public enum EntryStatus {
    WISHLIST("Lista de desejos", EnumSet.noneOf(Part.class)),
    BACKLOG("Quero jogar", EnumSet.of(ACQUISITION)),
    PLAYING("Jogando", EnumSet.of(REVIEW, PLAYTHROUGH, FAVORITE, ACQUISITION)),
    PLAYED("Jogado", EnumSet.allOf(Part.class)),
    DROPPED("Abandonado", EnumSet.of(REVIEW, PLAYTHROUGH, FINISHED_ON, ACQUISITION));

    /** Partes da entrada que dependem do status. */
    public enum Part {
        REVIEW("review", "Avaliação"),
        PLAYTHROUGH("playthrough", "Plataforma, horas e início"),
        FINISHED_ON("playthrough.finishedOn", "Término"),
        COMPLETED("playthrough.completed", "Zerou"),
        FAVORITE("favorite", "Favorito"),
        ACQUISITION("acquisition", "Aquisição");

        private final String field;
        private final String label;

        Part(String field, String label) {
            this.field = field;
            this.label = label;
        }

        public String field() {
            return field;
        }

        /** Ex.: "Término só vale em Jogado ou Abandonado." */
        public String onlyAllowedMessage() {
            List<String> statuses = Arrays.stream(EntryStatus.values())
                    .filter(status -> status.allows(this))
                    .map(EntryStatus::label)
                    .toList();
            String last = statuses.getLast();
            String others = String.join(", ", statuses.subList(0, statuses.size() - 1));
            return label + " só vale em " + (others.isEmpty() ? last : others + " ou " + last) + ".";
        }
    }

    private final String label;
    private final Set<Part> allowed;

    EntryStatus(String label, Set<Part> allowed) {
        this.label = label;
        this.allowed = allowed;
    }

    public String label() {
        return label;
    }

    public boolean allows(Part part) {
        return allowed.contains(part);
    }
}
