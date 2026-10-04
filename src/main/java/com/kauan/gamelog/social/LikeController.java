package com.kauan.gamelog.social;

import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Curtir e descurtir avaliações; as listas de avaliações respondem igual para todos e trazem só a contagem. */
@RestController
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class LikeController {
    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /** Curtir de novo não muda nada. */
    @PutMapping("/reviews/{entryId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@CurrentUserId Long userId, @PathVariable long entryId) {
        likeService.like(userId, entryId);
    }

    @DeleteMapping("/reviews/{entryId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@CurrentUserId Long userId, @PathVariable long entryId) {
        likeService.unlike(userId, entryId);
    }

    /** Quais destas avaliações eu curti; o front pergunta pelas que está mostrando. */
    @GetMapping("/me/likes")
    public List<Long> liked(
            @CurrentUserId Long userId,
            @RequestParam @Size(max = 100, message = "envie no máximo 100 ids") List<Long> entryIds) {
        return likeService.likedAmong(userId, entryIds);
    }
}
