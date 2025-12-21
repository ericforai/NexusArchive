package com.nexusarchive.controller;

import com.nexusarchive.dto.GlobalSearchDTO;
import com.nexusarchive.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public List<GlobalSearchDTO> search(@RequestParam("q") String query) {
        return globalSearchService.search(query);
    }
}
