// Input: Mybatis-Plus, PoolService, ArcFileMetadataIndexMapper
// Output: PoolServiceImpl 类
// Pos: 业务逻辑实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.annotation.ReadOnly;
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
 *
 * ARCHITECTURE-NOTE: 预归档模块边界
 *
 * 职责范围：
 * - 管理 ArcFileContent 表（预归档文件）
 * - 管理元数据索引（ArcFileMetadataIndex）
 * - 状态机：PENDING_CHECK → NEEDS_ACTION → READY_TO_MATCH → READY_TO_ARCHIVE → SUBMITTED → COMPLETED
 *
 * 边界说明：
 * - 本服务不直接操作 Archive 表
 * - 归档申请通过 PreArchiveSubmitService 提交到 Archive
 * - 档号生成由 ArchivalCodeGenerator 策略类负责
 *
 * 相关文档：docs/architecture/module-dependency-status.md
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
        map.put(com.nexusarchive.common.constants.ArchiveConstants.Categories.VOUCHER, List.of(com.nexusarchive.common.constants.ArchiveConstants.Categories.VOUCHER, "ATTACHMENT"));
        // 会计账簿
        map.put(com.nexusarchive.common.constants.ArchiveConstants.Categories.BOOK, List.of(com.nexusarchive.common.constants.ArchiveConstants.Categories.BOOK));
        // 财务报告
        map.put(com.nexusarchive.common.constants.ArchiveConstants.Categories.REPORT, List.of(com.nexusarchive.common.constants.ArchiveConstants.Categories.REPORT, "REPORT"));
        // 其他资料 (包含未分类的 NULL 值) -> 注意: NULL 需要特殊处理
        map.put(com.nexusarchive.common.constants.ArchiveConstants.Categories.OTHERS, List.of(com.nexusarchive.common.constants.ArchiveConstants.Categories.OTHERS, "OTHER"));
        // 兼容前端传 OTHER 的情况
        map.put("OTHER", List.of(com.nexusarchive.common.constants.ArchiveConstants.Categories.OTHERS, "OTHER"));
        CATEGORY_TYPE_MAP = Map.copyOf(map);
    }


    @Override
    public List<PoolItemDto> searchCandidates(CandidateSearchRequest request) {
        log.info("开始搜索候选凭证: {}", request);

        // 1. 构建元数据查询 (针对金额、发票号、销售方、日期等)
        LambdaQueryWrapper<ArcFileMetadataIndex> metaQuery = new LambdaQueryWrapper<>();
        
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
        LambdaQueryWrapper<ArcFileContent> contentQuery = new LambdaQueryWrapper<>();
        
        // 排除已归档
        contentQuery.ne(ArcFileContent::getPreArchiveStatus, com.nexusarchive.common.constants.StatusConstants.PreArchive.COMPLETED);
        
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

        // 3. 批量补充缺失的元数据（避免 N+1 查询）
        // 收集所有在 metaMap 中不存在的 fileIds
        List<String> missingFileIds = contents.stream()
                .filter(c -> !metaMap.containsKey(c.getId()))
                .map(ArcFileContent::getId)
                .collect(Collectors.toList());

        if (!missingFileIds.isEmpty()) {
            // 批量查询所有缺失的元数据（单次 IN 查询）
            LambdaQueryWrapper<ArcFileMetadataIndex> batchQuery = new LambdaQueryWrapper<>();
            batchQuery.in(ArcFileMetadataIndex::getFileId, missingFileIds);
            batchQuery.last("LIMIT 1000");  // 安全限制

            List<ArcFileMetadataIndex> batchMetas = arcFileMetadataIndexMapper.selectList(batchQuery);
            // 合并到 metaMap 中
            batchMetas.forEach(m -> {
                if (m.getFileId() != null) {
                    metaMap.put(m.getFileId(), m);
                }
            });
        }

        // 4. 转换为 DTO
        return contents.stream()
                .map(c -> convertToPoolItemDto(c, metaMap.get(c.getId())))
                .collect(Collectors.toList());
    }

    private PoolItemDto convertToPoolItemDto(ArcFileContent fileContent, ArcFileMetadataIndex metadata) {
        String displayCode = fileContent.getArchivalCode() != null
                ? fileContent.getArchivalCode().replace("TEMP-", "")
                : "PENDING";

        String amountStr = (metadata != null && metadata.getTotalAmount() != null)
                ? metadata.getTotalAmount().toString() : "-";

        String source = fileContent.getSourceSystem();
        if (source == null || source.isEmpty()) {
            source = com.nexusarchive.common.constants.ArchiveConstants.SourceChannel.WEB_UPLOAD;
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
                .status(fileContent.getPreArchiveStatus() != null ? fileContent.getPreArchiveStatus() : com.nexusarchive.common.constants.StatusConstants.PreArchive.PENDING_CHECK)
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
    @ReadOnly
    public ArcFileContent getFileById(String id) {
        return arcFileContentMapper.selectById(id);
    }

    @Override
    @ReadOnly
    public List<PoolItemDto> listPoolItems(String category) {
        LambdaQueryWrapper<ArcFileContent> queryWrapper = new LambdaQueryWrapper<>();
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
    @ReadOnly
    public List<PoolItemDto> listByStatus(String status, String category) {
        String normalizedStatus = normalizeLegacyStatus(status);
        LambdaQueryWrapper<ArcFileContent> queryWrapper = new LambdaQueryWrapper<>();
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
    @ReadOnly
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
            boolean includeNull = "VOUCHER".equals(category) || com.nexusarchive.common.constants.ArchiveConstants.Categories.OTHERS.equals(category) || "OTHER".equals(category) || com.nexusarchive.common.constants.ArchiveConstants.Categories.REPORT.equals(category);

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
        if (com.nexusarchive.common.constants.StatusConstants.PreArchive.COMPLETED.equals(status)) {
            fileContent.setArchivedTime(LocalDateTime.now());
        }

        arcFileContentMapper.updateById(fileContent);
    }

    @Override
    @ReadOnly
    public List<ArcFileContent> listPendingCheckFiles() {
        LambdaQueryWrapper<ArcFileContent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ArcFileContent::getArchivalCode, "TEMP-POOL-")
                .and(w -> w.isNull(ArcFileContent::getPreArchiveStatus)
                        .or().eq(ArcFileContent::getPreArchiveStatus, com.nexusarchive.common.constants.StatusConstants.PreArchive.PENDING_CHECK)
                        .or().eq(ArcFileContent::getPreArchiveStatus, "draft")
                        .or().eq(ArcFileContent::getPreArchiveStatus, "DRAFT"));

        return arcFileContentMapper.selectList(queryWrapper);
    }

    @Override
    @ReadOnly
    public List<ArcFileContent> getLegacyAttachments(String businessDocNo) {
        LambdaQueryWrapper<ArcFileContent> query = new LambdaQueryWrapper<>();
        query.likeRight(ArcFileContent::getBusinessDocNo, businessDocNo + "_ATT_")
                .orderByAsc(ArcFileContent::getBusinessDocNo);
        return arcFileContentMapper.selectList(query);
    }

    @Override
    @ReadOnly
    public ArcFileMetadataIndex getMetadataByFileId(String fileId) {
        List<ArcFileMetadataIndex> metas = arcFileMetadataIndexMapper.selectList(
                new LambdaQueryWrapper<ArcFileMetadataIndex>().eq(ArcFileMetadataIndex::getFileId, fileId).last("LIMIT 1"));
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
        LambdaQueryWrapper<ArcFileContent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ArcFileContent::getFileHash, "DEMO_HASH_");
        List<ArcFileContent> oldFiles = arcFileContentMapper.selectList(queryWrapper);

        if (!oldFiles.isEmpty()) {
            List<String> oldFileIds = oldFiles.stream().map(ArcFileContent::getId).collect(Collectors.toList());
            arcFileMetadataIndexMapper.delete(new LambdaQueryWrapper<ArcFileMetadataIndex>().in(ArcFileMetadataIndex::getFileId, oldFileIds));
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

    @Override
    @Transactional
    public void deletePoolItem(String id) {
        ArcFileContent fileContent = arcFileContentMapper.selectById(id);
        if (fileContent == null) {
            throw new RuntimeException("文件不存在: " + id);
        }

        // 删除元数据索引
        arcFileMetadataIndexMapper.delete(new LambdaQueryWrapper<ArcFileMetadataIndex>()
                .eq(ArcFileMetadataIndex::getFileId, id));

        // 删除文件内容记录
        arcFileContentMapper.deleteById(id);

        log.info("已删除预归档记录: id={}, fileName={}", id, fileContent.getFileName());
    }
}
