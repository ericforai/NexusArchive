// Input: FileContent, physicalPath
// Output: Authenticity Check Result
// Pos: NexusCore preservation/impl
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.impl;

import com.nexusarchive.core.compliance.DigitalSignatureVerifier;
import com.nexusarchive.core.compliance.SignatureVerifyResult;
import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.preservation.CheckResult;
import com.nexusarchive.core.preservation.FourNaturesCheck;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticityCheck implements FourNaturesCheck {

    private final DigitalSignatureVerifier verifier;

    @Override
    public CheckResult check(FileContent fileContent, Path physicalPath) {
        String fileType = fileContent.getFileType();
        if ("XML".equalsIgnoreCase(fileType) || "JPG".equalsIgnoreCase(fileType) || "PNG".equalsIgnoreCase(fileType)) {
             // Non-signable formats for now (unless XML Signature implemented)
             // Allow pass for now as "Not Applicable" or just Pass?
             return CheckResult.pass(getName(), "真实性校验跳过 (非签名格式: " + fileType + ")");
        }

        try {
            SignatureVerifyResult result = verifier.verify(physicalPath);
            if (result.valid()) {
                return CheckResult.pass(getName(), "真实性校验通过: " + result.algorithm());
            } else {
                String msg = result.errorMessage() != null ? result.errorMessage() : "校验失败";
                if (result.timestampMessage() != null) {
                    msg += " (" + result.timestampMessage() + ")";
                }
                return CheckResult.fail(getName(), "真实性校验失败: " + msg, result.toString());
            }
        } catch (Exception e) {
            log.error("Authenticity check error", e);
            return CheckResult.fail(getName(), "真实性校验异常", e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "AUTHENTICITY";
    }
}
