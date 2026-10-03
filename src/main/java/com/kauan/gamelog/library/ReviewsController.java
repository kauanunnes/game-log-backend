package com.kauan.gamelog.library;

import com.kauan.gamelog.library.dto.PublicReviewDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Avaliações públicas: só de perfis públicos e com texto, das mais recentes para as mais antigas. */
@RestController
public class ReviewsController {
    private final LibraryService libraryService;

    public ReviewsController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/games/{slug}/reviews")
    public PagedModel<PublicReviewDTO> ofGame(@PathVariable String slug, Pageable pageable) {
        return new PagedModel<>(libraryService.reviewsOf(slug, pageable));
    }

    @GetMapping("/reviews")
    public PagedModel<PublicReviewDTO> recent(Pageable pageable) {
        return new PagedModel<>(libraryService.recentReviews(pageable));
    }
}
