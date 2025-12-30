// Input: Mybatis-Plus, PoolService, ArcFileMetadataIndexMapper
// Output: PoolServiceImpl 类
// Pos: 业务逻辑实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.dto.PoolItemDto;
import com.nexusarchive.dto.search.CandidateSearchRequest;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.service.PoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        contentQuery.ne(ArcFileContent::getPreArchiveStatus, "ARCHIVED");
        
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
                .amount(amountStr)
                .date(fileContent.getCreatedTime() != null ? fileContent.getCreatedTime().format(FORMATTER) : "-")
                .status(fileContent.getPreArchiveStatus() != null ? fileContent.getPreArchiveStatus() : "PENDING_CHECK")
                .sourceSystem(fileContent.getSourceSystem())
                .fileName(fileContent.getFileName())
                .summary(fileContent.getSummary())
                .voucherWord(fileContent.getVoucherWord())
                .docDate(fileContent.getDocDate() != null ? fileContent.getDocDate().toString() : "-")
                .build();
    }
}
