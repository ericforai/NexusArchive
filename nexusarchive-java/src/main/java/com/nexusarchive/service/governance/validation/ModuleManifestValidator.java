// Input: NIO Files, ModuleDiscoveryService
// Output: ModuleManifestValidator
// Pos: Service Layer - Governance
// 负责验证模块清单

package com.nexusarchive.service.governance.validation;

import com.nexusarchive.service.governance.ModuleDiscoveryResult;
import com.nexusarchive.service.governance.ModuleValidationResult;
import com.nexusarchive.service.governance.discovery.ModuleDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模块清单验证器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>验证 module-manifest.md 是否存在</li>
 *   <li>检查清单内容完整性</li>
 *   <li>发现未记录的模块</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModuleManifestValidator {

    private static final String DOCS_PATH = "docs/architecture";
    private static final String FRONTEND_MODULE_MARKER = "FE.SYS";
    private static final String BACKEND_MODULE_MARKER = "BE.BORROWING";

    private final ModuleDiscoveryService discoveryService;

    /**
     * 验证模块清单与代码的一致性
     *
     * @param basePath 基础路径
     * @return 验证结果
     */
    public ModuleValidationResult validate(String basePath) {
        ModuleValidationResult result = new ModuleValidationResult();
        result.setValidationTime(System.currentTimeMillis());
        List<String> issues = new ArrayList<>();

        // 检查前端模块清单
        Path manifestPath = Paths.get(DOCS_PATH, "module-manifest.md");
        if (!Files.exists(manifestPath)) {
            issues.add("模块清单文件不存在: " + manifestPath);
        } else {
            try {
                String content = Files.readString(manifestPath);
                // 简单验证：检查是否包含关键模块
                if (!content.contains(FRONTEND_MODULE_MARKER)) {
                    issues.add("前端模块清单缺少 " + FRONTEND_MODULE_MARKER);
                }
                if (!content.contains(BACKEND_MODULE_MARKER)) {
                    issues.add("后端模块清单缺少 " + BACKEND_MODULE_MARKER);
                }
            } catch (Exception e) {
                issues.add("无法读取模块清单: " + e.getMessage());
            }
        }

        // 检查实际模块
        List<ModuleDiscoveryResult> discoveredModules = discoveryService.discoverBackendModules(basePath);
        List<ModuleDiscoveryResult> actualNewModules = discoveredModules.stream()
            .filter(m -> Boolean.TRUE.equals(m.getIsNewModule()))
            .toList();

        if (!actualNewModules.isEmpty()) {
            issues.add("发现未在清单中记录的模块: " +
                actualNewModules.stream()
                    .map(ModuleDiscoveryResult::getModuleName)
                    .collect(Collectors.joining(", ")));
        }

        result.setValid(issues.isEmpty());
        result.setIssues(issues);
        result.setDiscoveredModules(discoveredModules);

        return result;
    }
}
