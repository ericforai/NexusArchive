// Input: MyBatis-Plus、Jakarta EE、Lombok、Spring Framework、等
// Output: ArchiveIndexService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.dto.search.ArchiveDocument;
import com.nexusarchive.dto.search.SearchResult;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 档案全文检索索引服务
 * 提供 Elasticsearch 索引管理和搜索功能
 * ES 不可用时自动降级到数据库 LIKE 查询
 * 
 * Modified: Disabled ES due to missing dependencies in offline environment.
 */
@Service
@Slf4j
public class ArchiveIndexService {

    private static final String INDEX_NAME = "nexus_archives";

    // private final ElasticsearchClient esClient;
    private final ArchiveMapper archiveMapper;
    private final boolean esEnabled = false; // Forced to false

    @Autowired
    public ArchiveIndexService(ArchiveMapper archiveMapper) {
        this.archiveMapper = archiveMapper;
        log.info("Elasticsearch disabled (Offline Mode). Search will use Database fallback.");
    }

    /**
     * 初始化索引
     */
    @PostConstruct
    public void init() {
        // Disabled
    }

    /**
     * 索引单个档案
     */
    public void indexArchive(Archive archive) {
        // Disabled
    }

    /**
     * 删除档案索引
     */
    public void deleteIndex(String archiveId) {
        // Disabled
    }

    /**
     * 全文检索
     * ES 不可用时降级到数据库查询
     */
    public SearchResult<ArchiveDocument> search(String keyword, int page, int size) {
        return searchFromDatabase(keyword, page, size);
    }

    /**
     * 全量重建索引
     */
    public int reindexAll() {
        log.warn("Elasticsearch 未启用，无法重建索引");
        return 0;
    }

    private ArchiveDocument toDocument(Archive archive) {
        return ArchiveDocument.builder()
            .id(archive.getId())
            .title(archive.getTitle())
            .archivalCode(archive.getArchiveCode())
            .summary(archive.getSummary())
            .categoryCode(archive.getCategoryCode())
            .fondsCode(archive.getFondsNo())
            .departmentId(archive.getDepartmentId())
            .departmentName(archive.getOrgName())
            .fiscalYear(archive.getFiscalYear())
            .retentionPeriod(archive.getRetentionPeriod())
            .status(archive.getStatus())
            .createdTime(archive.getCreatedTime())
            .fullText(null)
            .build();
    }

    /**
     * 数据库降级查询
     */
    private SearchResult<ArchiveDocument> searchFromDatabase(String keyword, int page, int size) {
        log.debug("使用数据库查询: keyword={}", keyword);
        
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(Archive::getTitle, keyword)
                .or().like(Archive::getArchiveCode, keyword)
                .or().like(Archive::getSummary, keyword)
                .or().like(Archive::getOrgName, keyword)
            );
        }
        wrapper.orderByDesc(Archive::getCreatedTime);
        
        Page<Archive> archivePage = new Page<>(page + 1, size);
        archiveMapper.selectPage(archivePage, wrapper);
        
        List<ArchiveDocument> items = archivePage.getRecords().stream()
            .map(this::toDocument)
            .collect(Collectors.toList());
        
        return SearchResult.ofFallback(items, archivePage.getTotal(), page, size);
    }
}
