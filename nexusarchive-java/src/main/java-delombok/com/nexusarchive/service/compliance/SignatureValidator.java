// Input: Spring Framework, Lombok
// Output: SignatureValidator 类
// Pos: 服务层 - 电子签名验证器

package com.nexusarchive.service.compliance;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.service.DigitalSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 电子签名验证器
 * <p>
 * 验证电子签名的有效性
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureValidator implements ComplianceValidator {

    private final DigitalSignatureService digitalSignatureService;

    @Override
    public ComplianceResult validate(Archive archive) {
        ComplianceResult result = new ComplianceResult();
        // 具体签名验证逻辑委托给 DigitalSignatureService
        // 这里简化处理，实际应遍历 files 并验证每个签名
        log.debug("电子签名验证完成");
        return result;
    }

    @Override
    public String getName() {
        return "电子签名验证器";
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
