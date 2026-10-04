package com.kauan.gamelog.lists;

import com.kauan.gamelog.lists.dto.ListDTO;
import com.kauan.gamelog.lists.dto.ListForm;
import com.kauan.gamelog.lists.dto.ListItemsRequest;
import com.kauan.gamelog.lists.dto.ListSummaryDTO;
import com.kauan.gamelog.shared.security.CurrentUserId;
import com.kauan.gamelog.shared.security.OpenApiConfig;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** As minhas listas, públicas e privadas; a de outra pessoa responde 404. */
@RestController
@RequestMapping("/me/lists")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class ListController {
    private final ListService listService;

    public ListController(ListService listService) {
        this.listService = listService;
    }

    @GetMapping
    public PagedModel<ListSummaryDTO> mine(@CurrentUserId Long userId, Pageable pageable) {
        return new PagedModel<>(listService.mine(userId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListDTO create(@CurrentUserId Long userId, @Valid @RequestBody ListForm form) {
        return listService.create(userId, form);
    }

    @GetMapping("/{listId}")
    public ListDTO get(@CurrentUserId Long userId, @PathVariable long listId) {
        return listService.get(userId, listId);
    }

    /** JSON Merge Patch: envie só o que muda; {@code null} limpa a descrição. */
    @PatchMapping("/{listId}")
    public ListDTO update(
            @CurrentUserId Long userId,
            @PathVariable long listId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(implementation = ListForm.class)))
                    @RequestBody
                    JsonNode patch) {
        return listService.update(userId, listId, patch);
    }

    @DeleteMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable long listId) {
        listService.delete(userId, listId);
    }

    /** Os itens na nova ordem, até 100; troca a lista inteira de uma vez. */
    @PutMapping("/{listId}/items")
    public ListDTO items(
            @CurrentUserId Long userId, @PathVariable long listId, @Valid @RequestBody ListItemsRequest request) {
        return listService.replaceItems(userId, listId, request);
    }
}
