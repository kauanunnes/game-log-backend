package com.kauan.gamelog.social;

import com.kauan.gamelog.shared.NotFoundException;
import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import com.kauan.gamelog.social.dto.FollowDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Quem está logado segue e deixa de seguir; as próprias listas valem mesmo com o perfil privado. */
@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class FollowController {
    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    /** 204 se eu sigo a pessoa; 404 se não. */
    @GetMapping("/users/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void check(@CurrentUserId Long userId, @PathVariable String username) {
        if (!followService.isFollowing(userId, username)) {
            throw new NotFoundException("Você não segue \"" + username + "\".");
        }
    }

    /** Seguir de novo não muda nada. */
    @PutMapping("/users/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@CurrentUserId Long userId, @PathVariable String username) {
        followService.follow(userId, username);
    }

    @DeleteMapping("/users/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@CurrentUserId Long userId, @PathVariable String username) {
        followService.unfollow(userId, username);
    }

    @GetMapping("/me/followers")
    public PagedModel<FollowDTO> myFollowers(@CurrentUserId Long userId, Pageable pageable) {
        return new PagedModel<>(followService.followers(userId, pageable));
    }

    @GetMapping("/me/following")
    public PagedModel<FollowDTO> myFollowing(@CurrentUserId Long userId, Pageable pageable) {
        return new PagedModel<>(followService.following(userId, pageable));
    }
}
