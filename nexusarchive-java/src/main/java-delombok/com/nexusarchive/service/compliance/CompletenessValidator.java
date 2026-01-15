// Input: Spring Framework, Lombok
// Output: CompletenessValidator 类
// Pos: 服务层 - 完整性验证器

package com.nexusarchive.service.compliance;

import com.nexusarchive.entity.Archive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 完整性验证器
 * <p>
 * 根据《会计档案管理办法》第六条验证档案完整性
 * </p>
 */
@Slf4j
@Component
public class CompletenessValidator implements ComplianceValidator {

    @Override
    public ComplianceResult validate(Archive archive) {
        ComplianceResult result = new ComplianceResult();

        // 检查元数据完整性
        if (archive.getStandardMetadata() == null || archive.getStandardMetadata().isEmpty()) {
            result.addViolation("档案缺少标准元数据，不符合《会计档案管理办法》第六条要求");
        }

        // 检查关键字段
        if (archive.getUniqueBizId() == null || archive.getUniqueBizId().isEmpty()) {
            result.addViolation("档案缺少唯一业务标识，不符合档案管理要求");
        }

        if (archive.getAmount() == null) {
            result.addViolation("档案缺少金额信息，不符合会计档案要求");
        }

        if (archive.getDocDate() == null) {
            result.addViolation("档案缺少业务日期，不符合档案管理要求");
        }

        log.debug("完整性验证完成，违规项: {}", result.getViolations().size());
        return result;
    }

    @Override
    public String getName() {
        return "完整性验证器";
    }

    @Override
    public int getPriority() {
        return 2;
    }
}
