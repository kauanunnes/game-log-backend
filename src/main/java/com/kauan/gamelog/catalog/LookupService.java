package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GenreDTO;
import com.kauan.gamelog.catalog.dto.PlatformDTO;
import com.kauan.gamelog.catalog.dto.StoreDTO;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LookupService {
    private static final Sort BY_NAME = Sort.by("name");

    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;
    private final StoreRepository storeRepository;

    public LookupService(
            GenreRepository genreRepository, PlatformRepository platformRepository, StoreRepository storeRepository) {
        this.genreRepository = genreRepository;
        this.platformRepository = platformRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public List<GenreDTO> genres() {
        return genreRepository.findAll(BY_NAME).stream().map(GenreDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformDTO> platforms() {
        return platformRepository.findAll(BY_NAME).stream()
                .map(PlatformDTO::from)
                .toList();
    }

    /** Na ordem de cadastro, que deixa "Loja física" e "Outra" por último. */
    @Transactional(readOnly = true)
    public List<StoreDTO> stores() {
        return storeRepository.findAll(Sort.by("id")).stream()
                .map(StoreDTO::from)
                .toList();
    }
}
