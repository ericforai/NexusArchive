// Input: Mybatis-Plus, PoolService, ArcFileMetadataIndexMapper
// Output: PoolServiceImpl 类
// Pos: 业务逻辑实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.dto.PoolItemDto;
import com.nexusarchive.dto.search.CandidateSearchRequest;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.service.PoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 电子凭证池服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoolServiceImpl implements PoolService {

    private final ArcFileContentMapper arcFileContentMapper;
    private final ArcFileMetadataIndexMapper arcFileMetadataIndexMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] SOURCE_SYSTEMS = {
            "Web上传", "用友", "金蝶", "泛微OA", "易快报", "汇联易", "SAP"
    };
    private static final Map<String, String> LEGACY_STATUS_MAP = Map.ofEntries(
            Map.entry("DRAFT", PreArchiveStatus.PENDING_CHECK.getCode()),
            Map.entry("PENDING_CHECK", PreArchiveStatus.PENDING_CHECK.getCode()),
            Map.entry("CHECK_FAILED", PreArchiveStatus.NEEDS_ACTION.getCode()),
            Map.entry("PENDING_METADATA", PreArchiveStatus.NEEDS_ACTION.getCode()),
            Map.entry("MATCH_PENDING", PreArchiveStatus.READY_TO_MATCH.getCode()),
            Map.entry("MATCHED", PreArchiveStatus.READY_TO_MATCH.getCode()),
            Map.entry("PENDING_ARCHIVE", PreArchiveStatus.READY_TO_ARCHIVE.getCode()),
            Map.entry("PENDING_APPROVAL", PreArchiveStatus.COMPLETED.getCode()),
            Map.entry("ARCHIVING", PreArchiveStatus.COMPLETED.getCode()),
            Map.entry("ARCHIVED", PreArchiveStatus.COMPLETED.getCode())
    );

    // 门类映射表: 前端/API代码 -> 数据库 voucher_type 值列表
    private static final Map<String, List<String>> CATEGORY_TYPE_MAP;
    static {
        Map<String, List<String>> map = new HashMap<>();
        // 记账凭证 (ERP同步)
        map.put("VOUCHER", List.of("VOUCHER"));
        // 原始凭证 (通常在单据池)
        map.put("AC01", List.of("AC01", "ATTACHMENT"));
        // 会计账簿
        map.put("AC02", List.of("AC02"));
        // 财务报告
        map.put("AC03", List.of("AC03", "REPORT"));
        // 其他资料 (包含未分类的 NULL 值) -> 注意: NULL 需要特殊处理
        map.put("AC04", List.of("AC04", "OTHER"));
        // 兼容前端传 OTHER 的情况
        map.put("OTHER", List.of("AC04", "OTHER"));
        CATEGORY_TYPE_MAP = Map.copyOf(map);
    }


    @Override
    public List<PoolItemDto> searchCandidates(CandidateSearchRequest request) {
        log.info("开始搜索候选凭证: {}", request);

        // 1. 构建元数据查询 (针对金额、发票号、销售方、日期等)
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileMetadataIndex> metaQuery = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        if (request.getMinAmount() != null) {
            metaQuery.ge(ArcFileMetadataIndex::getTotalAmount, request.getMinAmount());
        }
        if (request.getMaxAmount() != null) {
            metaQuery.le(ArcFileMetadataIndex::getTotalAmount, request.getMaxAmount());
        }
        if (request.getStartDate() != null) {
            metaQuery.ge(ArcFileMetadataIndex::getIssueDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            metaQuery.le(ArcFileMetadataIndex::getIssueDate, request.getEndDate());
        }
        if (request.getInvoiceNumber() != null && !request.getInvoiceNumber().isEmpty()) {
            metaQuery.eq(ArcFileMetadataIndex::getInvoiceNumber, request.getInvoiceNumber());
        }
        if (request.getInvoiceCode() != null && !request.getInvoiceCode().isEmpty()) {
            metaQuery.eq(ArcFileMetadataIndex::getInvoiceCode, request.getInvoiceCode());
        }

        // 关键字模糊匹配 (发票号或销售方)
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            metaQuery.and(w -> w.like(ArcFileMetadataIndex::getInvoiceNumber, request.getKeyword())
                    .or().like(ArcFileMetadataIndex::getSellerName, request.getKeyword()));
        }

        List<ArcFileMetadataIndex> metaResults = arcFileMetadataIndexMapper.selectList(metaQuery);
        Map<String, ArcFileMetadataIndex> metaMap = metaResults.stream()
                .filter(m -> m.getFileId() != null)
                .collect(Collectors.toMap(ArcFileMetadataIndex::getFileId, m -> m, (m1, m2) -> m1));

        // 2. 构建主表查询
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent> contentQuery = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        // 排除已归档
        contentQuery.ne(ArcFileContent::getPreArchiveStatus, "COMPLETED");
        
        // 如果有元数据过滤，则限制 ID 范围
        if (!metaMap.isEmpty()) {
            Set<String> fileIds = metaMap.keySet();
            if (fileIds.isEmpty()) {
                return new ArrayList<>(); // 元数据过滤无结果，直接返回空
            }
            contentQuery.in(ArcFileContent::getId, fileIds);
        }

        // 关键字模糊匹配主表字段 (文件名)
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            contentQuery.like(ArcFileContent::getFileName, request.getKeyword());
        }

        // 限制结果数量
        contentQuery.last("LIMIT 50");
        contentQuery.orderByDesc(ArcFileContent::getCreatedTime);

        List<ArcFileContent> contents = arcFileContentMapper.selectList(contentQuery);

        // 3. 转换并补充数据
        return contents.stream().map(c -> {
            ArcFileMetadataIndex meta = metaMap.get(c.getId());
            if (meta == null) {
                // 如果是没走元数据索引的简单查询，补查一下 (使用 selectList 并取第一条以防重复数据导致 selectOne 报错)
                List<ArcFileMetadataIndex> metas = arcFileMetadataIndexMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileMetadataIndex>()
                                .eq(ArcFileMetadataIndex::getFileId, c.getId()).last("LIMIT 1"));
                meta = metas.isEmpty() ? null : metas.get(0);
            }
            return convertToPoolItemDto(c, meta);
        }).collect(Collectors.toList());
    }

    private PoolItemDto convertToPoolItemDto(ArcFileContent fileContent, ArcFileMetadataIndex metadata) {
        String displayCode = fileContent.getArchivalCode() != null
                ? fileContent.getArchivalCode().replace("TEMP-", "")
                : "PENDING";

        String amountStr = (metadata != null && metadata.getTotalAmount() != null)
                ? metadata.getTotalAmount().toString() : "-";

        String source = fileContent.getSourceSystem();
        if (source == null || source.isEmpty()) {
            source = "Web上传";
        }

        return PoolItemDto.builder()
                .id(fileContent.getId())
                .businessDocNo(fileContent.getBusinessDocNo())
                .erpVoucherNo(fileContent.getErpVoucherNo())
                .code(displayCode)
                .source(source)
                .type(fileContent.getFileType())
                .voucherType(fileContent.getVoucherType())
                .amount(amountStr)
                .date(fileContent.getCreatedTime() != null ? fileContent.getCreatedTime().format(FORMATTER) : "-")
                .status(fileContent.getPreArchiveStatus() != null ? fileContent.getPreArchiveStatus() : "PENDING_CHECK")
                .sourceSystem(fileContent.getSourceSystem())
                .fileName(fileContent.getFileName())
                .summary(fileContent.getSummary())
                .voucherWord(fileContent.getVoucherWord())
                .docDate(fileContent.getDocDate() != null ? fileContent.getDocDate().toString() : "-")
                .docDate(fileContent.getDocDate() != null ? fileContent.getDocDate().toString() : "-")
                .sourceData(fileContent.getSourceData())
                .fiscalYear(fileContent.getFiscalYear())
                .period(fileContent.getPeriod())
                .fondsCode(fileContent.getFondsCode())
                .build();
    }

    @Override
    public ArcFileContent getFileById(String id) {
        return arcFileContentMapper.selectById(id);
    }

    @Override
    public List<PoolItemDto> listPoolItems(String category) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        queryWrapper.and(w -> w.likeRight(ArcFileContent::getArchivalCode, "TEMP-POOL-")
                .or()
                .isNotNull(ArcFileContent::getPreArchiveStatus));

        // Use standard filter logic
        applyCategoryFilter(queryWrapper, category);
        
        queryWrapper.orderByDesc(ArcFileContent::getCreatedTime);

        List<ArcFileContent> fileContents = arcFileContentMapper.selectList(queryWrapper);
        return fileContents.stream()
                .map(this::convertToPoolItemDto)
                .collect(Collectors.toList());
    }



    @Override
    public List<PoolItemDto> listByStatus(String status, String category) {
        String normalizedStatus = normalizeLegacyStatus(status);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        queryWrapper.eq(ArcFileContent::getPreArchiveStatus, normalizedStatus);

        applyCategoryFilter(queryWrapper, category);

        queryWrapper.orderByDesc(ArcFileContent::getCreatedTime);

        List<ArcFileContent> fileContents = arcFileContentMapper.selectList(queryWrapper);
        return fileContents.stream()
                .map(this::convertToPoolItemDto)
                .collect(Collectors.toList());
    }


    private String normalizeLegacyStatus(String status) {
        if (status == null || status.isBlank()) {
            return status;
        }
        String trimmed = status.trim();
        String normalized = LEGACY_STATUS_MAP.get(trimmed);
        return normalized != null ? normalized : trimmed;
    }



    @Override
    public Map<String, Long> getStatusStats(String category) {
        Map<String, Long> stats = new HashMap<>();
        String[] statuses = { "PENDING_CHECK", "NEEDS_ACTION", "READY_TO_MATCH", "READY_TO_ARCHIVE", "COMPLETED" };

        for (String status : statuses) {
            LambdaQueryWrapper<ArcFileContent> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ArcFileContent::getPreArchiveStatus, status);
            applyCategoryFilter(queryWrapper, category);
            Long count = arcFileContentMapper.selectCount(queryWrapper);
            stats.put(status, count);
        }

        // 统计无状态的记录（旧数据）
        LambdaQueryWrapper<ArcFileContent> nullStatusQuery = new LambdaQueryWrapper<>();
        nullStatusQuery.likeRight(ArcFileContent::getArchivalCode, "TEMP-POOL-")
                .isNull(ArcFileContent::getPreArchiveStatus);
        applyCategoryFilter(nullStatusQuery, category);
        Long nullCount = arcFileContentMapper.selectCount(nullStatusQuery);
        stats.put("NO_STATUS", nullCount);

        return stats;
    }

    /**
     * 应用门类过滤条件
     * 策略：
     * 1. 如果有 category，查找对应的 voucher_type 列表
     * 2. 如果 category 是 VOUCHER/AC04/OTHER，需要包含 NULL 值（VOUCHER 包含未设置的旧数据）
     * 3. 如果无 category，默认逻辑：排除 ATTACHMENT (保持向后兼容，或者根据新需求调整)
     *    -> 根据新逻辑，Pool 页面只显示 VOUCHER，所以如果不传 category，可能需要默认显示 VOUCHER？
     *    -> 现在的逻辑是：不传则显示"非附件"的所有内容 (旧逻辑)。为了兼容性，保持"排除附件"。
     */
    private void applyCategoryFilter(LambdaQueryWrapper<ArcFileContent> queryWrapper, String category) {
        if (category != null && !category.isBlank() && !"null".equals(category)) {
            List<String> types = new ArrayList<>(CATEGORY_TYPE_MAP.getOrDefault(category, List.of(category)));
            // VOUCHER、AC04、OTHER、AC03 门类包含 null 值（兼容未设置门类的旧数据）
            boolean includeNull = "VOUCHER".equals(category) || "AC04".equals(category) || "OTHER".equals(category) || "AC03".equals(category);

            queryWrapper.and(w -> {
                if (includeNull) {
                    w.isNull(ArcFileContent::getVoucherType);
                    if (!types.isEmpty()) {
                        w.or().in(ArcFileContent::getVoucherType, types);
                    }
                } else {
                    if (!types.isEmpty()) {
                        w.in(ArcFileContent::getVoucherType, types);
                    } else {
                        // 如果映射列表为空（未知的 category），可能应该查不到数据
                        // 这里为了安全，查一个不存在的值
                        w.eq(ArcFileContent::getVoucherType, "UNKNOWN_CATEGORY_" + category);
                    }
                }
            });
        } else {
            // 无门类参数时 (旧逻辑兼容): 排除 ATTACHMENT
            // 注意: 随着页面细分，应该总是传递 category。这个 fallback 主要是为了防止未更新的前端调用出错。
            queryWrapper.and(w -> w.isNull(ArcFileContent::getVoucherType)
                    .or().ne(ArcFileContent::getVoucherType, "ATTACHMENT"));
        }
    }

    @Override
    @Transactional
    public void updateStatus(String id, String status) {
        ArcFileContent fileContent = arcFileContentMapper.selectById(id);
        if (fileContent == null) {
            throw new RuntimeException("文件不存在: " + id);
        }

        fileContent.setPreArchiveStatus(status);

        // 记录状态变更时间
        if ("COMPLETED".equals(status)) {
            fileContent.setArchivedTime(LocalDateTime.now());
        }

        arcFileContentMapper.updateById(fileContent);
    }

    @Override
    public List<ArcFileContent> listPendingCheckFiles() {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        queryWrapper.likeRight(ArcFileContent::getArchivalCode, "TEMP-POOL-")
                .and(w -> w.isNull(ArcFileContent::getPreArchiveStatus)
                        .or().eq(ArcFileContent::getPreArchiveStatus, "PENDING_CHECK")
                        .or().eq(ArcFileContent::getPreArchiveStatus, "draft")
                        .or().eq(ArcFileContent::getPreArchiveStatus, "DRAFT"));

        return arcFileContentMapper.selectList(queryWrapper);
    }

    @Override
    public List<ArcFileContent> getLegacyAttachments(String businessDocNo) {
        QueryWrapper<ArcFileContent> query = new QueryWrapper<>();
        query.likeRight("business_doc_no", businessDocNo + "_ATT_")
                .orderByAsc("business_doc_no");
        return arcFileContentMapper.selectList(query);
    }

    @Override
    public ArcFileMetadataIndex getMetadataByFileId(String fileId) {
        List<ArcFileMetadataIndex> metas = arcFileMetadataIndexMapper.selectList(
                new QueryWrapper<ArcFileMetadataIndex>().eq("file_id", fileId).last("LIMIT 1"));
        return metas.isEmpty() ? null : metas.get(0);
    }

    @Override
    public PoolItemDto convertToPoolItemDto(ArcFileContent fileContent) {
        ArcFileMetadataIndex metadata = getMetadataByFileId(fileContent.getId());
        return convertToPoolItemDto(fileContent, metadata);
    }

    @Override
    @Transactional
    public int cleanupDemoData() {
        QueryWrapper<ArcFileContent> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("file_hash", "DEMO_HASH_");
        List<ArcFileContent> oldFiles = arcFileContentMapper.selectList(queryWrapper);

        if (!oldFiles.isEmpty()) {
            List<String> oldFileIds = oldFiles.stream().map(ArcFileContent::getId).collect(Collectors.toList());
            arcFileMetadataIndexMapper.delete(new QueryWrapper<ArcFileMetadataIndex>().in("file_id", oldFileIds));
        }

        int deletedCount = arcFileContentMapper.delete(queryWrapper);
        return deletedCount;
    }

    @Override
    @Transactional
    public void insertDemoFile(ArcFileContent content) {
        arcFileContentMapper.insert(content);
    }

    @Override
    @Transactional
    public void insertDemoMetadata(ArcFileMetadataIndex metadata) {
        arcFileMetadataIndexMapper.insert(metadata);
    }
}
