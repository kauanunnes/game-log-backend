package com.kauan.gamelog.social;

import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import com.kauan.gamelog.social.dto.ActivityDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/feed")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class FeedController {
    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    /** O que fizeram as pessoas que eu sigo, das atividades mais recentes para as mais antigas (RF51). */
    @GetMapping
    public PagedModel<ActivityDTO> feed(@CurrentUserId Long userId, Pageable pageable) {
        return new PagedModel<>(feedService.feed(userId, pageable));
    }
}
