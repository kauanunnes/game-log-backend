package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GameDetailsDTO;
import com.kauan.gamelog.catalog.dto.ImportGameRequest;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Só para o papel ADMIN (ver {@code SecurityConfig}). */
@RestController
@RequestMapping("/admin/games")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class AdminGameController {
    private final GameService gameService;

    public AdminGameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/import")
    public GameDetailsDTO importGame(@Valid @RequestBody ImportGameRequest request) {
        return gameService.importFromIgdb(request.igdbId());
    }

    @PostMapping("/{id}/sync")
    public GameDetailsDTO sync(@PathVariable long id) {
        return gameService.syncWithIgdb(id);
    }
}
