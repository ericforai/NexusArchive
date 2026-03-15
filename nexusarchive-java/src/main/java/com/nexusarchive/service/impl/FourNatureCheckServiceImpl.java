// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: FourNatureCheckServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.FourNatureCheckService;
import com.nexusarchive.service.FourNatureCoreService;
import com.nexusarchive.util.AmountValidator;
import com.nexusarchive.common.constants.ArchiveConstants;
import com.nexusarchive.common.constants.FourNatureConstants;
import com.nexusarchive.util.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 四性检测服务实现
 * 
 * 严格遵循 DA/T 92-2022 标准执行检测
 * 核心文件检测逻辑已委托给 FourNatureCoreService
 * 
 * @author Agent B - 合规开发工程师
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FourNatureCheckServiceImpl implements FourNatureCheckService {

    private final FourNatureCoreService fourNatureCoreService;
    private final ArcFileContentMapper arcFileContentMapper;
    private final AmountValidator amountValidator;
    private final FileHashUtil fileHashUtil; // Used for deduplication check locally

    @Override
    public FourNatureReport performFullCheck(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        String checkId = UUID.randomUUID().toString();
        log.info("Starting Four-Nature Check [ID: {}] for Voucher: {}", checkId, sip.getHeader().getVoucherNumber());
        
        FourNatureReport report = FourNatureReport.builder()
                .checkId(checkId)
                .checkTime(LocalDateTime.now())
                .archivalCode(null)
                .status(OverallStatus.PASS)
                .build();
        
        // 0. 去重检测 (Deduplication)
        CheckItem deduplication = checkDeduplication(sip, fileStreams);
        if (deduplication.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
            report.setAuthenticity(deduplication);
            log.error("SecurityEvent: De-duplication Check FAILED for CheckID: {}", checkId);
            return report;
        }

        // 1. 真实性检测 (Authenticity)
        CheckItem authenticity = checkAuthenticity(sip, fileStreams);
        report.setAuthenticity(authenticity);
        if (authenticity.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
            log.error("SecurityEvent: Authenticity Check FAILED for CheckID: {}", checkId);
            return report;
        }
        
        // 2. 完整性检测 (Integrity)
        CheckItem integrity = checkIntegrity(sip, fileStreams);
        report.setIntegrity(integrity);
        if (integrity.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
        } else if (integrity.getStatus() == OverallStatus.WARNING) {
            if (report.getStatus() != OverallStatus.FAIL) {
                report.setStatus(OverallStatus.WARNING);
            }
        }
        
        // 3. 可用性检测 (Usability)
        CheckItem usability = checkUsability(sip, fileStreams);
        report.setUsability(usability);
        if (usability.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
        } else if (usability.getStatus() == OverallStatus.WARNING) {
            if (report.getStatus() != OverallStatus.FAIL) {
                report.setStatus(OverallStatus.WARNING);
            }
        }
        
        // 4. 安全性检测 (Safety)
        CheckItem safety = checkSafety(sip, fileStreams);
        report.setSafety(safety);
        if (safety.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
            log.error("SecurityEvent: Safety Check FAILED for CheckID: {}", checkId);
            return report;
        } else if (safety.getStatus() == OverallStatus.WARNING) {
            if (report.getStatus() != OverallStatus.FAIL) {
                report.setStatus(OverallStatus.WARNING);
            }
        }
        
        return report;
    }
    
    /**
     * 去重检测
     *
     * 【合规增强】增加 original_hash 校验
     * 依据：DA/T 92-2022 要求真实性检测覆盖原始哈希
     * 【性能优化】批量查询避免 N+1 问题：收集所有哈希后单次 IN 查询
     */
    private CheckItem checkDeduplication(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem item = CheckItem.pass(FourNatureConstants.CheckType.DEDUPLICATION, FourNatureConstants.SuccessMessage.DEDUPLICATION_PASSED);

        if (sip.getAttachments() == null) return item;

        // Step 1: 收集所有文件哈希值（文件名 -> 哈希映射）
        Map<String, String> fileHashes = new HashMap<>();
        for (AttachmentDto attachment : sip.getAttachments()) {
            String fileName = attachment.getFileName();
            byte[] content = fileStreams.get(fileName);
            if (content == null) continue;

            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                String algo = attachment.getHashAlgorithm();
                if (algo == null || algo.isEmpty()) algo = com.nexusarchive.common.constants.ArchiveConstants.Retention.PERMANENT.equals("PERMANENT") ? "SM3" : "SHA256"; // Fallback to SM3

                String hash;
                if ("SM3".equalsIgnoreCase(algo)) {
                    hash = fileHashUtil.calculateSM3(bais);
                } else {
                    hash = fileHashUtil.calculateSHA256(bais);
                }
                fileHashes.put(fileName, hash);
            } catch (Exception e) {
                // Ignore hash error here, handled in Authenticity
            }
        }

        if (fileHashes.isEmpty()) return item;

        // Step 2: 批量查询所有可能重复的哈希（单次 IN 查询）
        Set<String> uniqueHashes = Set.copyOf(fileHashes.values());

        // ALLOW-QUERYWRAPPER: 动态字段场景需要使用字符串方式构建 IN 查询
        QueryWrapper<ArcFileContent> qw = new QueryWrapper<>();
        qw.select(ArchiveConstants.Fields.FILE_HASH, ArchiveConstants.Fields.ORIGINAL_HASH);  // 只查询哈希字段，减少数据传输
        qw.and(w -> w.in(ArchiveConstants.Fields.FILE_HASH, uniqueHashes).or().in(ArchiveConstants.Fields.ORIGINAL_HASH, uniqueHashes));

        // 使用 selectMaps 只获取哈希值，避免加载完整实体
        List<java.util.Map<String, Object>> hashMaps = arcFileContentMapper.selectMaps(qw);

        // 提取所有已存在的哈希值
        Set<String> existingHashes = hashMaps.stream()
            .flatMap(m -> Stream.of(
                (String) m.get(ArchiveConstants.Fields.FILE_HASH),
                (String) m.get(ArchiveConstants.Fields.ORIGINAL_HASH)
            ))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // Step 3: 标记重复的文件
        for (Map.Entry<String, String> entry : fileHashes.entrySet()) {
            String fileName = entry.getKey();
            String hash = entry.getValue();
            if (existingHashes.contains(hash)) {
                item.addError(String.format(FourNatureConstants.Prompt.DUPLICATE_FILE, fileName, hash));
            }
        }

        return item;
    }

    private CheckItem checkAuthenticity(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem combinedItem = CheckItem.pass(FourNatureConstants.CheckType.AUTHENTICITY, FourNatureConstants.SuccessMessage.AUTHENTICITY_PASSED);
        List<String> details = new ArrayList<>();
        
        if (sip.getAttachments() == null) return combinedItem;
        
        for (AttachmentDto attachment : sip.getAttachments()) {
            byte[] content = fileStreams.get(attachment.getFileName());
            if (content == null) {
                combinedItem.addError(FourNatureConstants.Prompt.MISSING_CONTENT + attachment.getFileName());
                continue;
            }
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                CheckItem singleResult = fourNatureCoreService.checkSingleFileAuthenticity(
                    bais,
                    attachment.getFileName(),
                    attachment.getFileHash(),
                    attachment.getHashAlgorithm(),
                    attachment.getFileType()
                );
                mergeResult(combinedItem, singleResult, details, attachment.getFileName());
            } catch (Exception e) {
                log.warn("四性检测-真实性检查失败: {}", e.getMessage());
            }
        }
        
        if (!details.isEmpty()) combinedItem.setMessage(String.join("; ", details));
        return combinedItem;
    }
    
    // ... Integrity uses SIP structure, stays largely same but cleaned up ...
    private CheckItem checkIntegrity(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem item = CheckItem.pass(FourNatureConstants.CheckType.INTEGRITY, FourNatureConstants.SuccessMessage.INTEGRITY_PASSED);
        VoucherHeadDto header = sip.getHeader();
        
        if (header.getFondsCode() == null) item.addError(FourNatureConstants.Prompt.MISSING_FONDS_CODE);
        if (header.getVoucherNumber() == null) item.addError(FourNatureConstants.Prompt.MISSING_VOUCHER_NUMBER);
        if (header.getAccountPeriod() == null) item.addError(FourNatureConstants.Prompt.MISSING_ACCOUNT_PERIOD);
        
        if (header.getTotalAmount() != null) {
            AmountValidator.ValidationResult result = amountValidator.validateAmount(header.getTotalAmount());
            if (!result.isValid()) {
                item.addError("金额格式不符合会计准则: " + result.getMessage());
            }
        }
        
        int declaredCount = header.getAttachmentCount();
        int actualCount = (sip.getAttachments() != null) ? sip.getAttachments().size() : 0;
        
        if (declaredCount != actualCount) {
            item.addError(String.format("Attachment count mismatch. Header: %d, List: %d", declaredCount, actualCount));
        }
        
        return item;
    }
    
    private CheckItem checkUsability(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem combinedItem = CheckItem.pass(FourNatureConstants.CheckType.USABILITY, FourNatureConstants.SuccessMessage.USABILITY_PASSED);
        List<String> details = new ArrayList<>();
        
        if (sip.getAttachments() == null) return combinedItem;

        for (AttachmentDto attachment : sip.getAttachments()) {
            byte[] content = fileStreams.get(attachment.getFileName());
            if (content == null) continue;
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                CheckItem singleResult = fourNatureCoreService.checkSingleFileUsability(
                    bais,
                    attachment.getFileName(),
                    attachment.getFileType()
                );
                mergeResult(combinedItem, singleResult, details, attachment.getFileName());
            } catch (Exception e) {
                log.warn("四性检测-可用性检查失败: {}", e.getMessage());
            }
        }
        
        if (!details.isEmpty()) combinedItem.setMessage(String.join("; ", details));
        return combinedItem;
    }
    
    private CheckItem checkSafety(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem combinedItem = CheckItem.pass(FourNatureConstants.CheckType.SAFETY, FourNatureConstants.SuccessMessage.SAFETY_PASSED);
        List<String> details = new ArrayList<>();
        
        if (sip.getAttachments() == null) return combinedItem;

        for (AttachmentDto attachment : sip.getAttachments()) {
            byte[] content = fileStreams.get(attachment.getFileName());
            if (content == null) continue;
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                CheckItem singleResult = fourNatureCoreService.checkSingleFileSafety(
                    bais,
                    attachment.getFileName()
                );
                
                if (singleResult.getStatus() == OverallStatus.FAIL) {
                    combinedItem.setStatus(OverallStatus.FAIL);
                    combinedItem.getErrors().add(attachment.getFileName() + ": " + singleResult.getMessage());
                } else if (singleResult.getStatus() == OverallStatus.WARNING) {
                    if (combinedItem.getStatus() != OverallStatus.FAIL) {
                        combinedItem.setStatus(OverallStatus.WARNING);
                    }
                    details.add(attachment.getFileName() + ": " + singleResult.getMessage());
                }
            } catch (Exception e) {
                log.warn("四性检测-安全性检查失败: {}", e.getMessage());
            }
        }
        
        if (!details.isEmpty()) {
            combinedItem.setMessage(String.join("; ", details));
        }
        
        return combinedItem;
    }

    @Override
    public FourNatureReport performHealthCheck(com.nexusarchive.entity.Archive archive, List<ArcFileContent> files) {
        String checkId = UUID.randomUUID().toString();
        log.info("Starting Health Check [ID: {}] for Archive: {}", checkId, archive.getArchiveCode());

        FourNatureReport report = FourNatureReport.builder()
                .checkId(checkId)
                .checkTime(LocalDateTime.now())
                .archivalCode(archive.getArchiveCode())
                .status(OverallStatus.PASS)
                .build();

        // 1. Authenticity
        CheckItem authenticity = performAuthenticityHealthCheck(files);
        report.setAuthenticity(authenticity);
        if (authenticity.getStatus() == OverallStatus.FAIL) report.setStatus(OverallStatus.FAIL);

        // 2. Integrity (Archive level metadata)
        CheckItem integrity = performIntegrityHealthCheck(archive, files);
        report.setIntegrity(integrity);

        // 3. Usability
        CheckItem usability = performUsabilityHealthCheck(files);
        report.setUsability(usability);

        // 4. Safety
        CheckItem safety = performSafetyHealthCheck(files);
        report.setSafety(safety);
        if (safety.getStatus() == OverallStatus.FAIL) report.setStatus(OverallStatus.FAIL);

        // Check for warnings if still passing
        if (report.getStatus() == OverallStatus.PASS && hasAnyWarning(authenticity, integrity, usability, safety)) {
            report.setStatus(OverallStatus.WARNING);
        }

        return report;
    }

    /**
     * Perform authenticity health check on files
     */
    private CheckItem performAuthenticityHealthCheck(List<ArcFileContent> files) {
        CheckItem authenticity = CheckItem.pass(FourNatureConstants.CheckType.AUTHENTICITY, FourNatureConstants.SuccessMessage.AUTHENTICITY_PASSED);
        List<String> authDetails = new ArrayList<>();

        for (ArcFileContent file : files) {
            CheckItem single = checkFileAuthenticity(file);
            mergeResult(authenticity, single, authDetails, file.getFileName());
        }

        if (!authDetails.isEmpty()) authenticity.setMessage(String.join("; ", authDetails));
        return authenticity;
    }

    /**
     * Check authenticity of a single file
     */
    private CheckItem checkFileAuthenticity(ArcFileContent file) {
        java.nio.file.Path path = java.nio.file.Paths.get(file.getStoragePath());
        if (!java.nio.file.Files.exists(path)) {
            return CheckItem.fail(FourNatureConstants.CheckType.AUTHENTICITY, "File not found: " + file.getFileName());
        }

        try (java.io.InputStream is = new java.io.BufferedInputStream(new java.io.FileInputStream(path.toFile()))) {
            return fourNatureCoreService.checkSingleFileAuthenticity(
                    is, file.getFileName(), file.getOriginalHash(), file.getHashAlgorithm(), file.getFileType()
            );
        } catch (Exception e) {
            return CheckItem.fail(FourNatureConstants.CheckType.AUTHENTICITY, "Error checking " + file.getFileName() + ": " + e.getMessage());
        }
    }

    /**
     * Perform integrity health check on archive metadata
     */
    private CheckItem performIntegrityHealthCheck(com.nexusarchive.entity.Archive archive, List<ArcFileContent> files) {
        CheckItem integrity = CheckItem.pass(FourNatureConstants.CheckType.INTEGRITY, "Metadata complete");
        if (archive.getUniqueBizId() == null) integrity.addError("Missing Unique Biz ID");
        if (archive.getAmount() == null) integrity.addError("Missing Amount");
        if (files.isEmpty()) integrity.addError("No files associated");
        return integrity;
    }

    /**
     * Perform usability health check on files
     */
    private CheckItem performUsabilityHealthCheck(List<ArcFileContent> files) {
        CheckItem usability = CheckItem.pass(FourNatureConstants.CheckType.USABILITY, FourNatureConstants.SuccessMessage.FILES_ACCESSIBLE);
        List<String> useDetails = new ArrayList<>();

        for (ArcFileContent file : files) {
            CheckItem single = checkFileUsability(file);
            if (single != null) {
                mergeResult(usability, single, useDetails, file.getFileName());
            }
        }

        if (!useDetails.isEmpty()) usability.setMessage(String.join("; ", useDetails));
        return usability;
    }

    /**
     * Check usability of a single file
     */
    private CheckItem checkFileUsability(ArcFileContent file) {
        java.nio.file.Path path = java.nio.file.Paths.get(file.getStoragePath());
        if (!java.nio.file.Files.exists(path)) {
            return null;
        }

        try (java.io.InputStream is = new java.io.FileInputStream(path.toFile())) {
            return fourNatureCoreService.checkSingleFileUsability(is, file.getFileName(), file.getFileType());
        } catch (Exception e) {
            return CheckItem.fail(FourNatureConstants.CheckType.USABILITY, "Usability error: " + file.getFileName() + " - " + e.getMessage());
        }
    }

    /**
     * Perform safety health check on files
     */
    private CheckItem performSafetyHealthCheck(List<ArcFileContent> files) {
        CheckItem safety = CheckItem.pass(FourNatureConstants.CheckType.SAFETY, "Safe");

        for (ArcFileContent file : files) {
            CheckItem single = checkFileSafety(file);
            if (single != null && single.getStatus() != OverallStatus.PASS) {
                safety.setStatus(single.getStatus());
                safety.addError(single.getMessage());
            }
        }

        return safety;
    }

    /**
     * Check safety of a single file
     */
    private CheckItem checkFileSafety(ArcFileContent file) {
        java.nio.file.Path path = java.nio.file.Paths.get(file.getStoragePath());
        if (!java.nio.file.Files.exists(path)) {
            return null;
        }

        try (java.io.InputStream is = new java.io.FileInputStream(path.toFile())) {
            return fourNatureCoreService.checkSingleFileSafety(is, file.getFileName());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if any of the check items has a warning status
     */
    private boolean hasAnyWarning(CheckItem authenticity, CheckItem integrity,
                                  CheckItem usability, CheckItem safety) {
        return authenticity.getStatus() == OverallStatus.WARNING
                || integrity.getStatus() == OverallStatus.WARNING
                || usability.getStatus() == OverallStatus.WARNING
                || safety.getStatus() == OverallStatus.WARNING;
    }
    
    private void mergeResult(CheckItem target, CheckItem source, List<String> detailsCollector, String fileName) {
        if (source.getStatus() == OverallStatus.FAIL) {
            target.setStatus(OverallStatus.FAIL);
            target.addError(fileName + ": " + source.getMessage());
        } else if (source.getStatus() == OverallStatus.WARNING) {
            if (target.getStatus() != OverallStatus.FAIL) {
                target.setStatus(OverallStatus.WARNING);
            }
            detailsCollector.add(fileName + ": " + source.getMessage());
        } else {
             // Pass with details
             if (source.getMessage() != null && !source.getMessage().isEmpty()) {
                 detailsCollector.add(fileName + ": " + source.getMessage());
             }
        }
    }
}
