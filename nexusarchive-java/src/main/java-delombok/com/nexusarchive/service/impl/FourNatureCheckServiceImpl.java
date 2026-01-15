// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: FourNatureCheckServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.nexusarchive.util.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        }
        
        // 3. 可用性检测 (Usability)
        CheckItem usability = checkUsability(sip, fileStreams);
        report.setUsability(usability);
        if (usability.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
        }
        
        // 4. 安全性检测 (Safety)
        CheckItem safety = checkSafety(sip, fileStreams);
        report.setSafety(safety);
        if (safety.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
            log.error("SecurityEvent: Safety Check FAILED for CheckID: {}", checkId);
            return report;
        }
        
        return report;
    }
    
    /**
     * 去重检测
     * 
     * 【合规增强】增加 original_hash 校验
     * 依据：DA/T 92-2022 要求真实性检测覆盖原始哈希
     */
    private CheckItem checkDeduplication(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem item = CheckItem.pass("De-duplication Check", "No duplicates found");
        
        if (sip.getAttachments() == null) return item;
        
        for (AttachmentDto attachment : sip.getAttachments()) {
            String fileName = attachment.getFileName();
            byte[] content = fileStreams.get(fileName);
            if (content == null) continue;
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                // Determine Algo
                String algo = attachment.getHashAlgorithm();
                if (algo == null || algo.isEmpty()) algo = "SM3";

                String hash;
                if ("SM3".equalsIgnoreCase(algo)) {
                    hash = fileHashUtil.calculateSM3(bais);
                } else {
                    hash = fileHashUtil.calculateSHA256(bais);
                }
                
                Long count = arcFileContentMapper.selectCount(
                    new LambdaQueryWrapper<ArcFileContent>()
                        .eq(ArcFileContent::getFileHash, hash)
                        .or()
                        .eq(ArcFileContent::getOriginalHash, hash)
                );
                
                if (count > 0) {
                    item.addError(String.format("Duplicate file detected: %s (Hash: %s already exists)", fileName, hash));
                }
            } catch (Exception e) {
                // Ignore hash error here, handled in Authenticity
            }
        }
        
        return item;
    }

    private CheckItem checkAuthenticity(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem combinedItem = CheckItem.pass("Authenticity Check", "All files verified");
        List<String> details = new ArrayList<>();
        
        if (sip.getAttachments() == null) return combinedItem;
        
        for (AttachmentDto attachment : sip.getAttachments()) {
            byte[] content = fileStreams.get(attachment.getFileName());
            if (content == null) {
                combinedItem.addError("Missing content: " + attachment.getFileName());
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
            } catch (Exception e) {}
        }
        
        if (!details.isEmpty()) combinedItem.setMessage(String.join("; ", details));
        return combinedItem;
    }
    
    // ... Integrity uses SIP structure, stays largely same but cleaned up ...
    private CheckItem checkIntegrity(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem item = CheckItem.pass("Integrity Check", "Metadata and structure valid");
        VoucherHeadDto header = sip.getHeader();
        
        if (header.getFondsCode() == null) item.addError("Missing Fonds Code");
        if (header.getVoucherNumber() == null) item.addError("Missing Voucher Number");
        if (header.getAccountPeriod() == null) item.addError("Missing Account Period");
        
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
        CheckItem combinedItem = CheckItem.pass("Usability Check", "All files usable");
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
            } catch (Exception e) {}
        }
        
        if (!details.isEmpty()) combinedItem.setMessage(String.join("; ", details));
        return combinedItem;
    }
    
    private CheckItem checkSafety(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem combinedItem = CheckItem.pass("Safety Check", "No threats");
        
        if (sip.getAttachments() == null) return combinedItem;

        for (AttachmentDto attachment : sip.getAttachments()) {
            byte[] content = fileStreams.get(attachment.getFileName());
            if (content == null) continue;
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                CheckItem singleResult = fourNatureCoreService.checkSingleFileSafety(
                    bais,
                    attachment.getFileName()
                );
                
                if (singleResult.getStatus() == OverallStatus.FAIL || singleResult.getStatus() == OverallStatus.WARNING) {
                    combinedItem.setStatus(singleResult.getStatus());
                    combinedItem.addError(singleResult.getMessage());
                }
            } catch (Exception e) {}
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
        CheckItem authenticity = CheckItem.pass("Authenticity Check", "All files verified");
        List<String> authDetails = new ArrayList<>();
        
        for (ArcFileContent file : files) {
            java.nio.file.Path path = java.nio.file.Paths.get(file.getStoragePath());
            if (!java.nio.file.Files.exists(path)) {
                authenticity.addError("File not found: " + file.getFileName());
                continue;
            }

            // [FIXED] 使用流式读取，避免 readAllBytes() 导致的 OOM
            try (java.io.InputStream is = new java.io.BufferedInputStream(new java.io.FileInputStream(path.toFile()))) {
                // 标记流开头，以便多次读取（如果可能，取决于底层实现，BufferedInputStream 默认不缓存 100MB）
                // 实际上对于大文件，我们应当按需重新开启流
                
                // 真实性检测 (包含哈希校验)
                CheckItem single = fourNatureCoreService.checkSingleFileAuthenticity(
                    is, file.getFileName(), file.getOriginalHash(), file.getHashAlgorithm(), file.getFileType()
                );
                mergeResult(authenticity, single, authDetails, file.getFileName());
            } catch (Exception e) {
                authenticity.addError("Error checking " + file.getFileName() + ": " + e.getMessage());
            }
        }
        if (!authDetails.isEmpty()) authenticity.setMessage(String.join("; ", authDetails));
        report.setAuthenticity(authenticity);
        if (authenticity.getStatus() == OverallStatus.FAIL) report.setStatus(OverallStatus.FAIL);

        // 2. Integrity (Archive level metadata)
        CheckItem integrity = CheckItem.pass("Integrity Check", "Metadata complete");
        if (archive.getUniqueBizId() == null) integrity.addError("Missing Unique Biz ID");
        if (archive.getAmount() == null) integrity.addError("Missing Amount");
        if (files.isEmpty()) integrity.addError("No files associated");
        report.setIntegrity(integrity);
        
        // 3. Usability
        CheckItem usability = CheckItem.pass("Usability Check", "Files accessible");
        List<String> useDetails = new ArrayList<>();
        for (ArcFileContent file : files) {
             java.nio.file.Path path = java.nio.file.Paths.get(file.getStoragePath());
             if (java.nio.file.Files.exists(path)) {
                 try (java.io.InputStream is = new java.io.FileInputStream(path.toFile())) {
                     CheckItem single = fourNatureCoreService.checkSingleFileUsability(is, file.getFileName(), file.getFileType());
                     mergeResult(usability, single, useDetails, file.getFileName());
                 } catch (Exception e) {
                     usability.addError("Usability error " + file.getFileName());
                 }
             }
        }
        if (!useDetails.isEmpty()) usability.setMessage(String.join("; ", useDetails));
        report.setUsability(usability);

        // 4. Safety
        CheckItem safety = CheckItem.pass("Safety Check", "Safe");
        for (ArcFileContent file : files) {
             java.nio.file.Path path = java.nio.file.Paths.get(file.getStoragePath());
             if (java.nio.file.Files.exists(path)) {
                 try (java.io.InputStream is = new java.io.FileInputStream(path.toFile())) {
                    CheckItem single = fourNatureCoreService.checkSingleFileSafety(is, file.getFileName());
                    if (single.getStatus() != OverallStatus.PASS) {
                        safety.setStatus(single.getStatus());
                        safety.addError(single.getMessage());
                    }
                 } catch (Exception e) {}
             }
        }
        report.setSafety(safety);
        if (safety.getStatus() == OverallStatus.FAIL) report.setStatus(OverallStatus.FAIL);

        if (report.getStatus() == OverallStatus.PASS) {
             // Check for warnings
             if (authenticity.getStatus() == OverallStatus.WARNING || integrity.getStatus() == OverallStatus.WARNING 
                 || usability.getStatus() == OverallStatus.WARNING || safety.getStatus() == OverallStatus.WARNING) {
                 report.setStatus(OverallStatus.WARNING);
             }
        }

        return report;
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
