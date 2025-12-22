// Input: Lombok、Apache、Spring Framework、Java 标准库、等
// Output: FourNatureCoreServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
import java.io.InputStream;
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
    public CheckItem checkSingleFileAuthenticity(InputStream inputStream, String fileName, String expectedHash, String hashAlgo, String fileType) {
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
                    calculatedHash = fileHashUtil.calculateSM3(inputStream);
                } else if ("SHA-256".equalsIgnoreCase(algo) || "SHA256".equalsIgnoreCase(algo)) {
                    calculatedHash = fileHashUtil.calculateSHA256(inputStream);
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

        // 注意: 签章校验在 Ingress 阶段通常针对完整文件流，这里假设 inputStream 如果是不可重复读的，则上层需处理
        // 实际上 FourNatureCheckServiceImpl 中会使用 BufferedInputStream 或先存临时文件
        
        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }
        return item;
    }

    @Override
    public CheckItem checkSingleFileUsability(InputStream inputStream, String fileName, String declaredType) {
        CheckItem item = CheckItem.pass("Usability Check", "File format valid");
        List<String> details = new ArrayList<>();

        // 1. Tika Magic Number Check
        try {
            // Tika detect can take InputStream. It will read ahead and attempt to reset if supported.
            String detectedType = tika.detect(inputStream);
            boolean typeMatch = checkTypeMatch(declaredType, detectedType);
            
            if (!typeMatch) {
                item.setStatus(OverallStatus.WARNING);
                details.add(String.format("WARNING: File type mismatch. Declared: %s, Detected: %s", 
                        declaredType, detectedType));
            } else {
                details.add("Format detected: " + detectedType);
            }

            // [IMPROVED] Parser Test for PDF
            if ("PDF".equalsIgnoreCase(declaredType)) {
                try (PDDocument doc = PDDocument.load(inputStream)) {
                    details.add("PDF Structure Valid (Pages: " + doc.getNumberOfPages() + ")");
                } catch (Exception e) {
                    item.addError("PDF Parser error: " + e.getMessage());
                }
            }
            // OFD stub remains as stream detection is complex without full SDK
        } catch (Exception e) {
            item.addError("Usability check failed: " + e.getMessage());
        }

        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }
        return item;
    }

    @Override
    public CheckItem checkSingleFileSafety(InputStream inputStream, String fileName) {
        CheckItem item = CheckItem.pass("Safety Check", "No threats detected");
        try {
            boolean isSafe = virusScanAdapter.scan(inputStream, fileName);
            if (!isSafe) {
                item.addError("Security Threat detected in file: " + fileName);
            }
        } catch (Exception e) {
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
