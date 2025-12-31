// Input: Spring Framework, Lombok
// Output: ComplianceCheckFacade 类
// Pos: 服务层 - 合规检查门面

package com.nexusarchive.service.compliance;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合规检查门面服务
 * <p>
 * 协调各个验证器，执行完整的合规性检查流程
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceCheckFacade {

    private final List<ComplianceValidator> validators;

    /**
     * 执行完整的合规性检查
     *
     * @param archive 待检查档案
     * @param files   关联文件列表
     * @return 检查结果
     */
    public ComplianceResult checkCompliance(Archive archive, List<ArcFileContent> files) {
        log.info("开始检查档案 {} 的合规性", archive.getArchiveCode());

        ComplianceResult result = new ComplianceResult();

        // 按优先级顺序执行验证
        validators.stream()
            .sorted((v1, v2) -> Integer.compare(v1.getPriority(), v2.getPriority()))
            .forEach(validator -> {
                log.debug("执行验证器: {}", validator.getName());
                ComplianceResult validatorResult = validator.validate(archive);
                result.merge(validatorResult);
            });

        log.info("档案 {} 合规性检查完成，违规项: {}, 警告项: {}",
            archive.getArchiveCode(), result.getViolations().size(), result.getWarnings().size());

        return result;
    }

    /**
     * 快速检查（仅检查关键项）
     */
    public ComplianceResult quickCheck(Archive archive) {
        log.info("快速检查档案 {}", archive.getArchiveCode());

        ComplianceResult result = new ComplianceResult();

        // 仅执行优先级最高的3个验证器
        validators.stream()
            .sorted((v1, v2) -> Integer.compare(v1.getPriority(), v2.getPriority()))
            .limit(3)
            .forEach(validator -> {
                ComplianceResult validatorResult = validator.validate(archive);
                result.merge(validatorResult);
            });

        return result;
    }
}
