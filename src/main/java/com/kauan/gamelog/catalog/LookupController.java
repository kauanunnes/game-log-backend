package com.kauan.gamelog.catalog;

import com.kauan.gamelog.catalog.dto.GenreDTO;
import com.kauan.gamelog.catalog.dto.PlatformDTO;
import com.kauan.gamelog.catalog.dto.StoreDTO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LookupController {
    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/genres")
    public List<GenreDTO> genres() {
        return lookupService.genres();
    }

    @GetMapping("/platforms")
    public List<PlatformDTO> platforms() {
        return lookupService.platforms();
    }

    @GetMapping("/stores")
    public List<StoreDTO> stores() {
        return lookupService.stores();
    }
}
