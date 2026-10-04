package com.kauan.gamelog.profile;

import com.kauan.gamelog.library.dto.LibraryEntryDTO;
import com.kauan.gamelog.library.dto.LibraryFilter;
import com.kauan.gamelog.library.dto.PublicReviewDTO;
import com.kauan.gamelog.library.dto.StatsDTO;
import com.kauan.gamelog.library.dto.StatsFilter;
import com.kauan.gamelog.profile.dto.ProfileDTO;
import com.kauan.gamelog.social.dto.FollowDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfis públicos (RF40, RF41): não exigem login. */
@RestController
@RequestMapping("/users/{username}")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileDTO profile(@PathVariable String username) {
        return profileService.profile(username);
    }

    /** Mesmos filtros de {@code /me/library}; as abas Jogados, Jogando etc. usam {@code ?status=}. */
    @GetMapping("/library")
    public PagedModel<LibraryEntryDTO> library(
            @PathVariable String username, @Valid LibraryFilter filter, Pageable pageable) {
        return new PagedModel<>(profileService.library(username, filter, pageable));
    }

    @GetMapping("/favorites")
    public PagedModel<LibraryEntryDTO> favorites(@PathVariable String username, Pageable pageable) {
        return new PagedModel<>(profileService.favorites(username, pageable));
    }

    /** Sem gastos, salvo se o dono permitir. */
    @GetMapping("/stats")
    public StatsDTO stats(@PathVariable String username, @Valid StatsFilter filter) {
        return profileService.stats(username, filter.year());
    }

    @GetMapping("/reviews")
    public PagedModel<PublicReviewDTO> reviews(@PathVariable String username, Pageable pageable) {
        return new PagedModel<>(profileService.reviews(username, pageable));
    }

    /** Dos mais recentes para os mais antigos; 403 se o perfil for privado. */
    @GetMapping("/followers")
    public PagedModel<FollowDTO> followers(@PathVariable String username, Pageable pageable) {
        return new PagedModel<>(profileService.followers(username, pageable));
    }

    @GetMapping("/following")
    public PagedModel<FollowDTO> following(@PathVariable String username, Pageable pageable) {
        return new PagedModel<>(profileService.following(username, pageable));
    }
}
