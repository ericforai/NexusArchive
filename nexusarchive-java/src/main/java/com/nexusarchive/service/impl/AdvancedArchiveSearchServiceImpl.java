// Input: AdvancedArchiveSearchService, ArchiveMapper, Archive Entity
// Output: AdvancedArchiveSearchServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.dto.request.AdvancedSearchRequest;
import com.nexusarchive.dto.response.ArchiveSearchResult;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.AdvancedArchiveSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 高级档案检索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedArchiveSearchServiceImpl implements AdvancedArchiveSearchService {
    
    private final ArchiveMapper archiveMapper;
    
    @Override
    public Page<ArchiveSearchResult> advancedSearch(AdvancedSearchRequest request) {
        // 1. 构建查询条件（排除摘要搜索，因为摘要字段是加密的）
        boolean hasSummarySearch = StringUtils.hasText(request.getSummary());
        LambdaQueryWrapper<Archive> wrapper = buildQueryWrapper(request, false);
        
        // 2. 分页查询
        // 如果有摘要搜索，需要扩大查询范围，然后在内存中过滤
        int pageSize = request.getPageSize();
        int page = request.getPage();
        
        Page<Archive> archivePage;
        if (hasSummarySearch) {
            // 摘要搜索：扩大查询范围，在内存中过滤
            // 查询更多记录以确保有足够的结果（最多查询 10 倍的数据）
            Page<Archive> expandedPage = new Page<>(1, pageSize * 10);
            archivePage = archiveMapper.selectPage(expandedPage, wrapper);
            
            // 在内存中解密并过滤摘要
            String summaryKeyword = request.getSummary().toLowerCase();
            List<Archive> filteredArchives = archivePage.getRecords().stream()
                .filter(archive -> {
                    try {
                        // 摘要字段已经通过 EncryptTypeHandler 自动解密
                        String summary = archive.getSummary();
                        if (summary == null) {
                            return false;
                        }
                        return summary.toLowerCase().contains(summaryKeyword);
                    } catch (Exception e) {
                        log.warn("解密摘要字段失败: archiveId={}", archive.getId(), e);
                        return false;
                    }
                })
                .collect(Collectors.toList());
            
            // 手动分页
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, filteredArchives.size());
            List<Archive> pagedArchives = start < filteredArchives.size() 
                ? filteredArchives.subList(start, end) 
                : List.of();
            
            // 创建新的分页结果
            archivePage = new Page<>(page, pageSize);
            archivePage.setRecords(pagedArchives);
            archivePage.setTotal(filteredArchives.size());
        } else {
            // 普通查询：直接分页
            archivePage = archiveMapper.selectPage(new Page<>(page, pageSize), wrapper);
        }
        
        // 3. 转换为结果DTO
        Page<ArchiveSearchResult> resultPage = new Page<>(archivePage.getCurrent(), archivePage.getSize(), archivePage.getTotal());
        List<ArchiveSearchResult> results = archivePage.getRecords().stream()
            .map(this::toSearchResult)
            .collect(Collectors.toList());
        resultPage.setRecords(results);
        
        log.info("高级检索完成: 条件数={}, 结果数={}, 摘要搜索={}", 
            countConditions(request), results.size(), hasSummarySearch);
        
        return resultPage;
    }
    
    @Override
    public Page<ArchiveSearchResult> searchByAmountRange(String fondsNo, 
                                                         BigDecimal minAmount, 
                                                         BigDecimal maxAmount,
                                                         Integer page, 
                                                         Integer pageSize) {
        AdvancedSearchRequest request = new AdvancedSearchRequest();
        request.setFondsNo(fondsNo);
        request.setMinAmount(minAmount);
        request.setMaxAmount(maxAmount);
        request.setPage(page);
        request.setPageSize(pageSize);
        
        return advancedSearch(request);
    }
    
    @Override
    public Page<ArchiveSearchResult> searchBySummary(String fondsNo, 
                                                    String summary,
                                                    Integer page, 
                                                    Integer pageSize) {
        AdvancedSearchRequest request = new AdvancedSearchRequest();
        request.setFondsNo(fondsNo);
        request.setSummary(summary);
        request.setPage(page);
        request.setPageSize(pageSize);
        
        return advancedSearch(request);
    }
    
    /**
     * 构建查询条件
     * 
     * @param request 检索请求
     * @param includeSummary 是否包含摘要搜索条件（摘要字段加密，不能直接在SQL中搜索）
     */
    private LambdaQueryWrapper<Archive> buildQueryWrapper(AdvancedSearchRequest request, boolean includeSummary) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        
        // 全宗号过滤（必须）
        if (StringUtils.hasText(request.getFondsNo())) {
            wrapper.eq(Archive::getFondsNo, request.getFondsNo());
        }
        
        // 档案类型
        if (StringUtils.hasText(request.getDocType())) {
            wrapper.eq(Archive::getCategoryCode, request.getDocType());
        }
        
        // 关键字搜索（标题）
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Archive::getTitle, request.getKeyword());
        }
        
        // 摘要搜索（注意：摘要字段是加密的，不能直接在SQL中搜索）
        // 如果 includeSummary=false，则在内存中过滤
        // 如果 includeSummary=true，则不添加此条件（已废弃，保留兼容性）
        if (includeSummary && StringUtils.hasText(request.getSummary())) {
            // 注意：由于摘要字段是加密存储，不能直接在数据库层面进行 LIKE 查询
            // 这里不添加条件，将在内存中过滤
            log.warn("摘要搜索条件被忽略（字段加密），将在内存中过滤");
        }
        
        // 金额范围（Archive 实体有 amount 字段）
        if (request.getMinAmount() != null) {
            wrapper.ge(Archive::getAmount, request.getMinAmount());
        }
        if (request.getMaxAmount() != null) {
            wrapper.le(Archive::getAmount, request.getMaxAmount());
        }
        
        // 日期范围
        // if (request.getStartDate() != null) {
        //     wrapper.ge(Archive::getDocDate, request.getStartDate());
        // }
        // if (request.getEndDate() != null) {
        //     wrapper.le(Archive::getDocDate, request.getEndDate());
        // }
        
        // 归档年度
        if (request.getArchiveYear() != null) {
            wrapper.eq(Archive::getFiscalYear, String.valueOf(request.getArchiveYear()));
        }
        
        // 只查询已归档的档案
        wrapper.eq(Archive::getStatus, "archived");
        
        // 排序：按创建时间倒序
        wrapper.orderByDesc(Archive::getCreatedTime);
        
        return wrapper;
    }
    
    /**
     * 转换为检索结果DTO
     */
    private ArchiveSearchResult toSearchResult(Archive archive) {
        ArchiveSearchResult result = new ArchiveSearchResult();
        result.setId(archive.getId());
        result.setArchiveCode(archive.getArchiveCode());
        result.setTitle(archive.getTitle());
        // 摘要字段已经通过 EncryptTypeHandler 自动解密
        result.setSummary(archive.getSummary());
        result.setAmount(archive.getAmount());
        result.setArchiveYear(archive.getFiscalYear() != null ? Integer.parseInt(archive.getFiscalYear()) : null);
        result.setDocType(archive.getCategoryCode());
        result.setFondsNo(archive.getFondsNo());
        result.setIsMasked(false); // 根据用户权限决定是否脱敏
        
        // TODO: 从 metadata_ext 或 standardMetadata 中提取日期、对方、凭证号、发票号等信息
        // 这里需要根据实际的数据结构进行解析
        
        return result;
    }
    
    /**
     * 统计查询条件数量（用于日志）
     */
    private int countConditions(AdvancedSearchRequest request) {
        int count = 0;
        if (request.getFondsNo() != null) count++;
        if (request.getDocType() != null) count++;
        if (request.getKeyword() != null) count++;
        if (request.getSummary() != null) count++;
        if (request.getMinAmount() != null) count++;
        if (request.getMaxAmount() != null) count++;
        if (request.getStartDate() != null) count++;
        if (request.getEndDate() != null) count++;
        if (request.getArchiveYear() != null) count++;
        return count;
    }
}

