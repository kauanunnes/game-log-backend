package com.kauan.gamelog.library;

import com.kauan.gamelog.library.dto.LibraryEntryRequest;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Um jogo na biblioteca de alguém (RN01: um por usuário e jogo). As leituras saem de {@link LibraryQueries}. */
@Entity
@Table(name = "library_entries")
class LibraryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long gameId;

    @Enumerated(EnumType.STRING)
    private EntryStatus status;

    private boolean favorite;

    /** De 1 a 5 entre os favoritos em destaque (RF38); muda por {@link LibraryEntryRepository#feature}. */
    private Short favoritePosition;

    @Embedded
    private Review review;

    @Embedded
    private Playthrough playthrough;

    @Embedded
    private Acquisition acquisition;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected LibraryEntry() {}

    LibraryEntry(long userId, long gameId) {
        this.userId = userId;
        this.gameId = gameId;
    }

    /** Substitui a entrada inteira; quem chama já validou com {@link EntryRules}. */
    void replace(LibraryEntryRequest request) {
        status = request.status();
        favorite = request.favorite();
        if (!favorite) {
            favoritePosition = null;
        }
        review = Review.from(request.review(), review);
        playthrough = Playthrough.from(request.playthrough());
        acquisition = Acquisition.from(request.acquisition());
    }

    void removeReviewText() {
        review = review == null ? null : review.withoutText();
    }

    /** Só o que interessa a quem ouve {@link LibraryEntryChanged}. */
    LibraryEntryChanged.State state() {
        return new LibraryEntryChanged.State(
                status,
                favorite,
                playthrough != null && Boolean.TRUE.equals(playthrough.completed()),
                review != null && (review.rating() != null || review.text() != null),
                review != null && review.text() != null,
                review == null ? null : review.reviewedAt());
    }

    Long getId() {
        return id;
    }

    Long getUserId() {
        return userId;
    }

    Long getGameId() {
        return gameId;
    }
}
