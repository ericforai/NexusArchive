package com.nexusarchive.service;

import com.nexusarchive.entity.es.ArchiveDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ArchiveSearchService {
    
    /**
     * 索引单个档案
     * @param archiveId 档案ID
     */
    void indexArchive(String archiveId);
    
    /**
     * 批量索引
     * @param archiveIds 档案ID列表
     */
    void batchIndex(List<String> archiveIds);
    
    /**
     * 全文检索
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 包含高亮信息的搜索结果
     */
    Map<String, Object> search(String keyword, Pageable pageable);
    
    /**
     * 删除索引
     * @param archiveId 档案ID
     */
    void deleteIndex(String archiveId);

    /**
     * 重建所有索引
     */
    void reindexAll();
}
