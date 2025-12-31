// Input: Spring Framework, Lombok
// Output: TimingValidator 类
// Pos: 服务层 - 归档时间验证器

package com.nexusarchive.service.compliance;

import com.nexusarchive.entity.Archive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 归档时间验证器
 * <p>
 * 验证归档时间是否符合规定（通常要求在会计年度结束后一定时间内完成归档）
 * </p>
 */
@Slf4j
@Component
public class TimingValidator implements ComplianceValidator {

    @Override
    public ComplianceResult validate(Archive archive) {
        ComplianceResult result = new ComplianceResult();

        // 检查归档时间是否在业务日期后的合理范围内
        if (archive.getDocDate() != null && archive.getCreatedTime() != null) {
            long monthsBetween = ChronoUnit.MONTHS.between(
                archive.getDocDate(),
                archive.getCreatedTime().toLocalDate()
            );

            if (monthsBetween > 12) {
                result.addWarning("归档时间距离业务日期超过12个月，建议及时归档");
            }
        }

        log.debug("归档时间验证完成，警告项: {}", result.getWarnings().size());
        return result;
    }

    @Override
    public String getName() {
        return "归档时间验证器";
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
