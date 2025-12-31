// Input: List<FourNaturesCheck>
// Output: Validation Results
// Pos: NexusCore preservation/impl
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.impl;

import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.preservation.CheckResult;
import com.nexusarchive.core.preservation.FourNaturesCheck;
import com.nexusarchive.core.preservation.FourNaturesService;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultFourNaturesService implements FourNaturesService {

    private final List<FourNaturesCheck> checks;

    @Override
    public List<CheckResult> validate(FileContent fileContent, Path physicalPath) {
        if (checks == null || checks.isEmpty()) {
            return Collections.emptyList();
        }
        return checks.stream()
                .map(check -> safeCheck(check, fileContent, physicalPath))
                .collect(Collectors.toList());
    }

    @Override
    public CheckResult validateSpecific(FileContent fileContent, Path physicalPath, String checkName) {
        return checks.stream()
                .filter(c -> c.getName().equalsIgnoreCase(checkName))
                .findFirst()
                .map(check -> safeCheck(check, fileContent, physicalPath))
                .orElse(CheckResult.fail(checkName, "检测项未找到: " + checkName, null));
    }

    private CheckResult safeCheck(FourNaturesCheck check, FileContent fileContent, Path physicalPath) {
        try {
            return check.check(fileContent, physicalPath);
        } catch (Exception e) {
            log.error("Check execution failed: {}", check.getName(), e);
            return CheckResult.fail(check.getName(), "检测执行异常", e.getMessage());
        }
    }
}
