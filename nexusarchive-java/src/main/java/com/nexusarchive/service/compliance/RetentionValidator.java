// Input: Spring Framework, Lombok
// Output: RetentionValidator 类
// Pos: 服务层 - 保存期限验证器

package com.nexusarchive.service.compliance;

import com.nexusarchive.entity.Archive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 保存期限验证器
 * <p>
 * 根据《会计档案管理办法》第八条验证保存期限
 * </p>
 */
@Slf4j
@Component
public class RetentionValidator implements ComplianceValidator {

    @Override
    public ComplianceResult validate(Archive archive) {
        ComplianceResult result = new ComplianceResult();
        String retentionPeriod = archive.getRetentionPeriod();
        String categoryCode = archive.getCategoryCode();

        // 会计凭证保存期限至少为30年
        if (com.nexusarchive.common.constants.ArchiveConstants.Categories.VOUCHER.equals(categoryCode)) {
            if (retentionPeriod == null || !com.nexusarchive.common.constants.ArchiveConstants.Retention.Y30.equals(retentionPeriod)) {
                result.addViolation("会计凭证保存期限不符合《会计档案管理办法》第八条要求，应保存至少30年");
            }
        }
        // 会计账簿保存期限至少为30年
        else if (com.nexusarchive.common.constants.ArchiveConstants.Categories.BOOK.equals(categoryCode)) {
            if (retentionPeriod == null || !com.nexusarchive.common.constants.ArchiveConstants.Retention.Y30.equals(retentionPeriod)) {
                result.addViolation("会计账簿保存期限不符合《会计档案管理办法》第八条要求，应保存至少30年");
            }
        }
        // 财务报告保存期限至少为永久
        else if (com.nexusarchive.common.constants.ArchiveConstants.Categories.REPORT.equals(categoryCode)) {
            if (retentionPeriod == null || !com.nexusarchive.common.constants.ArchiveConstants.Retention.PERMANENT.equals(retentionPeriod)) {
                result.addViolation("财务报告保存期限不符合《会计档案管理办法》第八条要求，应永久保存");
            }
        }
        // 其他财务文件保存期限至少为15年
        else if (com.nexusarchive.common.constants.ArchiveConstants.Categories.OTHERS.equals(categoryCode)) {
            if (retentionPeriod == null || (!com.nexusarchive.common.constants.ArchiveConstants.Retention.Y10.equals(retentionPeriod) && !com.nexusarchive.common.constants.ArchiveConstants.Retention.Y30.equals(retentionPeriod))) {
                result.addWarning("其他财务文件建议保存期限至少为10年或30年");
            }
        }

        log.debug("保存期限验证完成，违规项: {}", result.getViolations().size());
        return result;
    }

    @Override
    public String getName() {
        return "保存期限验证器";
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
