package com.kauan.gamelog.export;

import com.kauan.gamelog.export.dto.ExportDTO;
import com.kauan.gamelog.library.LibraryService;
import com.kauan.gamelog.library.dto.LibraryFilter;
import com.kauan.gamelog.lists.ListService;
import com.kauan.gamelog.lists.dto.ListSummaryDTO;
import com.kauan.gamelog.social.FollowService;
import com.kauan.gamelog.social.LikeService;
import com.kauan.gamelog.user.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Junta os dados da conta pelos serviços públicos de cada módulo (RF10, RNF12). */
@Service
public class ExportService {
    private static final LibraryFilter EVERYTHING =
            new LibraryFilter(null, null, null, null, null, null, null, null, null);
    private static final int PAGE = 500;

    private final UserService users;
    private final LibraryService library;
    private final ListService lists;
    private final FollowService follows;
    private final LikeService likes;

    ExportService(
            UserService users, LibraryService library, ListService lists, FollowService follows, LikeService likes) {
        this.users = users;
        this.library = library;
        this.lists = lists;
        this.follows = follows;
        this.likes = likes;
    }

    @Transactional(readOnly = true)
    public ExportDTO export(long userId) {
        return new ExportDTO(
                Instant.now(),
                users.getMe(userId),
                all(page -> library.list(userId, EVERYTHING, page)),
                library.featured(userId),
                all(page -> lists.mine(userId, page)).stream()
                        .map(ListSummaryDTO::id)
                        .map(listId -> lists.get(userId, listId))
                        .toList(),
                all(page -> follows.following(userId, page)),
                all(page -> follows.followers(userId, page)),
                likes.likedBy(userId));
    }

    /** Percorre as páginas de uma consulta até o fim. */
    private static <T> List<T> all(Function<Pageable, Page<T>> query) {
        List<T> items = new ArrayList<>();
        Page<T> page = query.apply(PageRequest.of(0, PAGE));
        items.addAll(page.getContent());
        while (page.hasNext()) {
            page = query.apply(page.nextPageable());
            items.addAll(page.getContent());
        }
        return items;
    }
}
