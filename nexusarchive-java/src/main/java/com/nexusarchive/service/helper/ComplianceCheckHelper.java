package com.nexusarchive.service.helper;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.service.ComplianceCheckService.ComplianceResult;
import com.nexusarchive.service.DigitalSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceCheckHelper {

    private final DigitalSignatureService digitalSignatureService;

    public void checkRetention(Archive a, ComplianceResult r) {
        String p = a.getRetentionPeriod();
        String c = a.getCategoryCode();
        if ("AC01".equals(c) || "AC02".equals(c)) {
            if (!"30".equals(p)) r.addViolation("会计凭证/账簿保存期限应至少30年");
        } else if ("AC03".equals(c)) {
            if (!"永久".equals(p)) r.addViolation("财务报告应永久保存");
        }
    }

    public void checkCompleteness(Archive a, List<ArcFileContent> fs, ComplianceResult r) {
        if (fs == null || fs.isEmpty()) r.addViolation("缺少电子文件");
        if (a.getStandardMetadata() == null || a.getStandardMetadata().isEmpty()) r.addViolation("缺少标准元数据");
        if (a.getAmount() == null) r.addViolation("缺少金额信息");
    }

    public void checkSignature(List<ArcFileContent> fs, ComplianceResult r) {
        for (ArcFileContent f : fs) {
            if (f.getSignValue() != null && f.getSignValue().length > 0) {
                try {
                    var res = digitalSignatureService.verifySignature(f);
                    if (!res.isValid()) r.addViolation("文件 " + f.getFileName() + " 签名无效");
                } catch (Exception e) {
                    r.addWarning("无法验证签名: " + f.getFileName());
                }
            }
        }
    }

    public void checkTiming(Archive a, ComplianceResult r) {
        if (a.getDocDate() != null && a.getCreatedTime() != null) {
            LocalDate deadline = LocalDate.of(a.getDocDate().getYear(), 12, 31).plusYears(1);
            if (a.getCreatedTime().toLocalDate().isAfter(deadline)) {
                r.addViolation("归档时间延迟超过1年");
            }
        }
    }

    public boolean isValidCategory(String c) {
        return List.of("AC01", "AC02", "AC03", "AC04", "AC05").contains(c);
    }
}
