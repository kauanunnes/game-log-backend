package com.kauan.gamelog.profile;

import com.kauan.gamelog.library.LibraryService;
import com.kauan.gamelog.library.dto.LibraryEntryDTO;
import com.kauan.gamelog.library.dto.LibraryFilter;
import com.kauan.gamelog.library.dto.StatsDTO;
import com.kauan.gamelog.profile.dto.ProfileDTO;
import com.kauan.gamelog.shared.ForbiddenException;
import com.kauan.gamelog.shared.NotFoundException;
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

    ProfileService(UserService users, LibraryService library) {
        this.users = users;
        this.library = library;
    }

    @Transactional(readOnly = true)
    public ProfileDTO profile(String username) {
        PublicUser user = find(username);
        return user.privateProfile() ? ProfileDTO.privateHeader(user) : ProfileDTO.of(user, library.counts(user.id()));
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

    @Transactional(readOnly = true)
    public Page<LibraryEntryDTO> reviews(String username, Pageable pageable) {
        PublicUser user = findVisible(username);
        return hideSpending(user, library.reviews(user.id(), pageable));
    }

    @Transactional(readOnly = true)
    public StatsDTO stats(String username, Integer year) {
        PublicUser user = findVisible(username);
        StatsDTO stats = library.stats(user.id(), year);
        return user.showSpending() ? stats : stats.withoutSpending();
    }

    private PublicUser find(String username) {
        return users.findPublic(username)
                .orElseThrow(() -> new NotFoundException("Ninguém usa o username \"" + username + "\"."));
    }

    /** RF42: num perfil privado, quem visita vê só o cabeçalho. */
    private PublicUser findVisible(String username) {
        PublicUser user = find(username);
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
