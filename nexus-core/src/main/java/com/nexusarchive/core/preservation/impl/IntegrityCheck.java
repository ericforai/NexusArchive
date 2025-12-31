// Input: FileContent, physicalPath
// Output: Integrity Check Result
// Pos: NexusCore preservation/impl
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.impl;

import com.nexusarchive.core.compliance.FileHashService;
import com.nexusarchive.core.compliance.HashAlgorithm;
import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.preservation.CheckResult;
import com.nexusarchive.core.preservation.FourNaturesCheck;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrityCheck implements FourNaturesCheck {

    private final FileHashService fileHashService;

    @Override
    public CheckResult check(FileContent fileContent, Path physicalPath) {
        String expectedHash = fileContent.getFileHash();
        String algoStr = fileContent.getHashAlgorithm();

        if (!StringUtils.hasText(expectedHash)) {
            // If no expected hash, maybe we should calculate and return PASS(New Hash) or FAIL?
            // For preservation check of EXISTING file, it MUST have a hash.
            // For new file ingestion, this check might be run after hash calculation.
            return CheckResult.fail(getName(), "完整性校验失败: 预期哈希值缺失", "Metadata missing fixity_value");
        }
        
        try {
             HashAlgorithm algorithm = HashAlgorithm.SM3; // Default
             if ("SHA-256".equalsIgnoreCase(algoStr) || "SHA256".equalsIgnoreCase(algoStr)) {
                 algorithm = HashAlgorithm.SHA256;
             }

             String calculatedHash = fileHashService.hashFile(physicalPath, algorithm);

             if (expectedHash.equalsIgnoreCase(calculatedHash)) {
                 return CheckResult.pass(getName(), "完整性校验通过 (" + algorithm + ")");
             } else {
                 return CheckResult.fail(getName(), "完整性校验失败: 哈希不匹配", 
                         String.format("Expected: %s, Actual: %s", expectedHash, calculatedHash));
             }
        } catch (Exception e) {
            log.error("Integrity check error", e);
            return CheckResult.fail(getName(), "完整性校验异常", e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "INTEGRITY";
    }
}
