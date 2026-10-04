package com.kauan.gamelog.library;

import com.kauan.gamelog.library.dto.PublicReviewDTO;
import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Avaliações com texto: as públicas, só de perfis públicos, e as minhas. */
@RestController
public class ReviewsController {
    private final LibraryService libraryService;

    public ReviewsController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /** Das mais recentes para as mais antigas, ou das mais curtidas com {@code ?sort=likes}. */
    @GetMapping("/games/{slug}/reviews")
    public PagedModel<PublicReviewDTO> ofGame(
            @PathVariable String slug, @RequestParam(defaultValue = "recent") ReviewSort sort, Pageable pageable) {
        return new PagedModel<>(libraryService.reviewsOf(slug, sort, pageable));
    }

    @GetMapping("/reviews")
    public PagedModel<PublicReviewDTO> recent(Pageable pageable) {
        return new PagedModel<>(libraryService.recentReviews(pageable));
    }

    /** As minhas, das editadas por último, mesmo com o perfil privado. */
    @GetMapping("/me/reviews")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    public PagedModel<PublicReviewDTO> mine(@CurrentUserId Long userId, Pageable pageable) {
        return new PagedModel<>(libraryService.reviewsBy(userId, pageable));
    }
}
