package com.kauan.gamelog.library;

import static com.kauan.gamelog.shared.WebConfig.API_PREFIX;

import com.kauan.gamelog.library.LibraryService.Saved;
import com.kauan.gamelog.library.dto.LibraryEntryDTO;
import com.kauan.gamelog.library.dto.LibraryEntryRequest;
import com.kauan.gamelog.library.dto.LibraryFilter;
import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** A entrada é identificada por usuário + jogo, então o caminho usa o id do jogo. */
@RestController
@RequestMapping("/me/library")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class LibraryController {
    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /** Ordena por {@code createdAt}, {@code updatedAt}, {@code rating}, {@code title} ou {@code finishedOn}. */
    @GetMapping
    public PagedModel<LibraryEntryDTO> list(
            @CurrentUserId Long userId, @Valid LibraryFilter filter, Pageable pageable) {
        return new PagedModel<>(libraryService.list(userId, filter, pageable));
    }

    @GetMapping("/{gameId}")
    public LibraryEntryDTO get(@CurrentUserId Long userId, @PathVariable long gameId) {
        return libraryService.get(userId, gameId);
    }

    @PutMapping("/{gameId}")
    public ResponseEntity<LibraryEntryDTO> put(
            @CurrentUserId Long userId, @PathVariable long gameId, @Valid @RequestBody LibraryEntryRequest request) {
        Saved saved = libraryService.put(userId, gameId, request);
        return saved.created()
                ? ResponseEntity.created(URI.create(API_PREFIX + "/me/library/" + gameId))
                        .body(saved.entry())
                : ResponseEntity.ok(saved.entry());
    }

    /** JSON Merge Patch: envie só o que muda, como {@code {"status": "PLAYED"}}; {@code null} limpa. */
    @PatchMapping("/{gameId}")
    public LibraryEntryDTO patch(
            @CurrentUserId Long userId,
            @PathVariable long gameId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(implementation = LibraryEntryRequest.class)))
                    @RequestBody
                    JsonNode patch) {
        return libraryService.patch(userId, gameId, patch);
    }

    @DeleteMapping("/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable long gameId) {
        libraryService.delete(userId, gameId);
    }
}
