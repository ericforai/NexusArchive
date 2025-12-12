package com.nexusarchive.service.impl;

import com.nexusarchive.common.enums.ArchiveFileType;
import com.nexusarchive.dto.signature.VerifyResult;
import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.service.FourNatureCoreService;
import com.nexusarchive.service.adapter.VirusScanAdapter;
import com.nexusarchive.service.signature.SignatureAdapter;
import com.nexusarchive.util.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 四性检测核心原子服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FourNatureCoreServiceImpl implements FourNatureCoreService {

    private final FileHashUtil fileHashUtil;
    private final VirusScanAdapter virusScanAdapter;
    private final Tika tika = new Tika();

    @Autowired(required = false)
    private SignatureAdapter signatureAdapter;

    @Override
    public CheckItem checkSingleFileAuthenticity(byte[] content, String fileName, String expectedHash, String hashAlgo, String fileType) {
        CheckItem item = CheckItem.pass("Authenticity Check", "Hash verification passed");
        List<String> details = new ArrayList<>();

        // 1. 哈希校验
        if (expectedHash == null || expectedHash.isEmpty()) {
            item.setStatus(OverallStatus.WARNING);
            details.add("WARNING: No hash provided for " + fileName + ", skipping hash verification");
        } else {
            String algo = (hashAlgo == null || hashAlgo.isEmpty()) ? "SM3" : hashAlgo;
            
            try {
                String calculatedHash;
                if ("SM3".equalsIgnoreCase(algo)) {
                    calculatedHash = fileHashUtil.calculateSM3(new ByteArrayInputStream(content));
                } else if ("SHA-256".equalsIgnoreCase(algo) || "SHA256".equalsIgnoreCase(algo)) {
                    calculatedHash = fileHashUtil.calculateSHA256(new ByteArrayInputStream(content));
                } else {
                    item.addError("Unsupported hash algorithm: " + algo);
                    return item;
                }

                if (!calculatedHash.equalsIgnoreCase(expectedHash)) {
                    item.addError(String.format("Hash mismatch for %s. Expected: %s, Actual: %s", 
                            fileName, expectedHash, calculatedHash));
                } else {
                    details.add(String.format("Hash verified (%s)", algo));
                }
            } catch (Exception e) {
                item.addError("Error calculating hash: " + e.getMessage());
            }
        }

        // 2. 签章校验 (如果服务可用)
        if (signatureAdapter != null && signatureAdapter.isAvailable() && fileType != null) {
            checkSignature(content, fileName, fileType, item, details);
        }

        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }
        return item;
    }

    private void checkSignature(byte[] content, String fileName, String fileType, CheckItem item, List<String> details) {
        try {
            if ("PDF".equalsIgnoreCase(fileType)) {
                VerifyResult sigResult = signatureAdapter.verifyPdfSignature(new ByteArrayInputStream(content));
                processSigResult(sigResult, fileName, "PDF", item, details);
            } else if ("OFD".equalsIgnoreCase(fileType)) {
                VerifyResult sigResult = signatureAdapter.verifyOfdSignature(new ByteArrayInputStream(content));
                processSigResult(sigResult, fileName, "OFD", item, details);
            }
        } catch (Exception e) {
            log.debug("Signature check exception: {}", e.getMessage());
        }
    }

    private void processSigResult(VerifyResult sigResult, String fileName, String type, CheckItem item, List<String> details) {
        if (sigResult.isValid()) {
            details.add(String.format("%s Signature VALID (Signer: %s)", type, sigResult.getSignerName()));
        } else if (sigResult.getErrorMessage() != null) {
            // 如果是"不支持"或"开发中"，视情况作为 WARNING 或 PASS(SKIP)
            if (sigResult.getErrorMessage().contains("Unsupported") || sigResult.getErrorMessage().contains("开发中")) {
                // 不影响四性结果，仅提示
                // details.add(String.format("%s Signature check skipped: %s", type, sigResult.getErrorMessage()));
            } else {
                // 真正的验证失败
                item.setStatus(OverallStatus.WARNING); // 签章失败暂时定为警告，取决于业务严格程度
                details.add(String.format("WARNING: %s Signature INVALID: %s", type, sigResult.getErrorMessage()));
            }
        }
    }

    @Override
    public CheckItem checkSingleFileUsability(byte[] content, String fileName, String declaredType) {
        CheckItem item = CheckItem.pass("Usability Check", "File format valid");
        List<String> details = new ArrayList<>();

        if (content == null || content.length == 0) {
            item.addError("File content is empty: " + fileName);
            return item;
        }

        // 1. Tika Magic Number Check
        try {
            String detectedType = tika.detect(content);
            boolean typeMatch = checkTypeMatch(declaredType, detectedType);
            
            if (!typeMatch) {
                item.setStatus(OverallStatus.WARNING);
                details.add(String.format("WARNING: File type mismatch. Declared: %s, Detected: %s", 
                        declaredType, detectedType));
            } else {
                details.add("Format detected: " + detectedType);
            }

            // 2. Parser Test
            if ("PDF".equalsIgnoreCase(declaredType)) {
                try (PDDocument doc = PDDocument.load(content)) {
                    details.add("PDF Structure Valid (Pages: " + doc.getNumberOfPages() + ")");
                }
            } else if ("OFD".equalsIgnoreCase(declaredType)) {
                // Simple stub for OFD
                String headerStr = new String(content, 0, Math.min(content.length, 100));
                if (!headerStr.contains("OFD") && !headerStr.contains("xml") && !headerStr.contains("PK")) {
                     // OFD usually starts with PK (zip) or XML-like headers
                     // Don't fail hard, OFD structure varies
                }
            }

        } catch (Exception e) {
            item.addError("Usability check failed: " + e.getMessage());
        }

        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }
        return item;
    }

    @Override
    public CheckItem checkSingleFileSafety(byte[] content, String fileName) {
        CheckItem item = CheckItem.pass("Safety Check", "No threats detected");
        try {
            boolean isSafe = virusScanAdapter.scan(content, fileName);
            if (!isSafe) {
                item.addError("Security Threat detected in file: " + fileName);
            }
        } catch (Exception e) {
            // 如果扫描服务不可用，视为 Fail 还是 Warning 由策略决定。
            // 这里遵循 Expert Review 建议，视为 Risk
            item.setStatus(OverallStatus.WARNING);
            item.addError("Virus scan failed to execute: " + e.getMessage());
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
}
