// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: PreArchiveCheckService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 预归档库四性检测服务
 * 用于对电子凭证池中的单个文件执行四性检测
 * 
 * @author 合规开发工程师
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreArchiveCheckService {

    private final ArcFileContentMapper arcFileContentMapper;
    private final FourNatureCoreService fourNatureCoreService;

    /**
     * 对单个预归档文件执行四性检测
     * @param fileId 文件ID
     * @return 检测报告
     */
    public FourNatureReport checkSingleFile(String fileId) {
        String checkId = UUID.randomUUID().toString();
        log.info("Starting Pre-Archive Check [ID: {}] for File: {}", checkId, fileId);

        ArcFileContent file = arcFileContentMapper.selectById(fileId);
        if (file == null) {
            return createFailedReport(checkId, "文件不存在: " + fileId);
        }

        FourNatureReport report = FourNatureReport.builder()
                .checkId(checkId)
                .checkTime(LocalDateTime.now())
                .archivalCode(file.getArchivalCode())
                .status(OverallStatus.PASS)
                .build();

        try {
            Path filePath = Paths.get(file.getStoragePath());
            if (!Files.exists(filePath)) {
                return createFailedReport(checkId, "文件不存在于存储路径: " + file.getStoragePath());
            }

            // [FIXED] P1: Use InputStream instead of readAllBytes to prevent OOM
            try (java.io.InputStream fis = java.nio.file.Files.newInputStream(filePath);
                 java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis)) {
                
                bis.mark(Integer.MAX_VALUE);

                // 1. 真实性检测 (Delegated to Core)
                String expectedHash = (file.getOriginalHash() != null && !file.getOriginalHash().isEmpty()) 
                        ? file.getOriginalHash() 
                        : file.getFileHash();
                
                CheckItem authenticity = fourNatureCoreService.checkSingleFileAuthenticity(
                        bis, file.getFileName(), expectedHash, file.getHashAlgorithm(), file.getFileType()
                );
                report.setAuthenticity(authenticity);
                
                if (authenticity.getStatus() == OverallStatus.FAIL) {
                    report.setStatus(OverallStatus.FAIL);
                    updateFileStatus(file, PreArchiveStatus.NEEDS_ACTION.getCode(), report);
                    return report;
                }

                // 2. 完整性检测 - 元数据完整性
                CheckItem integrity = checkMetadataIntegrity(file);
                report.setIntegrity(integrity);
                if (integrity.getStatus() == OverallStatus.FAIL) {
                    report.setStatus(OverallStatus.FAIL);
                }

                // 3. 可用性检测 (Delegated to Core)
                bis.reset();
                CheckItem usability = fourNatureCoreService.checkSingleFileUsability(
                        bis, file.getFileName(), file.getFileType()
                );
                report.setUsability(usability);
                if (usability.getStatus() == OverallStatus.FAIL) {
                    report.setStatus(OverallStatus.FAIL);
                }

                // 4. 安全性检测 (Delegated to Core)
                bis.reset();
                CheckItem safety = fourNatureCoreService.checkSingleFileSafety(
                        bis, file.getFileName()
                );
                report.setSafety(safety);
                if (safety.getStatus() == OverallStatus.FAIL) {
                    report.setStatus(OverallStatus.FAIL);
                    updateFileStatus(file, PreArchiveStatus.NEEDS_ACTION.getCode(), report);
                    return report;
                }

                // Update overall status aggregation (moved inside block)
                if (report.getStatus() == OverallStatus.PASS) {
                    boolean hasWarning = authenticity.getStatus() == OverallStatus.WARNING ||
                            integrity.getStatus() == OverallStatus.WARNING ||
                            usability.getStatus() == OverallStatus.WARNING ||
                            safety.getStatus() == OverallStatus.WARNING;
                    if (hasWarning) {
                        report.setStatus(OverallStatus.WARNING);
                    }
                }

                // Determine next status (moved inside block)
                if (report.getStatus() == OverallStatus.FAIL) {
                    updateFileStatus(file, PreArchiveStatus.NEEDS_ACTION.getCode(), report);
                } else if (integrity.getStatus() == OverallStatus.WARNING ||
                           isMetadataIncomplete(file)) {
                    updateFileStatus(file, PreArchiveStatus.NEEDS_ACTION.getCode(), report);
                } else {
                    updateFileStatus(file, PreArchiveStatus.READY_TO_ARCHIVE.getCode(), report);
                }
            }

        } catch (Exception e) {
            log.error("四性检测异常: {}", e.getMessage(), e);
            return createFailedReport(checkId, "检测异常: " + e.getMessage());
        }

        return report;
    }

    /**
     * 批量检测
     */
    public List<FourNatureReport> checkMultipleFiles(List<String> fileIds) {
        List<FourNatureReport> reports = new ArrayList<>();
        for (String fileId : fileIds) {
            reports.add(checkSingleFile(fileId));
        }
        return reports;
    }

    private CheckItem checkMetadataIntegrity(ArcFileContent file) {
        CheckItem item = CheckItem.pass("完整性检测", "元数据完整");
        List<String> missing = new ArrayList<>();

        // DA/T 94-2022 必填字段检查
        if (file.getFileName() == null || file.getFileName().isEmpty()) {
            missing.add("文件名");
        }
        if (file.getFileType() == null || file.getFileType().isEmpty()) {
            missing.add("文件类型");
        }
        if (file.getFileSize() == null || file.getFileSize() <= 0) {
            missing.add("文件大小");
        }
        if (file.getFiscalYear() == null || file.getFiscalYear().isEmpty()) {
            item.setStatus(OverallStatus.WARNING);
            missing.add("会计年度");
        }
        if (file.getVoucherType() == null || file.getVoucherType().isEmpty()) {
            item.setStatus(OverallStatus.WARNING);
            missing.add("凭证类型");
        }
        if (file.getCreator() == null || file.getCreator().isEmpty()) {
            item.setStatus(OverallStatus.WARNING);
            missing.add("责任者");
        }
        if (file.getFondsCode() == null || file.getFondsCode().isEmpty()) {
            missing.add("全宗号(建议完善)");
        }

        if (!missing.isEmpty()) {
            item.setMessage("缺失字段: " + String.join(", ", missing));
        }

        return item;
    }

    private boolean isMetadataIncomplete(ArcFileContent file) {
        return file.getFiscalYear() == null || file.getFiscalYear().isEmpty() ||
               file.getVoucherType() == null || file.getVoucherType().isEmpty() ||
               file.getCreator() == null || file.getCreator().isEmpty();
    }

    private void updateFileStatus(ArcFileContent file, String status, FourNatureReport report) {
        file.setPreArchiveStatus(status);
        file.setCheckedTime(LocalDateTime.now());
        file.setCheckResult(report.getStatus().name());
        arcFileContentMapper.updateById(file);
        log.info("文件 {} 状态更新为: {}", file.getId(), status);
    }

    private FourNatureReport createFailedReport(String checkId, String errorMessage) {
        FourNatureReport report = FourNatureReport.builder()
                .checkId(checkId)
                .checkTime(LocalDateTime.now())
                .status(OverallStatus.FAIL)
                .build();
        
        CheckItem failedItem = CheckItem.fail("检测失败", errorMessage);
        report.setAuthenticity(failedItem);
        
        return report;
    }
}
