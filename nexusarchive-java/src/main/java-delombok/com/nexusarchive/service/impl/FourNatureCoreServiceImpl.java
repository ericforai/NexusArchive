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
    public CheckItem checkSingleFileAuthenticity(InputStream inputStream, String fileName, String expectedHash,
            String hashAlgo, String fileType) {
        CheckItem item = CheckItem.pass("Authenticity Check", "Hash verification passed");
        List<String> details = new ArrayList<>();

        // 缓存流内容以支持多次读取（哈希校验 + 签章校验）
        byte[] content;
        try {
            content = readInputStream(inputStream);
        } catch (Exception e) {
            item.addError("无法读取文件内容: " + e.getMessage());
            return item;
        }

        // 1. 哈希校验
        if (expectedHash == null || expectedHash.isEmpty()) {
            item.setStatus(OverallStatus.WARNING);
            details.add("WARNING: No hash provided for " + fileName + ", skipping hash verification");
        } else {
            String algo = (hashAlgo == null || hashAlgo.isEmpty()) ? "SM3" : hashAlgo;

            try (ByteArrayInputStream hashStream = new ByteArrayInputStream(content)) {
                String calculatedHash;
                if ("SM3".equalsIgnoreCase(algo)) {
                    calculatedHash = fileHashUtil.calculateSM3(hashStream);
                } else if ("SHA-256".equalsIgnoreCase(algo) || "SHA256".equalsIgnoreCase(algo)) {
                    calculatedHash = fileHashUtil.calculateSHA256(hashStream);
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

        // 2. 签章校验 (PDF/OFD 版式文件)
        if (isVersionedDocumentType(fileType)) {
            CheckItem signCheck = checkFileSignature(content, fileName, fileType);
            mergeSignatureResult(item, signCheck, details);
        }

        if (!details.isEmpty()) {
            item.setMessage(String.join("; ", details));
        }
        return item;
    }

    /**
     * 检查文件签章
     */
    private CheckItem checkFileSignature(byte[] content, String fileName, String fileType) {
        if (signatureAdapter == null || !signatureAdapter.isAvailable()) {
            log.info("签章服务不可用，跳过签章校验: {}", fileName);
            CheckItem item = CheckItem.pass("Signature Check", "签章服务不可用，跳过校验");
            item.setStatus(OverallStatus.WARNING);
            return item;
        }

        try (ByteArrayInputStream signStream = new ByteArrayInputStream(content)) {
            VerifyResult result;
            if ("PDF".equalsIgnoreCase(fileType)) {
                result = signatureAdapter.verifyPdfSignature(signStream);
            } else if ("OFD".equalsIgnoreCase(fileType)) {
                result = signatureAdapter.verifyOfdSignature(signStream);
            } else {
                return CheckItem.pass("Signature Check", "非版式文件，无需签章校验");
            }

            return convertVerifyResultToCheckItem(result, fileType);
        } catch (Exception e) {
            log.error("签章校验异常: {}", e.getMessage(), e);
            CheckItem item = CheckItem.pass("Signature Check", "签章校验异常: " + e.getMessage());
            item.setStatus(OverallStatus.WARNING);
            return item;
        }
    }

    /**
     * 将签章验证结果转换为 CheckItem
     */
    private CheckItem convertVerifyResultToCheckItem(VerifyResult result, String fileType) {
        if (result.isValid()) {
            String message = String.format("%s签章校验通过", fileType);
            if (result.getSignerName() != null) {
                message += " (签章人: " + result.getSignerName() + ")";
            }
            return CheckItem.pass("Signature Check", message);
        } else if (result.getErrorMessage() != null && result.getErrorMessage().contains("未检测到")) {
            // 无签章不视为失败，视为 WARNING
            CheckItem item = CheckItem.pass("Signature Check", result.getErrorMessage());
            item.setStatus(OverallStatus.WARNING);
            return item;
        } else {
            CheckItem item = CheckItem.pass("Signature Check", "签章校验失败");
            item.addError(result.getErrorMessage() != null ? result.getErrorMessage() : "签章无效");
            return item;
        }
    }

    /**
     * 合并签章校验结果到主检测项
     */
    private void mergeSignatureResult(CheckItem target, CheckItem signResult, List<String> details) {
        if (signResult.getStatus() == OverallStatus.FAIL) {
            target.setStatus(OverallStatus.FAIL);
            if (signResult.getErrors() != null) {
                signResult.getErrors().forEach(target::addError);
            }
        } else if (signResult.getStatus() == OverallStatus.WARNING) {
            if (target.getStatus() != OverallStatus.FAIL) {
                target.setStatus(OverallStatus.WARNING);
            }
            details.add(signResult.getMessage());
        } else {
            details.add(signResult.getMessage());
        }
    }

    /**
     * 判断是否为版式文件（需签章校验）
     */
    private boolean isVersionedDocumentType(String fileType) {
        return "PDF".equalsIgnoreCase(fileType) || "OFD".equalsIgnoreCase(fileType);
    }

    /**
     * 读取输入流内容
     */
    private byte[] readInputStream(InputStream is) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    @Override
    public CheckItem checkSingleFileUsability(InputStream inputStream, String fileName, String declaredType) {
        CheckItem item = CheckItem.pass("Usability Check", "File format valid");
        List<String> details = new ArrayList<>();

        // 1. Tika Magic Number Check
        try {
            // Tika detect can take InputStream. It will read ahead and attempt to reset if
            // supported.
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
        if (declared == null || detected == null)
            return false;
        declared = declared.toUpperCase();
        detected = detected.toLowerCase();

        if (declared.equals("PDF") && detected.contains("pdf"))
            return true;
        if (declared.equals("OFD")
                && (detected.contains("xml") || detected.contains("zip") || detected.contains("ofd")))
            return true;
        if (declared.equals("XML") && detected.contains("xml"))
            return true;
        if ((declared.equals("JPG") || declared.equals("JPEG")) && detected.contains("jpeg"))
            return true;
        if (declared.equals("PNG") && detected.contains("png"))
            return true;

        return false;
    }
}
