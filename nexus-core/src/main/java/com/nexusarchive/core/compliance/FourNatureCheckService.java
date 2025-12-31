// Input: 归档文件 + 四性检测策略
// Output: 四性检测结果（含签名/完整性/病毒）
// Pos: NexusCore compliance/fournature
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class FourNatureCheckService {
    private final FileHashService fileHashService;
    private final MagicNumberValidator magicNumberValidator;
    private final IntegrityChecker integrityChecker;
    private final DigitalSignatureVerifier digitalSignatureVerifier;
    private final VirusScanService virusScanService;

    public FourNatureCheckService(FileHashService fileHashService,
                                   MagicNumberValidator magicNumberValidator,
                                   IntegrityChecker integrityChecker,
                                   DigitalSignatureVerifier digitalSignatureVerifier,
                                   VirusScanService virusScanService) {
        this.fileHashService = Objects.requireNonNull(fileHashService);
        this.magicNumberValidator = Objects.requireNonNull(magicNumberValidator);
        this.integrityChecker = Objects.requireNonNull(integrityChecker);
        this.digitalSignatureVerifier = Objects.requireNonNull(digitalSignatureVerifier);
        this.virusScanService = Objects.requireNonNull(virusScanService);
    }

    public FourNatureCheckResult check(Path filePath, FourNatureCheckRequest request) throws IOException {
        Objects.requireNonNull(filePath, "filePath must not be null");
        Objects.requireNonNull(request, "request must not be null");

        FourNatureCheckResult.Builder builder = FourNatureCheckResult.builder();

        // 1. 真实性检测 (Authenticity) - 哈希校验
        String computedHash = fileHashService.hashFile(filePath, request.getHashAlgorithm());
        String expectedHash = request.getExpectedHash();
        boolean hashProvided = expectedHash != null && !expectedHash.isBlank();
        boolean hashMatches = hashProvided && computedHash.equalsIgnoreCase(expectedHash);

        StringBuilder authenticityMessage = new StringBuilder();
        if (!hashProvided) {
            authenticityMessage.append("缺少预期哈希值");
        } else if (!hashMatches) {
            authenticityMessage.append("哈希不匹配");
        }

        SignatureVerifyResult signatureResult = digitalSignatureVerifier.verify(filePath);
        boolean signaturePresent = signatureResult != null && signatureResult.signaturePresent();
        boolean signatureValid = signatureResult != null && signatureResult.valid();
        boolean signaturePassed = true;
        if (request.isSignatureRequired()) {
            signaturePassed = signaturePresent && signatureValid;
            if (!signaturePassed) {
                appendSignatureMessage(authenticityMessage, signatureResult, signaturePresent);
            }
        } else if (signaturePresent) {
            signaturePassed = signatureValid;
            if (!signaturePassed) {
                appendSignatureMessage(authenticityMessage, signatureResult, true);
            }
        }

        boolean authenticityPassed = hashMatches && signaturePassed;
        if (authenticityMessage.length() == 0) {
            authenticityMessage.append("通过");
        }
        builder.authenticity(authenticityPassed, computedHash)
                .authenticityDetail(authenticityMessage.toString(), signatureResult);

        // 2. 完整性检测 (Integrity) - 元数据一致性
        IntegrityCheckResult integrityResult = null;
        boolean integrityPassed;
        String integrityMessage;
        if (request.isIntegrityRequired()) {
            Path metadataXmlPath = request.getMetadataXmlPath();
            if (metadataXmlPath == null) {
                integrityPassed = false;
                integrityMessage = "缺少 XML 元数据";
            } else {
                integrityResult = integrityChecker.verify(metadataXmlPath, filePath);
                integrityPassed = integrityResult.passed();
                integrityMessage = integrityPassed ? "一致性校验通过" : summarizeIntegrityDiffs(integrityResult);
            }
        } else {
            integrityPassed = true;
            integrityMessage = "完整性校验跳过";
        }
        builder.integrity(integrityPassed, integrityMessage)
                .integrityDetail(integrityResult);

        // 3. 可用性检测 (Usability) - Magic Number 校验
        MagicNumberValidator.FileType detectedType = magicNumberValidator.detectFileType(filePath);
        String expectedExtension = request.getExpectedExtension();
        boolean usabilityPassed = expectedExtension != null
                && !expectedExtension.isBlank()
                && detectedType.matchesExtension(expectedExtension);
        builder.usability(usabilityPassed, detectedType.name());

        // 4. 安全性检测 (Safety) - 病毒扫描
        VirusScanResult virusScanResult = null;
        boolean safetyPassed;
        String safetyMessage;
        if (request.isVirusScanRequired()) {
            virusScanResult = virusScanService.scan(filePath);
            safetyPassed = virusScanResult.clean();
            safetyMessage = safetyPassed ? "CLEAN" : virusScanResult.virusName();
        } else {
            safetyPassed = true;
            safetyMessage = "病毒扫描跳过";
        }
        builder.safety(safetyPassed, safetyMessage)
                .safetyDetail(virusScanResult);

        return builder.build();
    }

    private String summarizeIntegrityDiffs(IntegrityCheckResult result) {
        if (result == null || result.diffs() == null || result.diffs().isEmpty()) {
            return "一致性校验失败";
        }
        StringBuilder summary = new StringBuilder();
        int limit = Math.min(3, result.diffs().size());
        for (int i = 0; i < limit; i++) {
            IntegrityDiff diff = result.diffs().get(i);
            if (i > 0) {
                summary.append("; ");
            }
            summary.append(diff.fieldName());
        }
        if (result.diffs().size() > limit) {
            summary.append("; ...");
        }
        return "字段不一致: " + summary;
    }

    private void appendSignatureMessage(StringBuilder authenticityMessage,
                                        SignatureVerifyResult signatureResult,
                                        boolean signaturePresent) {
        if (authenticityMessage.length() > 0) {
            authenticityMessage.append("; ");
        }
        if (!signaturePresent) {
            authenticityMessage.append("未发现数字签名");
            return;
        }
        if (signatureResult == null || signatureResult.errorMessage() == null) {
            authenticityMessage.append("签名验证异常");
            return;
        }
        authenticityMessage.append(signatureResult.errorMessage());
    }
}
