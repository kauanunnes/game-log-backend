package com.kauan.gamelog.social;

import com.kauan.gamelog.social.dto.ActivityDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Feed de atividade (RF51). As atividades nascem no {@link FeedRecorder}, a partir da biblioteca. */
@Service
public class FeedService {
    private final Activities activities;

    FeedService(Activities activities) {
        this.activities = activities;
    }

    @Transactional(readOnly = true)
    public Page<ActivityDTO> feed(long userId, Pageable pageable) {
        return activities.feed(userId, pageable);
    }
}
