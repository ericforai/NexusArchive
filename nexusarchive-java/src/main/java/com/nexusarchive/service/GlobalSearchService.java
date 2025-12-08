package com.nexusarchive.service;

import com.nexusarchive.dto.GlobalSearchDTO;
import java.util.List;

public interface GlobalSearchService {
    /**
     * Perform a global search across Archives and Metadata.
     *
     * @param query The search query string.
     * @return List of matching results.
     */
    List<GlobalSearchDTO> search(String query);
}
