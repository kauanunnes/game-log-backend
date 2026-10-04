package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GenreDTO;
import com.kauan.gamelog.catalog.dto.PlatformDTO;
import com.kauan.gamelog.catalog.dto.StoreDTO;
import com.kauan.gamelog.shared.Caches;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

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

    @Cacheable(cacheNames = Caches.LOOKUPS, key = "'genres'")
    @Transactional(readOnly = true)
    public List<GenreDTO> genres() {
        return genreRepository.findAll(BY_NAME).stream().map(GenreDTO::from).toList();
    }

    @Cacheable(cacheNames = Caches.LOOKUPS, key = "'platforms'")
    @Transactional(readOnly = true)
    public List<PlatformDTO> platforms() {
        return platformRepository.findAll(BY_NAME).stream()
                .map(PlatformDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean platformExists(long id) {
        return platformRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public boolean storeExists(long id) {
        return storeRepository.existsById(id);
    }

    /** Na ordem de cadastro, que deixa "Loja física" e "Outra" por último. */
    @Cacheable(cacheNames = Caches.LOOKUPS, key = "'stores'")
    @Transactional(readOnly = true)
    public List<StoreDTO> stores() {
        return storeRepository.findAll(Sort.by("id")).stream()
                .map(StoreDTO::from)
                .toList();
    }

    /** A importação pode ter trazido gêneros e plataformas novos. */
    @TransactionalEventListener
    @CacheEvict(cacheNames = Caches.LOOKUPS, allEntries = true)
    public void forgetLists(GameImported event) {}
}
