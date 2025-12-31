// Input: Archive Entity, ArchiveMapper
// Output: AdvancedArchiveSearchService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.dto.request.AdvancedSearchRequest;
import com.nexusarchive.dto.response.ArchiveSearchResult;

/**
 * 高级档案检索服务
 * 
 * 功能：
 * 1. 金额范围查询
 * 2. 摘要搜索
 * 3. 多条件组合查询
 * 
 * PRD 来源: Section 2.1 - 高级检索与脱敏
 */
public interface AdvancedArchiveSearchService {
    
    /**
     * 高级检索
     * 
     * @param request 检索请求
     * @return 检索结果（分页）
     */
    Page<ArchiveSearchResult> advancedSearch(AdvancedSearchRequest request);
    
    /**
     * 按金额范围查询
     * 
     * @param fondsNo 全宗号
     * @param minAmount 最小金额
     * @param maxAmount 最大金额
     * @param page 页码
     * @param pageSize 每页大小
     * @return 检索结果
     */
    Page<ArchiveSearchResult> searchByAmountRange(String fondsNo, 
                                                   java.math.BigDecimal minAmount, 
                                                   java.math.BigDecimal maxAmount,
                                                   Integer page, 
                                                   Integer pageSize);
    
    /**
     * 按摘要搜索
     * 
     * @param fondsNo 全宗号
     * @param summary 摘要关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 检索结果
     */
    Page<ArchiveSearchResult> searchBySummary(String fondsNo, 
                                              String summary,
                                              Integer page, 
                                              Integer pageSize);
}

