package com.nexusarchive.service;

import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.adapter.VirusScanAdapter;
import com.nexusarchive.util.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
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
    private final FileHashUtil fileHashUtil;
    private final VirusScanAdapter virusScanAdapter;
    private final Tika tika = new Tika();

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

            byte[] content = Files.readAllBytes(filePath);

            // 1. 真实性检测 - 哈希校验
            CheckItem authenticity = checkAuthenticity(file, content);
            report.setAuthenticity(authenticity);
            if (authenticity.getStatus() == OverallStatus.FAIL) {
                report.setStatus(OverallStatus.FAIL);
                updateFileStatus(file, PreArchiveStatus.CHECK_FAILED.getCode(), report);
                return report;
            }

            // 2. 完整性检测 - 元数据完整性
            CheckItem integrity = checkMetadataIntegrity(file);
            report.setIntegrity(integrity);
            if (integrity.getStatus() == OverallStatus.FAIL) {
                report.setStatus(OverallStatus.FAIL);
            }

            // 3. 可用性检测 - 文件格式验证
            CheckItem usability = checkFileUsability(file, content);
            report.setUsability(usability);
            if (usability.getStatus() == OverallStatus.FAIL) {
                report.setStatus(OverallStatus.FAIL);
            }

            // 4. 安全性检测 - 病毒扫描
            CheckItem safety = checkFileSafety(file, content);
            report.setSafety(safety);
            if (safety.getStatus() == OverallStatus.FAIL) {
                report.setStatus(OverallStatus.FAIL);
                updateFileStatus(file, PreArchiveStatus.CHECK_FAILED.getCode(), report);
                return report;
            }

            // Update overall status
            if (report.getStatus() == OverallStatus.PASS) {
                boolean hasWarning = authenticity.getStatus() == OverallStatus.WARNING ||
                        integrity.getStatus() == OverallStatus.WARNING ||
                        usability.getStatus() == OverallStatus.WARNING ||
                        safety.getStatus() == OverallStatus.WARNING;
                if (hasWarning) {
                    report.setStatus(OverallStatus.WARNING);
                }
            }

            // Determine next status based on check result
            if (report.getStatus() == OverallStatus.FAIL) {
                updateFileStatus(file, PreArchiveStatus.CHECK_FAILED.getCode(), report);
            } else if (integrity.getStatus() == OverallStatus.WARNING || 
                       isMetadataIncomplete(file)) {
                updateFileStatus(file, PreArchiveStatus.PENDING_METADATA.getCode(), report);
            } else {
                updateFileStatus(file, PreArchiveStatus.PENDING_ARCHIVE.getCode(), report);
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

    private CheckItem checkAuthenticity(ArcFileContent file, byte[] content) {
        CheckItem item = CheckItem.pass("真实性检测", "哈希校验通过");
        List<String> details = new ArrayList<>();

        try {
            // 计算当前哈希
            String algo = file.getHashAlgorithm() != null ? file.getHashAlgorithm() : "SM3";
            String currentHash;
            if ("SM3".equalsIgnoreCase(algo)) {
                currentHash = fileHashUtil.calculateSM3(new ByteArrayInputStream(content));
            } else {
                currentHash = fileHashUtil.calculateSHA256(new ByteArrayInputStream(content));
            }

            // 更新当前哈希
            file.setCurrentHash(currentHash);

            // 与原始哈希比对
            if (file.getOriginalHash() != null && !file.getOriginalHash().isEmpty()) {
                if (!file.getOriginalHash().equalsIgnoreCase(currentHash)) {
                    item.addError(String.format("哈希不一致: 原始=%s, 当前=%s", 
                            file.getOriginalHash(), currentHash));
                } else {
                    details.add("文件哈希一致性验证通过");
                }
            } else if (file.getFileHash() != null && !file.getFileHash().isEmpty()) {
                // Fallback to fileHash
                if (!file.getFileHash().equalsIgnoreCase(currentHash)) {
                    item.setStatus(OverallStatus.WARNING);
                    details.add("WARNING: 哈希与记录不一致，可能是文件被修改");
                } else {
                    details.add("文件哈希验证通过");
                }
            } else {
                item.setStatus(OverallStatus.WARNING);
                details.add("WARNING: 无原始哈希记录，跳过校验");
            }

        } catch (Exception e) {
            item.addError("哈希计算失败: " + e.getMessage());
        }

        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }
        return item;
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
            // item.setStatus(OverallStatus.WARNING); // 全宗号可选，不影响完整性状态
            missing.add("全宗号(建议完善)");
        }

        if (!missing.isEmpty()) {
            item.setMessage("缺失字段: " + String.join(", ", missing));
        }

        return item;
    }

    private CheckItem checkFileUsability(ArcFileContent file, byte[] content) {
        CheckItem item = CheckItem.pass("可用性检测", "文件格式有效");
        List<String> details = new ArrayList<>();

        try {
            // 使用 Tika 检测实际文件类型
            String detectedType = tika.detect(content);
            String declaredType = file.getFileType();

            if (declaredType != null) {
                boolean match = checkTypeMatch(declaredType, detectedType);
                if (!match) {
                    item.setStatus(OverallStatus.WARNING);
                    details.add(String.format("文件类型不匹配: 声明=%s, 检测=%s", 
                            declaredType, detectedType));
                } else {
                    details.add("文件格式验证通过: " + detectedType);
                }
            }

            // 检查文件是否可读取
            if (content.length == 0) {
                item.addError("文件内容为空");
            }

        } catch (Exception e) {
            item.addError("格式检测失败: " + e.getMessage());
        }

        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }
        return item;
    }

    private CheckItem checkFileSafety(ArcFileContent file, byte[] content) {
        CheckItem item = CheckItem.pass("安全性检测", "无安全威胁");

        try {
            boolean isSafe = virusScanAdapter.scan(content, file.getFileName());
            if (!isSafe) {
                item.addError("检测到安全威胁: " + file.getFileName());
            }
        } catch (Exception e) {
            item.setStatus(OverallStatus.WARNING);
            item.setMessage("安全扫描跳过: " + e.getMessage());
        }

        return item;
    }

    private boolean checkTypeMatch(String declared, String detected) {
        if (declared == null || detected == null) return false;
        declared = declared.toUpperCase();
        detected = detected.toLowerCase();

        if (declared.equals("PDF") && detected.contains("pdf")) return true;
        if (declared.equals("OFD") && (detected.contains("xml") || detected.contains("zip") || detected.contains("ofd"))) return true;
        if (declared.equals("XML") && detected.contains("xml")) return true;
        if ((declared.equals("JPG") || declared.equals("JPEG")) && detected.contains("jpeg")) return true;
        if (declared.equals("PNG") && detected.contains("png")) return true;

        return false;
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
