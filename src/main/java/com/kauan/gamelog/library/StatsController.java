package com.kauan.gamelog.library;

import com.kauan.gamelog.library.dto.StatsDTO;
import com.kauan.gamelog.library.dto.StatsFilter;
import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/stats")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class StatsController {
    private final LibraryService libraryService;

    public StatsController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /** Completas, inclusive os gastos. */
    @GetMapping
    public StatsDTO stats(@CurrentUserId Long userId, @Valid StatsFilter filter) {
        return libraryService.stats(userId, filter.year());
    }
}
