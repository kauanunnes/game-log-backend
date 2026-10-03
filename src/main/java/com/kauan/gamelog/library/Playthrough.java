package com.kauan.gamelog.library;

import com.kauan.gamelog.library.dto.PlaythroughDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** @param completed "zerou" */
@Embeddable
record Playthrough(
        @Column(name = "played_platform_id") Long platformId,
        Integer hoursPlayed,
        LocalDate startedOn,
        LocalDate finishedOn,
        Boolean completed) {

    static Playthrough from(PlaythroughDTO dto) {
        return dto == null
                ? null
                : new Playthrough(
                        dto.platformId(), dto.hoursPlayed(), dto.startedOn(), dto.finishedOn(), dto.completed());
    }
}
