package com.kauan.gamelog.lists;

import com.kauan.gamelog.catalog.GameService;
import com.kauan.gamelog.lists.dto.ListDTO;
import com.kauan.gamelog.lists.dto.ListForm;
import com.kauan.gamelog.lists.dto.ListItemsRequest;
import com.kauan.gamelog.lists.dto.ListSummaryDTO;
import com.kauan.gamelog.shared.FieldIssue;
import com.kauan.gamelog.shared.JsonMergePatch;
import com.kauan.gamelog.shared.NotFoundException;
import com.kauan.gamelog.shared.UnprocessableException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/** Listas personalizadas (RF54, RN19). Quem visita só vê as públicas, e o módulo profile confere o perfil. */
@Service
public class ListService {
    private final UserLists lists;
    private final GameService games;
    private final JsonMergePatch mergePatch;

    ListService(UserLists lists, GameService games, JsonMergePatch mergePatch) {
        this.lists = lists;
        this.games = games;
        this.mergePatch = mergePatch;
    }

    @Transactional(readOnly = true)
    public Page<ListSummaryDTO> mine(long userId, Pageable pageable) {
        return lists.summaries(userId, false, pageable);
    }

    @Transactional
    public ListDTO create(long userId, ListForm form) {
        return get(userId, lists.create(userId, form));
    }

    @Transactional(readOnly = true)
    public ListDTO get(long userId, long listId) {
        return lists.find(listId, userId, false).orElseThrow(ListService::notFound);
    }

    /** JSON Merge Patch sobre título, descrição e visibilidade. */
    @Transactional
    public ListDTO update(long userId, long listId, JsonNode patch) {
        ListDTO current = get(userId, listId);
        ListForm form =
                mergePatch.apply(new ListForm(current.title(), current.description(), current.visibility()), patch);
        lists.updateMeta(listId, form);
        return get(userId, listId);
    }

    @Transactional
    public void delete(long userId, long listId) {
        if (!lists.delete(listId, userId)) {
            throw notFound();
        }
    }

    /** Troca os itens de uma vez, na ordem recebida; cada jogo entra uma vez. */
    @Transactional
    public ListDTO replaceItems(long userId, long listId, ListItemsRequest request) {
        if (!lists.owns(listId, userId)) {
            throw notFound();
        }
        List<Long> gameIds =
                request.items().stream().map(ListItemsRequest.Item::gameId).toList();
        Set<Long> unique = new HashSet<>(gameIds);
        if (unique.size() < gameIds.size()) {
            throw new UnprocessableException("DUPLICATE_GAME", "Cada jogo entra uma vez na lista.", List.of());
        }
        Set<Long> known = games.existing(unique);
        List<FieldIssue> unknown = IntStream.range(0, gameIds.size())
                .filter(i -> !known.contains(gameIds.get(i)))
                .mapToObj(i -> new FieldIssue("items[" + i + "].gameId", "jogo não encontrado"))
                .toList();
        if (!unknown.isEmpty()) {
            throw new UnprocessableException("UNKNOWN_REFERENCE", "Confira os campos indicados.", unknown);
        }
        lists.replaceItems(listId, request.items());
        return get(userId, listId);
    }

    @Transactional(readOnly = true)
    public Page<ListSummaryDTO> publicListsOf(long userId, Pageable pageable) {
        return lists.summaries(userId, true, pageable);
    }

    @Transactional(readOnly = true)
    public ListDTO publicListOf(long userId, long listId) {
        return lists.find(listId, userId, true).orElseThrow(ListService::notFound);
    }

    private static NotFoundException notFound() {
        return new NotFoundException("Lista não encontrada.");
    }
}
