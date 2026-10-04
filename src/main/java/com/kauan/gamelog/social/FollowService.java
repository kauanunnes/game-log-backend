package com.kauan.gamelog.social;

import com.kauan.gamelog.shared.UnprocessableException;
import com.kauan.gamelog.social.Follows.Side;
import com.kauan.gamelog.social.dto.FollowCounts;
import com.kauan.gamelog.social.dto.FollowDTO;
import com.kauan.gamelog.user.UserService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seguir e deixar de seguir (RF50). Qualquer perfil pode ser seguido, inclusive um privado; o que aparece dele
 * continua valendo a RN10, e quem decide isso é o módulo profile.
 */
@Service
public class FollowService {
    private final Follows follows;
    private final UserService users;

    FollowService(Follows follows, UserService users) {
        this.follows = follows;
        this.users = users;
    }

    @Transactional
    public void follow(long userId, String username) {
        long followeeId = users.getPublic(username).id();
        if (followeeId == userId) {
            throw new UnprocessableException("CANNOT_FOLLOW_SELF", "Não dá para seguir o próprio perfil.", List.of());
        }
        follows.add(userId, followeeId);
    }

    @Transactional
    public void unfollow(long userId, String username) {
        follows.remove(userId, users.getPublic(username).id());
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(long userId, String username) {
        return follows.exists(userId, users.getPublic(username).id());
    }

    @Transactional(readOnly = true)
    public FollowCounts counts(long userId) {
        return follows.counts(userId);
    }

    @Transactional(readOnly = true)
    public Page<FollowDTO> followers(long userId, Pageable pageable) {
        return follows.list(userId, Side.FOLLOWERS, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FollowDTO> following(long userId, Pageable pageable) {
        return follows.list(userId, Side.FOLLOWING, pageable);
    }
}
