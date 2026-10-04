package com.kauan.gamelog.social;

import com.kauan.gamelog.library.LibraryEntryChanged;
import com.kauan.gamelog.shared.UnprocessableException;
import com.kauan.gamelog.social.dto.LikedReviewDTO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/** Curtir avaliações (RF52, RN17). */
@Service
public class LikeService {
    private final ReviewLikes likes;
    private final ListedReviews listed;

    LikeService(ReviewLikes likes, ListedReviews listed) {
        this.likes = likes;
        this.listed = listed;
    }

    /** Só avaliações que aparecem nas listas, e nunca a própria. */
    @Transactional
    public void like(long userId, long entryId) {
        long authorId = listed.authorOf(entryId);
        if (authorId == userId) {
            throw new UnprocessableException(
                    "CANNOT_LIKE_OWN_REVIEW", "Não dá para curtir a própria avaliação.", List.of());
        }
        likes.add(userId, entryId);
    }

    @Transactional
    public void unlike(long userId, long entryId) {
        likes.remove(userId, entryId);
    }

    @Transactional(readOnly = true)
    public List<Long> likedAmong(long userId, List<Long> entryIds) {
        return entryIds.isEmpty() ? List.of() : likes.likedAmong(userId, entryIds);
    }

    @Transactional(readOnly = true)
    public List<LikedReviewDTO> likedBy(long userId) {
        return likes.likedBy(userId);
    }

    /** Sem texto, a avaliação sai das listas, e as curtidas dela vão junto. */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(LibraryEntryChanged event) {
        if (event.before() != null
                && event.after() != null
                && event.before().hasText()
                && !event.after().hasText()) {
            likes.removeAll(event.entryId());
        }
    }
}
