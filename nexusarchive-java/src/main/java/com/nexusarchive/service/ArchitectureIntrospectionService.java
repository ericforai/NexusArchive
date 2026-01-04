// Input: 架构自省服务
// Output: J4 Reflex - 运行时架构可见性
// Pos: 服务层 - 架构防御

package com.nexusarchive.service;

import com.nexusarchive.annotation.architecture.ModuleManifest;
import com.nexusarchive.dto.response.ArchitectureReportDto;
import com.nexusarchive.dto.response.ModuleInfoDto;
import com.nexusarchive.dto.response.ViolationReportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 架构自省服务
 * <p>
 * 实现 Architecture Defense 的 J4 (Reflex) 原则：
 * 运行时架构可见性，允许系统自省和报告架构健康状态。
 * </p>
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>扫描和读取所有 @ModuleManifest 注解</li>
 *   <li>验证模块依赖规则</li>
 *   <li>生成架构健康报告</li>
 *   <li>检测架构违规</li>
 * </ul>
 */
@Service
public class ArchitectureIntrospectionService {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureIntrospectionService.class);

    private static final String BASE_PACKAGE = "com.nexusarchive";

    /**
     * 获取所有模块清单
     */
    public ArchitectureReportDto getModuleManifests() {
        ArchitectureReportDto report = new ArchitectureReportDto();
        report.setTimestamp(new Date());
        report.setModules(new ArrayList<>());

        try {
            // 扫描所有 package-info.java 文件
            String packagePath = BASE_PACKAGE.replace('.', '/');
            Enumeration<URL> resources = getClass().getClassLoader()
                    .getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());

                if (directory.exists() && directory.isDirectory()) {
                    scanPackages(directory, BASE_PACKAGE, report);
                }
            }

        } catch (IOException e) {
            log.error("Failed to scan packages", e);
            report.setError("Failed to scan packages: " + e.getMessage());
        }

        return report;
    }

    /**
     * 扫描包目录
     */
    private void scanPackages(File directory, String packageName, ArchitectureReportDto report) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanPackages(file, packageName + "." + file.getName(), report);
            } else if (file.getName().equals("package-info.java")) {
                // 读取 package-info.java 中的 @ModuleManifest
                ModuleInfoDto moduleInfo = readModuleManifest(packageName);
                if (moduleInfo != null) {
                    report.getModules().add(moduleInfo);
                }
            }
        }
    }

    /**
     * 读取模块清单注解
     */
    private ModuleInfoDto readModuleManifest(String packageName) {
        try {
            Package pkg = Package.getPackage(packageName);
            if (pkg == null) {
                return null;
            }

            // 尝试获取注解
            Annotation[] annotations = pkg.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof ModuleManifest) {
                    return createModuleInfo(packageName, (ModuleManifest) annotation);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to read manifest for package: {}", packageName, e);
        }
        return null;
    }

    /**
     * 创建模块信息 DTO
     */
    private ModuleInfoDto createModuleInfo(String packageName, ModuleManifest manifest) {
        ModuleInfoDto info = new ModuleInfoDto();
        info.setPackage(packageName);
        info.setId(manifest.id());
        info.setName(manifest.name());
        info.setOwner(manifest.owner());
        info.setLayer(manifest.layer().name());
        info.setDescription(manifest.description());
        info.setTags(Arrays.asList(manifest.tags()));
        info.setLegacy(manifest.legacy());
        info.setComplianceTarget(manifest.complianceTarget());
        info.setExceptionReason(manifest.exceptionReason());
        info.setReviewDate(manifest.reviewDate());

        // 解析依赖规则
        List<String> dependencies = Arrays.stream(manifest.dependencies())
                .flatMap(rule -> Arrays.stream(rule.allowedPackages()))
                .collect(Collectors.toList());
        info.setDeclaredDependencies(dependencies);

        return info;
    }

    /**
     * 验证模块依赖规则
     * <p>
     * 注意：完整的依赖验证需要运行 ArchUnit 测试。
     * 此方法仅检查模块清单的完整性。
     * </p>
     */
    public ViolationReportDto validateModuleDependencies() {
        ViolationReportDto report = new ViolationReportDto();
        report.setTimestamp(new Date());
        report.setViolations(new ArrayList<>());

        // 获取所有模块清单
        ArchitectureReportDto manifests = getModuleManifests();

        // 检查是否有模块缺少清单
        List<String> requiredPackages = Arrays.asList("controller", "service", "mapper", "entity");
        Set<String> foundPackages = manifests.getModules().stream()
                .map(m -> m.getPackage().substring(m.getPackage().lastIndexOf('.') + 1))
                .collect(Collectors.toSet());

        for (String required : requiredPackages) {
            if (!foundPackages.contains(required)) {
                ViolationReportDto.ViolationDto violation = new ViolationReportDto.ViolationDto();
                violation.setType("missing-manifest");
                violation.setSeverity("warn");
                violation.setDescription("缺少模块清单: " + required);
                violation.setSuggestion("创建 " + required + "/package-info.java 并添加 @ModuleManifest 注解");
                report.getViolations().add(violation);
            }
        }

        return report;
    }

    /**
     * 运行所有架构测试并返回结果
     * <p>
     * 注意：完整的架构验证需要运行 Maven 测试。
     * 此方法返回测试状态信息。
     * </p>
     */
    public Map<String, Object> runArchitectureTests() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> tests = new ArrayList<>();

        // 返回测试状态信息（实际测试需要运行 Maven）
        tests.add(createTestInfo(
                "分层架构约束",
                "skipped",
                "运行 'mvn test -Dtest=ArchitectureTest' 进行验证"
        ));

        tests.add(createTestInfo(
                "增强架构规则",
                "skipped",
                "运行 'mvn test -Dtest=EnhancedArchitectureTest' 进行验证"
        ));

        result.put("timestamp", new Date());
        result.put("totalTests", tests.size());
        result.put("message", "请运行 Maven 测试进行完整的架构验证");
        result.put("tests", tests);

        return result;
    }

    /**
     * 创建测试信息
     */
    private Map<String, Object> createTestInfo(String name, String status, String message) {
        Map<String, Object> test = new HashMap<>();
        test.put("name", name);
        test.put("status", status);
        test.put("message", message);
        return test;
    }
}
