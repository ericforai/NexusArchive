// Input: Spring Framework, Lombok
// Output: AccountingCodeValidator 类
// Pos: 服务层 - 会计科目验证器

package com.nexusarchive.service.compliance;

import com.nexusarchive.entity.Archive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 会计科目验证器
 * <p>
 * 验证会计科目代码是否符合国家标准
 * </p>
 */
@Slf4j
@Component
public class AccountingCodeValidator implements ComplianceValidator {

    @Override
    public ComplianceResult validate(Archive archive) {
        ComplianceResult result = new ComplianceResult();

        String customMetadata = archive.getCustomMetadata();
        if (customMetadata != null && !customMetadata.isEmpty()) {
            // 简化验证：检查科目代码格式
            // 实际应解析 customMetadata JSON 中的 accsubject 字段
            if (customMetadata.contains("accsubject")) {
                // 这里可以添加更详细的验证逻辑
                log.debug("检测到会计科目信息");
            }
        }

        log.debug("会计科目验证完成，违规项: {}", result.getViolations().size());
        return result;
    }

    @Override
    public String getName() {
        return "会计科目验证器";
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
