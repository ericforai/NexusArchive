package com.nexusarchive.service.impl;

import com.nexusarchive.service.ArchiveSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveSearchServiceImpl implements ArchiveSearchService {

    // private final ArchiveSearchRepository archiveSearchRepository;
    // private final ArchiveService archiveService;
    // private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void indexArchive(String archiveId) {
        log.warn("Search indexing is disabled (Offline Mode). archiveId={}", archiveId);
    }

    @Override
    public void batchIndex(List<String> archiveIds) {
        log.warn("Batch indexing is disabled (Offline Mode). count={}", archiveIds.size());
    }

    @Override
    public Map<String, Object> search(String keyword, Pageable pageable) {
        log.warn("Search is disabled (Offline Mode). keyword={}", keyword);
        return Collections.emptyMap();
    }

    @Override
    public void deleteIndex(String archiveId) {
        log.warn("Delete index is disabled (Offline Mode). archiveId={}", archiveId);
    }

    @Override
    public void reindexAll() {
        log.warn("Reindex all is disabled (Offline Mode).");
    }
}
