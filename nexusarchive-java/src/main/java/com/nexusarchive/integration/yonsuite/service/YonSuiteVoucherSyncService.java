// Input: Jackson、Lombok、Spring Framework、Java 标准库、等
// Output: YonSuiteVoucherSyncService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.*;
import com.nexusarchive.integration.yonsuite.mapper.YonVoucherMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.PreArchiveCheckService;
import com.nexusarchive.service.VoucherPdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * YonSuite 凭证同步服务
 * 
 * 改造后流程：
 * 1. 同步凭证数据 -> 写入 arc_file_content (预归档表)
 * 2. 生成 PDF 版式文件
 * 3. 状态设为 PENDING_CHECK
 * 4. 异步触发四性检测
 * 5. 用户在电子凭证池查看并提交归档申请
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Deprecated
public class YonSuiteVoucherSyncService {

    private static final String SOURCE_SYSTEM = "YonSuite";

    private final YonSuiteClient yonSuiteClient;
    private final YonVoucherMapper yonVoucherMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final PreArchiveCheckService preArchiveCheckService;
    private final VoucherPdfGeneratorService pdfGeneratorService;
    private final com.nexusarchive.service.FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    // 保留原有 ArchiveService 用于兼容旧逻辑（如需要）
    private final ArchiveService archiveService;

    /**
     * 按期间同步凭证列表 (写入预归档库)
     */
    public List<String> syncVouchersByPeriod(String accessToken, String accbookCode,
            String periodStart, String periodEnd) {
        log.info("开始同步凭证到预归档库: accbookCode={}, period={}-{}", accbookCode, periodStart, periodEnd);

        List<String> syncedIds = new ArrayList<>();
        List<VoucherSyncResult> syncResults = new ArrayList<>();
        int pageIndex = 1;
        int pageSize = 100;
        boolean hasMore = true;

        while (hasMore) {
            // 构建请求
            YonVoucherListRequest request = new YonVoucherListRequest();
            YonVoucherListRequest.Pager pager = new YonVoucherListRequest.Pager();
            pager.setPageIndex(pageIndex);
            pager.setPageSize(pageSize);
            request.setPager(pager);
            request.setAccbookCode(accbookCode);
            request.setPeriodStart(periodStart);
            request.setPeriodEnd(periodEnd);

            // 调用 API
            YonVoucherListResponse response = yonSuiteClient.queryVouchers(accessToken, request);

            if (response.getData() == null || response.getData().getRecordList() == null) {
                break;
            }

            List<YonVoucherListResponse.VoucherRecord> records = response.getData().getRecordList();

            for (YonVoucherListResponse.VoucherRecord record : records) {
                try {
                    VoucherSyncResult result = processVoucherRecordToPreArchive(record);
                    if (result != null) {
                        syncedIds.add(result.fileId);
                        syncResults.add(result);
                    }
                } catch (Exception e) {
                    log.error("Failed to process voucher: {}", record.getHeader().getId(), e);
                }
            }

            // 检查是否还有更多
            int totalPages = (int) Math.ceil((double) response.getData().getRecordCount() / pageSize);
            hasMore = pageIndex < totalPages;
            pageIndex++;
        }

        log.info("凭证同步到预归档库完成: 共同步 {} 条", syncedIds.size());

        // 异步生成 PDF 和触发四性检测
        if (!syncResults.isEmpty()) {
            generatePdfsAndCheck(syncResults);
        }

        return syncedIds;
    }

    /**
     * 按凭证ID同步单个凭证 (写入预归档库)
     */
    @Transactional
    public String syncVoucherById(String accessToken, String voucherId) {
        log.info("同步单个凭证到预归档库: voucherId={}", voucherId);

        YonVoucherDetailResponse response = yonSuiteClient.queryVoucherById(accessToken, voucherId);

        if (response.getData() == null) {
            log.warn("凭证不存在: {}", voucherId);
            return null;
        }

        // 映射为预归档文件记录
        ArcFileContent fileContent = yonVoucherMapper.toPreArchiveFile(response.getData(), SOURCE_SYSTEM);

        if (fileContent == null) {
            return null;
        }

        // 幂等性检查：根据业务单据号查找
        String businessDocNo = fileContent.getBusinessDocNo();
        ArcFileContent existing = arcFileContentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                        .eq(ArcFileContent::getBusinessDocNo, businessDocNo));

        String fileId;
        if (existing != null) {
            // 已存在，检查状态
            if ("ARCHIVED".equals(existing.getPreArchiveStatus())) {
                log.info("凭证已归档，跳过更新: {}", voucherId);
                return existing.getId();
            }
            // 更新现有记录
            fileContent.setId(existing.getId());
            arcFileContentMapper.updateById(fileContent);
            log.info("更新预归档凭证: voucherId={}, fileId={}", voucherId, existing.getId());
            fileId = existing.getId();
        } else {
            // 创建新记录
            arcFileContentMapper.insert(fileContent);
            log.info("创建预归档凭证: voucherId={}, fileId={}", voucherId, fileContent.getId());
            fileId = fileContent.getId();
        }

        // 生成 PDF 文件
        try {
            String voucherJson = objectMapper.writeValueAsString(response.getData());
            pdfGeneratorService.generatePdfForPreArchive(fileId, voucherJson);
        } catch (Exception e) {
            log.error("生成 PDF 失败: voucherId={}", voucherId, e);
        }

        // 异步触发四性检测
        triggerPreArchiveCheck(List.of(fileId));

        return fileId;
    }

    /**
     * 处理单条凭证记录 (写入预归档库)
     * 
     * @return VoucherSyncResult 包含 fileId 和凭证 JSON 用于后续 PDF 生成
     */
    private VoucherSyncResult processVoucherRecordToPreArchive(YonVoucherListResponse.VoucherRecord record) {
        ArcFileContent fileContent = yonVoucherMapper.toPreArchiveFile(record, SOURCE_SYSTEM);

        if (fileContent == null) {
            return null;
        }

        // 幂等性检查
        String businessDocNo = fileContent.getBusinessDocNo();
        ArcFileContent existing = arcFileContentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                        .eq(ArcFileContent::getBusinessDocNo, businessDocNo));

        String fileId;
        if (existing != null) {
            if ("ARCHIVED".equals(existing.getPreArchiveStatus())) {
                return null; // 已归档，跳过
            }
            fileContent.setId(existing.getId());
            arcFileContentMapper.updateById(fileContent);
            fileId = existing.getId();
        } else {
            arcFileContentMapper.insert(fileContent);
            fileId = fileContent.getId();
        }

        // 序列化凭证 JSON 用于 PDF 生成
        String voucherJson = "";
        try {
            voucherJson = objectMapper.writeValueAsString(record);
        } catch (Exception e) {
            log.warn("Failed to serialize voucher record: {}", record.getHeader().getId());
        }

        // 同步附件
        List<String> allFileIds = new ArrayList<>();
        allFileIds.add(fileId);

        try {
            var attResponse = yonSuiteClient.queryVoucherAttachments(null, record.getHeader().getId());
            if (attResponse != null && attResponse.getData() != null) {
                int index = 1;
                for (var att : attResponse.getData()) {
                    String attBusinessDocNo = businessDocNo + "_ATT_" + index++;

                    // 检查附件是否已存在
                    ArcFileContent existingAtt = arcFileContentMapper.selectOne(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                                    .eq(ArcFileContent::getBusinessDocNo, attBusinessDocNo));

                    if (existingAtt != null && "ARCHIVED".equals(existingAtt.getPreArchiveStatus())) {
                        continue;
                    }

                    ArcFileContent attContent = existingAtt != null ? existingAtt : new ArcFileContent();
                    if (existingAtt == null) {
                        // 复制主凭证的基础信息
                        attContent.setFondsCode(fileContent.getFondsCode());
                        attContent.setFiscalYear(fileContent.getFiscalYear());
                        attContent.setSourceSystem(fileContent.getSourceSystem());
                        attContent.setErpVoucherNo(fileContent.getErpVoucherNo());
                        attContent.setBusinessDocNo(attBusinessDocNo);
                        attContent.setCreatedTime(java.time.LocalDateTime.now());
                    }

                    attContent.setFileName(att.getFileName());
                    attContent.setFileType(att.getFileExtension());
                    attContent.setFileSize(att.getFileSize());
                    attContent.setVoucherType("ATTACHMENT"); // 标记为附件
                    attContent.setPreArchiveStatus("PENDING_CHECK");

                    // 下载并保存文件
                    if (att.getUrl() != null) {
                        try (java.io.InputStream is = yonSuiteClient.downloadStream(att.getUrl())) {
                            if (is != null) {
                                String safeFileName = attBusinessDocNo + "_" + att.getFileName();
                                String path = fileStorageService.saveFile(is, "attachments/" + safeFileName);
                                attContent.setStoragePath(path);
                            }
                        }
                    }

                    if (existingAtt != null) {
                        arcFileContentMapper.updateById(attContent);
                    } else {
                        arcFileContentMapper.insert(attContent);
                    }

                    allFileIds.add(attContent.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to sync attachments for voucher: {}", record.getHeader().getId(), e);
        }

        return new VoucherSyncResult(fileId, voucherJson, allFileIds);
    }

    /**
     * 异步生成 PDF 并触发四性检测
     */
    @Async
    public void generatePdfsAndCheck(List<VoucherSyncResult> results) {
        log.info("异步生成 PDF 并检测: {} 条记录", results.size());

        List<String> fileIdsToCheck = new ArrayList<>();
        for (VoucherSyncResult result : results) {
            try {
                // 主凭证生成 PDF
                pdfGeneratorService.generatePdfForPreArchive(result.fileId, result.voucherJson);
                // 将所有相关文件（主凭证+附件）加入检测队列
                fileIdsToCheck.addAll(result.allFileIds);
            } catch (Exception e) {
                log.error("PDF 生成失败: fileId={}", result.fileId, e);
            }
        }

        // 触发四性检测
        if (!fileIdsToCheck.isEmpty()) {
            triggerPreArchiveCheck(fileIdsToCheck);
        }
    }

    /**
     * 异步触发四性检测
     */
    @Async
    public void triggerPreArchiveCheck(List<String> fileIds) {
        log.info("触发四性检测: fileIds={}", fileIds);
        try {
            preArchiveCheckService.checkMultipleFiles(fileIds);
        } catch (Exception e) {
            log.error("四性检测失败: {}", fileIds, e);
        }
    }

    /**
     * 凭证同步结果内部类
     */
    private static class VoucherSyncResult {
        final String fileId;
        final String voucherJson;
        final List<String> allFileIds;

        VoucherSyncResult(String fileId, String voucherJson, List<String> allFileIds) {
            this.fileId = fileId;
            this.voucherJson = voucherJson;
            this.allFileIds = allFileIds;
        }
    }
}
