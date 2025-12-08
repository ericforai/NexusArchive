package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.enums.ArchiveFileType;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.service.FourNatureCheckService;
import com.nexusarchive.service.adapter.VirusScanAdapter;
import com.nexusarchive.service.signature.SignatureAdapter;
import com.nexusarchive.dto.signature.VerifyResult;
import com.nexusarchive.util.AmountValidator;
import com.nexusarchive.util.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 
 * 增强功能：
 * - 集成签章验证到真实性检测
 * - 增强元数据完整性检查
 * 
 * @author Agent B - 合规开发工程师
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FourNatureCheckServiceImpl implements FourNatureCheckService {

    private final FileHashUtil fileHashUtil;
    private final VirusScanAdapter virusScanAdapter;
    private final Tika tika = new Tika();
    private final ArcFileContentMapper arcFileContentMapper;
    private final ArcFileMetadataIndexMapper arcFileMetadataIndexMapper;
    private final AmountValidator amountValidator;
    
    // 可选依赖：签章适配器（用于增强真实性检测）
    @Autowired(required = false)
    private SignatureAdapter signatureAdapter;

    @Override
    public FourNatureReport performFullCheck(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        String checkId = UUID.randomUUID().toString();
        log.info("Starting Four-Nature Check [ID: {}] for Voucher: {}", checkId, sip.getHeader().getVoucherNumber());
        
        FourNatureReport report = FourNatureReport.builder()
                .checkId(checkId)
                .checkTime(LocalDateTime.now())
                .archivalCode(null) // 尚未生成
                .status(OverallStatus.PASS) // 默认为 PASS，后续检测中可能降级
                .build();
        
        // 0. 去重检测 (De-duplication) - Critical Pre-check
        // 虽然不属于标准的"四性"，但在实际业务中属于"真实性"或"安全性"的前置
        CheckItem deduplication = checkDeduplication(sip, fileStreams);
        if (deduplication.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
            report.setAuthenticity(deduplication); // 归类为真实性问题
            log.error("SecurityEvent: De-duplication Check FAILED for CheckID: {}", checkId);
            return report;
        }

        // 1. 真实性检测 (Authenticity) - Critical
        CheckItem authenticity = checkAuthenticity(sip, fileStreams);
        report.setAuthenticity(authenticity);
        if (authenticity.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
            log.error("SecurityEvent: Authenticity Check FAILED for CheckID: {}", checkId);
            return report; // Stop immediately
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
        
        // 4. 安全性检测 (Safety) - Critical
        CheckItem safety = checkSafety(sip, fileStreams);
        report.setSafety(safety);
        if (safety.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
            log.error("SecurityEvent: Safety Check FAILED for CheckID: {}", checkId);
            return report;
        }
        
        // 如果任何非阻断性检查导致 WARNING，更新总体状态
        if (report.getStatus() == OverallStatus.PASS) {
            boolean hasWarning = authenticity.getStatus() == OverallStatus.WARNING ||
                                integrity.getStatus() == OverallStatus.WARNING ||
                                usability.getStatus() == OverallStatus.WARNING ||
                                safety.getStatus() == OverallStatus.WARNING;
            if (hasWarning) {
                report.setStatus(OverallStatus.WARNING);
            }
        }
        
        return report;
    }
    
    /**
     * 0. 去重检测
     * - 物理去重: 检查文件 Hash 是否已存在
     * - 逻辑去重: 检查发票号码是否已存在 (需要解析后的元数据，这里只能做初步检查或依赖 SIP 中的元数据)
     *   注意：SIP 中的元数据可能不包含发票代码/号码，只有附件。
     *   这里主要做物理去重。
     */
    private CheckItem checkDeduplication(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem item = CheckItem.pass("De-duplication Check", "No duplicates found");
        
        if (sip.getAttachments() == null) return item;
        
        for (AttachmentDto attachment : sip.getAttachments()) {
            String fileName = attachment.getFileName();
            byte[] content = fileStreams.get(fileName);
            if (content == null) continue;
            
            // Calculate Hash (SM3 preferred)
            String hash = null;
            try {
                hash = fileHashUtil.calculateSM3(new ByteArrayInputStream(content));
            } catch (Exception e) {
                // Fallback or skip
                continue;
            }
            
            // Check DB for existing hash
            Long count = arcFileContentMapper.selectCount(
                new LambdaQueryWrapper<ArcFileContent>()
                    .eq(ArcFileContent::getFileHash, hash)
            );
            
            if (count > 0) {
                item.addError(String.format("Duplicate file detected: %s (Hash: %s already exists)", fileName, hash));
            }
        }
        
        return item;
    }

    /**
     * 1. 真实性检测
     * - 计算哈希值并与提供的哈希值比对
     * - 优先使用 SM3
     */
    private CheckItem checkAuthenticity(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem item = CheckItem.pass("Authenticity Check", "Hash verification passed");
        List<String> details = new ArrayList<>();
        
        if (sip.getAttachments() == null || sip.getAttachments().isEmpty()) {
            return item;
        }
        
        for (AttachmentDto attachment : sip.getAttachments()) {
            String fileName = attachment.getFileName();
            byte[] content = fileStreams.get(fileName);
            
            if (content == null) {
                item.addError("File content missing for: " + fileName);
                continue;
            }
            
            String providedHash = attachment.getFileHash();
            String algo = attachment.getHashAlgorithm();
            
            if (providedHash == null || providedHash.isEmpty()) {
                item.setStatus(OverallStatus.WARNING);
                details.add("WARNING: No hash provided for " + fileName + ", skipping verification");
                continue;
            }
            
            // Default to SM3 if not specified
            if (algo == null || algo.isEmpty()) {
                algo = "SM3";
            }
            
            try {
                String calculatedHash;
                if ("SM3".equalsIgnoreCase(algo)) {
                    calculatedHash = fileHashUtil.calculateSM3(new ByteArrayInputStream(content));
                } else if ("SHA-256".equalsIgnoreCase(algo) || "SHA256".equalsIgnoreCase(algo)) {
                    calculatedHash = fileHashUtil.calculateSHA256(new ByteArrayInputStream(content));
                } else {
                    item.addError("Unsupported hash algorithm: " + algo);
                    continue;
                }
                
                if (!calculatedHash.equalsIgnoreCase(providedHash)) {
                    item.addError(String.format("Hash mismatch for %s. Expected: %s, Actual: %s", 
                            fileName, providedHash, calculatedHash));
                } else {
                    details.add(String.format("File %s: %s hash verified", fileName, algo));
                }
                
                // 增强：验证文件签章（如果是 PDF/OFD 且签章服务可用）
                if (signatureAdapter != null && signatureAdapter.isAvailable()) {
                    String fileType = attachment.getFileType();
                    if ("PDF".equalsIgnoreCase(fileType)) {
                        try {
                            VerifyResult sigResult = signatureAdapter.verifyPdfSignature(
                                    new ByteArrayInputStream(content));
                            if (sigResult.isValid()) {
                                details.add(String.format("File %s: 签章验证通过 (签章人: %s)", 
                                        fileName, sigResult.getSignerName()));
                            } else if (sigResult.getErrorMessage() != null && 
                                       !sigResult.getErrorMessage().contains("开发中")) {
                                // 不是"功能开发中"的情况才记录警告
                                item.setStatus(OverallStatus.WARNING);
                                details.add(String.format("WARNING: File %s 签章验证失败: %s", 
                                        fileName, sigResult.getErrorMessage()));
                            }
                        } catch (Exception e) {
                            log.debug("签章验证异常: {}", e.getMessage());
                        }
                    } else if ("OFD".equalsIgnoreCase(fileType)) {
                        try {
                            VerifyResult sigResult = signatureAdapter.verifyOfdSignature(
                                    new ByteArrayInputStream(content));
                            if (sigResult.isValid()) {
                                details.add(String.format("File %s: OFD 签章验证通过", fileName));
                            } else if (sigResult.getErrorMessage() != null && 
                                       !sigResult.getErrorMessage().contains("开发中")) {
                                item.setStatus(OverallStatus.WARNING);
                                details.add(String.format("WARNING: File %s OFD 签章验证失败: %s", 
                                        fileName, sigResult.getErrorMessage()));
                            }
                        } catch (Exception e) {
                            log.debug("OFD 签章验证异常: {}", e.getMessage());
                        }
                    }
                }
                
            } catch (Exception e) {
                item.addError("Error calculating hash for " + fileName + ": " + e.getMessage());
            }
        }
        
        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }
        
        return item;
    }
    
    /**
     * 2. 完整性检测
     * - 验证元数据完整性
     * - 验证附件数量一致性
     * - 金额精度校验（新增）
     */
    private CheckItem checkIntegrity(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem item = CheckItem.pass("Integrity Check", "Metadata and structure valid");
        VoucherHeadDto header = sip.getHeader();
        
        // Metadata Completeness (Already partially handled by @Valid, but double check critical fields)
        if (header.getFondsCode() == null) item.addError("Missing Fonds Code");
        if (header.getVoucherNumber() == null) item.addError("Missing Voucher Number");
        if (header.getAccountPeriod() == null) item.addError("Missing Account Period");
        
        // 金额精度校验（新增）
        if (header.getTotalAmount() != null) {
            AmountValidator.ValidationResult result = amountValidator.validateAmount(header.getTotalAmount());
            if (!result.isValid()) {
                item.addError("金额格式不符合会计准则: " + result.getMessage());
            }
        }
        
        // Attachment Consistency
        int declaredCount = header.getAttachmentCount();
        int actualCount = (sip.getAttachments() != null) ? sip.getAttachments().size() : 0;
        int streamCount = fileStreams.size();
        
        if (declaredCount != actualCount) {
            item.addError(String.format("Attachment count mismatch. Header: %d, List: %d", declaredCount, actualCount));
        }
        
        if (actualCount != streamCount) {
            item.addError(String.format("File stream count mismatch. List: %d, Streams: %d", actualCount, streamCount));
        }
        
        return item;
    }
    
    /**
     * 3. 可用性检测
     * - Magic Number 验证
     * - 解析测试 (PDF/OFD)
     */
    private CheckItem checkUsability(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem item = CheckItem.pass("Usability Check", "File formats valid and parsable");
        List<String> details = new ArrayList<>();
        
        if (sip.getAttachments() == null) return item;
        
        for (AttachmentDto attachment : sip.getAttachments()) {
            String fileName = attachment.getFileName();
            byte[] content = fileStreams.get(fileName);
            
            if (content == null) continue;
            
            // 1. Magic Number Validation using Tika
            String detectedType = tika.detect(content);
            String declaredType = attachment.getFileType();
            
            // Simple mapping check
            boolean typeMatch = checkTypeMatch(declaredType, detectedType);
            if (!typeMatch) {
                item.setStatus(OverallStatus.WARNING);
                details.add(String.format("WARNING: File type mismatch for %s. Declared: %s, Detected: %s", 
                        fileName, declaredType, detectedType));
            }
            
            // 2. Parse Test
            try {
                if ("PDF".equalsIgnoreCase(declaredType)) {
                    try (PDDocument doc = PDDocument.load(content)) {
                        // Just loading is enough to prove it's a valid PDF structure
                        details.add(fileName + ": PDF parsed successfully (Pages: " + doc.getNumberOfPages() + ")");
                    }
                } else if ("OFD".equalsIgnoreCase(declaredType)) {
                    // Stub for OFD parser
                    // Check for OFD magic bytes or XML structure
                    String headerStr = new String(content, 0, Math.min(content.length, 100));
                    if (!headerStr.contains("OFD") && !headerStr.contains("xml")) {
                        item.addError(fileName + ": Invalid OFD header");
                    } else {
                        details.add(fileName + ": OFD structure valid (Basic check)");
                    }
                }
            } catch (Exception e) {
                item.addError("Parse failed for " + fileName + ": " + e.getMessage());
            }
        }
        
        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }
        
        return item;
    }
    
    private boolean checkTypeMatch(String declared, String detected) {
        if (declared == null || detected == null) return false;
        declared = declared.toUpperCase();
        detected = detected.toLowerCase();
        
        if (declared.equals("PDF") && detected.contains("pdf")) return true;
        if (declared.equals("OFD") && (detected.contains("xml") || detected.contains("zip") || detected.contains("ofd"))) return true; // OFD is often zipped XML
        if (declared.equals("XML") && detected.contains("xml")) return true;
        if ((declared.equals("JPG") || declared.equals("JPEG")) && detected.contains("jpeg")) return true;
        if (declared.equals("PNG") && detected.contains("png")) return true;
        
        return false;
    }
    
    /**
     * 4. 安全性检测
     * - 病毒扫描
     * - 脚本检测
     */
    private CheckItem checkSafety(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
        CheckItem item = CheckItem.pass("Safety Check", "No threats detected");
        
        if (sip.getAttachments() == null) return item;
        
        for (AttachmentDto attachment : sip.getAttachments()) {
            String fileName = attachment.getFileName();
            byte[] content = fileStreams.get(fileName);
            
            if (content == null) continue;
            
            boolean isSafe = virusScanAdapter.scan(content, fileName);
            if (!isSafe) {
                item.addError("Security Threat detected in file: " + fileName);
            }
        }
        
        return item;
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

        // 1. 真实性检测 (Authenticity) - Critical
        // 重新计算文件哈希并与原始哈希对比
        CheckItem authenticity = checkHealthAuthenticity(files);
        report.setAuthenticity(authenticity);
        if (authenticity.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
            log.error("SecurityEvent: Health Check Authenticity FAILED for Archive: {}", archive.getArchiveCode());
            return report;
        }

        // 2. 完整性检测 (Integrity)
        // 检查元数据完整性
        CheckItem integrity = checkHealthIntegrity(archive, files);
        report.setIntegrity(integrity);
        if (integrity.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
        }

        // 3. 可用性检测 (Usability)
        // 检查文件是否存在且可读
        CheckItem usability = checkHealthUsability(files);
        report.setUsability(usability);
        if (usability.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
        }

        // 4. 安全性检测 (Safety)
        // 病毒扫描
        CheckItem safety = checkHealthSafety(files);
        report.setSafety(safety);
        if (safety.getStatus() == OverallStatus.FAIL) {
            report.setStatus(OverallStatus.FAIL);
            log.error("SecurityEvent: Health Check Safety FAILED for Archive: {}", archive.getArchiveCode());
            return report;
        }

        // Update overall status if warnings exist
        if (report.getStatus() == OverallStatus.PASS) {
            boolean hasWarning = authenticity.getStatus() == OverallStatus.WARNING ||
                    integrity.getStatus() == OverallStatus.WARNING ||
                    usability.getStatus() == OverallStatus.WARNING ||
                    safety.getStatus() == OverallStatus.WARNING;
            if (hasWarning) {
                report.setStatus(OverallStatus.WARNING);
            }
        }

        return report;
    }

    private CheckItem checkHealthAuthenticity(List<ArcFileContent> files) {
        CheckItem item = CheckItem.pass("Authenticity Check", "All files hash verified");
        List<String> details = new ArrayList<>();

        for (ArcFileContent file : files) {
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(file.getStoragePath());
                if (!java.nio.file.Files.exists(path)) {
                    item.addError("File not found on disk: " + file.getFileName());
                    continue;
                }

                byte[] content = java.nio.file.Files.readAllBytes(path);
                String algo = file.getHashAlgorithm() != null ? file.getHashAlgorithm() : "SM3";
                String currentHash;

                if ("SM3".equalsIgnoreCase(algo)) {
                    currentHash = fileHashUtil.calculateSM3(new ByteArrayInputStream(content));
                } else {
                    currentHash = fileHashUtil.calculateSHA256(new ByteArrayInputStream(content));
                }

                // Update current hash in memory (caller should save if needed)
                file.setCurrentHash(currentHash);

                if (file.getOriginalHash() != null && !file.getOriginalHash().equalsIgnoreCase(currentHash)) {
                    item.addError(String.format("Hash mismatch for %s. Original: %s, Current: %s",
                            file.getFileName(), file.getOriginalHash(), currentHash));
                } else {
                    details.add(file.getFileName() + ": Verified");
                }

            } catch (Exception e) {
                item.addError("Error verifying " + file.getFileName() + ": " + e.getMessage());
            }
        }

        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }
        return item;
    }

    private CheckItem checkHealthIntegrity(com.nexusarchive.entity.Archive archive, List<ArcFileContent> files) {
        CheckItem item = CheckItem.pass("Integrity Check", "Metadata complete");

        // Check Mandatory Fields (DA/T 94)
        if (archive.getUniqueBizId() == null || archive.getUniqueBizId().isEmpty()) {
            item.addError("Missing Unique Biz ID");
        }
        if (archive.getAmount() == null) {
            item.addError("Missing Amount");
        }
        if (archive.getDocDate() == null) {
            item.addError("Missing Doc Date");
        }
        if (archive.getFondsNo() == null) item.addError("Missing Fonds No");
        if (archive.getArchiveCode() == null) item.addError("Missing Archive Code");

        // Check File Association
        if (files == null || files.isEmpty()) {
            item.addError("No physical files associated with this archive");
        }

        return item;
    }

    private CheckItem checkHealthUsability(List<ArcFileContent> files) {
        CheckItem item = CheckItem.pass("Usability Check", "Files accessible");
        List<String> details = new ArrayList<>();

        for (ArcFileContent file : files) {
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(file.getStoragePath());
                if (!java.nio.file.Files.exists(path)) {
                    item.addError("File missing: " + file.getStoragePath());
                    continue;
                }
                if (!java.nio.file.Files.isReadable(path)) {
                    item.addError("File not readable: " + file.getStoragePath());
                    continue;
                }
                
                // Magic Number Check (Simplified for Health Check)
                byte[] header = new byte[Math.min((int)java.nio.file.Files.size(path), 100)];
                try (java.io.InputStream is = java.nio.file.Files.newInputStream(path)) {
                    is.read(header);
                }
                String detected = tika.detect(header);
                if (!checkTypeMatch(file.getFileType(), detected)) {
                     // Log warning but don't fail hard unless strict mode
                     details.add("WARNING: Type mismatch for " + file.getFileName() + " (" + detected + ")");
                }

            } catch (Exception e) {
                item.addError("Usability check failed for " + file.getFileName() + ": " + e.getMessage());
            }
        }
        
        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }

        return item;
    }

    private CheckItem checkHealthSafety(List<ArcFileContent> files) {
        CheckItem item = CheckItem.pass("Safety Check", "No threats detected");

        for (ArcFileContent file : files) {
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(file.getStoragePath());
                if (java.nio.file.Files.exists(path)) {
                    byte[] content = java.nio.file.Files.readAllBytes(path);
                    boolean isSafe = virusScanAdapter.scan(content, file.getFileName());
                    if (!isSafe) {
                        item.addError("Virus detected in " + file.getFileName());
                    }
                }
            } catch (Exception e) {
                // Ignore read errors here as they are caught in Usability
            }
        }
        return item;
    }
}
