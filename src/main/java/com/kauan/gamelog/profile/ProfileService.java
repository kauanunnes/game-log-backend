package com.kauan.gamelog.profile;

import com.kauan.gamelog.library.LibraryService;
import com.kauan.gamelog.library.dto.LibraryEntryDTO;
import com.kauan.gamelog.library.dto.LibraryFilter;
import com.kauan.gamelog.library.dto.PublicReviewDTO;
import com.kauan.gamelog.library.dto.StatsDTO;
import com.kauan.gamelog.lists.ListService;
import com.kauan.gamelog.lists.dto.ListDTO;
import com.kauan.gamelog.lists.dto.ListSummaryDTO;
import com.kauan.gamelog.profile.dto.ProfileCounts;
import com.kauan.gamelog.profile.dto.ProfileDTO;
import com.kauan.gamelog.shared.ForbiddenException;
import com.kauan.gamelog.social.FollowService;
import com.kauan.gamelog.social.dto.FollowDTO;
import com.kauan.gamelog.user.PublicUser;
import com.kauan.gamelog.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leitura pública dos perfis. Responde igual para qualquer pessoa, inclusive o dono: os dados privados dele saem
 * pelas rotas de {@code /me}.
 */
@Service
public class ProfileService {
    private static final LibraryFilter FAVORITES =
            new LibraryFilter(null, true, null, null, null, null, null, null, null);

    private final UserService users;
    private final LibraryService library;
    private final FollowService follows;
    private final ListService lists;

    ProfileService(UserService users, LibraryService library, FollowService follows, ListService lists) {
        this.users = users;
        this.library = library;
        this.follows = follows;
        this.lists = lists;
    }

    @Transactional(readOnly = true)
    public ProfileDTO profile(String username) {
        PublicUser user = users.getPublic(username);
        return user.privateProfile() ? ProfileDTO.privateHeader(user) : header(user);
    }

    /** O cabeçalho completo para o dono, mesmo com o perfil privado. */
    @Transactional(readOnly = true)
    public ProfileDTO mine(long userId) {
        return header(users.getPublic(userId));
    }

    @Transactional(readOnly = true)
    public Page<LibraryEntryDTO> library(String username, LibraryFilter filter, Pageable pageable) {
        PublicUser user = findVisible(username);
        return hideSpending(user, library.list(user.id(), filter, pageable));
    }

    @Transactional(readOnly = true)
    public Page<LibraryEntryDTO> favorites(String username, Pageable pageable) {
        PublicUser user = findVisible(username);
        return hideSpending(user, library.list(user.id(), FAVORITES, pageable));
    }

    /** No formato das avaliações públicas, que nunca trazem loja nem valor pago. */
    @Transactional(readOnly = true)
    public Page<PublicReviewDTO> reviews(String username, Pageable pageable) {
        return library.reviewsBy(findVisible(username).id(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<FollowDTO> followers(String username, Pageable pageable) {
        return follows.followers(findVisible(username).id(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<FollowDTO> following(String username, Pageable pageable) {
        return follows.following(findVisible(username).id(), pageable);
    }

    /** Só as listas públicas; 403 se o perfil for privado. */
    @Transactional(readOnly = true)
    public Page<ListSummaryDTO> lists(String username, Pageable pageable) {
        return lists.publicListsOf(findVisible(username).id(), pageable);
    }

    @Transactional(readOnly = true)
    public ListDTO list(String username, long listId) {
        return lists.publicListOf(findVisible(username).id(), listId);
    }

    @Transactional(readOnly = true)
    public StatsDTO stats(String username, Integer year) {
        PublicUser user = findVisible(username);
        StatsDTO stats = library.stats(user.id(), year);
        return user.showSpending() ? stats : stats.withoutSpending();
    }

    private ProfileDTO header(PublicUser user) {
        return ProfileDTO.of(user, new ProfileCounts(library.counts(user.id()), follows.counts(user.id())));
    }

    /** RF42: num perfil privado, quem visita vê só o cabeçalho. */
    private PublicUser findVisible(String username) {
        PublicUser user = users.getPublic(username);
        if (user.privateProfile()) {
            throw new ForbiddenException("Este perfil é privado.");
        }
        return user;
    }

    /** RN10: loja e valor pago só aparecem se o dono ativou "mostrar gastos". */
    private static Page<LibraryEntryDTO> hideSpending(PublicUser user, Page<LibraryEntryDTO> page) {
        return user.showSpending() ? page : page.map(LibraryEntryDTO::withoutSpending);
    }
}
