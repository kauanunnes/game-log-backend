package com.kauan.gamelog.social;

import com.kauan.gamelog.library.LibraryEntryChanged;
import com.kauan.gamelog.library.LibraryEntryChanged.State;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Transforma mudanças na biblioteca em atividades do feed (RN16). Cada vez que a pessoa salva, entra no máximo
 * uma: a avaliação nova ou editada, senão o status novo (ou "zerou"), senão o favorito.
 */
@Component
class FeedRecorder {
    private final Activities activities;

    FeedRecorder(Activities activities) {
        this.activities = activities;
    }

    /** Depois do commit e numa transação própria: um erro aqui não desfaz o que a pessoa salvou. */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(LibraryEntryChanged event) {
        State before = event.before();
        State after = event.after();
        if (after == null) {
            return; // a entrada foi removida, e as atividades dela foram junto (ON DELETE CASCADE)
        }
        if (before != null && before.reviewed() && !after.reviewed()) {
            activities.remove(ActivityType.REVIEW, event.entryId());
        }
        if (before != null && before.favorite() && !after.favorite()) {
            activities.remove(ActivityType.FAVORITE, event.entryId());
        }

        if (after.reviewed() && (before == null || !Objects.equals(before.reviewedAt(), after.reviewedAt()))) {
            activities.replace(ActivityType.REVIEW, event.userId(), event.gameId(), event.entryId());
        } else if (before == null || before.status() != after.status() || (after.completed() && !before.completed())) {
            activities.addStatus(event.userId(), event.gameId(), event.entryId(), after.status(), after.completed());
        } else if (after.favorite() && !before.favorite()) {
            activities.replace(ActivityType.FAVORITE, event.userId(), event.gameId(), event.entryId());
        }
    }
}
