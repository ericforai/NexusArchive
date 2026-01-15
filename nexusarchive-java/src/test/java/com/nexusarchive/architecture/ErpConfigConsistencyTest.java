// Input: ArchUnit 测试框架、JUnit 5
// Output: ErpConfigConsistencyTest 架构测试类
// Pos: 架构测试 - ErpConfig 实体完整性验证
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ErpConfig 实体完整性架构测试
 *
 * <p>确保 ErpConfig 实体与 configJson 字段保持同步：</p>
 * <ul>
 *   <li>configJson 中的每个字段都必须有对应的 getter 方法</li>
 *   <li>添加新字段时必须同时添加 getter 方法，否则测试失败</li>
 *   <li>防止客户端代码调用不存在的方法导致运行时错误</li>
 * </ul>
 *
 * <p><strong>背景：</strong></p>
 * <pre>
 * ErpConfig 实体将配置存储在 configJson JSON 字段中，
 * 客户端代码通过 getter 方法（如 getAppKey()、getAppSecret()）访问这些字段。
 * 如果 getter 方法缺失，会导致编译错误或运行时 NullPointerException。
 * </pre>
 *
 * <p><strong>规则：</strong></p>
 * <pre>
 * configJson 中声明的字段 → 必须有对应的 getter 方法
 * 例如：configJson 包含 {"appKey": "xxx"} → 必须有 getAppKey() 方法
 * </pre>
 */
@Tag("architecture")
public class ErpConfigConsistencyTest {

    /**
     * 定义必须在 configJson 中有 getter 的字段
     *
     * <p>这些字段是 ERP 连接器正常运行所必需的。</p>
     * <p>添加新字段时，必须同时更新此列表。</p>
     */
    private static final Set<String> REQUIRED_CONFIG_FIELDS = Set.of(
            "baseUrl",      // API 基础 URL
            "appKey",       // 应用密钥
            "appSecret",    // 应用密钥（敏感）
            "clientSecret", // 客户端密钥（敏感）
            "accbookCode",  // 账套编码
            "extraConfig"   // 额外配置
    );

    /**
     * 获取所有需要分析的类
     */
    private List<JavaClass> getAllClasses() {
        return new ArrayList<>(
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages("com.nexusarchive")
        );
    }

    /**
     * 规则 1：ErpConfig 必须为所有 configJson 字段提供 getter 方法
     *
     * <p>这确保了当适配器或客户端代码调用 config.getXxx() 时，
     * 方法存在并能正确返回值。</p>
     */
    @Test
    @DisplayName("ErpConfig must provide getter methods for all configJson fields")
    void erpConfigMustProvideGettersForAllFields() {
        List<JavaClass> allClasses = getAllClasses();
        JavaClass erpConfigClass = findClass(allClasses, "ErpConfig");

        if (erpConfigClass == null) {
            throw new AssertionError("ErpConfig class not found");
        }

        List<String> missingGetters = new ArrayList<>();
        String sourceCode = getSourceCode(erpConfigClass);

        for (String field : REQUIRED_CONFIG_FIELDS) {
            String expectedMethodName = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);

            if (!hasMethod(sourceCode, expectedMethodName)) {
                missingGetters.add(field + " → 需要 " + expectedMethodName + "()");
            }
        }

        if (!missingGetters.isEmpty()) {
            StringBuilder error = new StringBuilder();
            error.append("ErpConfig 缺少以下 configJson 字段的 getter 方法:\n\n");
            for (String missing : missingGetters) {
                error.append("  - ").append(missing).append("\n");
            }
            error.append("\n请在 ErpConfig.java 中添加这些方法：\n");
            error.append("private String getConfigValue(String key) { ... }\n");
            for (String field : REQUIRED_CONFIG_FIELDS) {
                String methodName = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
                error.append("public String ").append(methodName).append("() { return getConfigValue(\"")
                      .append(field).append("\"); }\n");
            }

            throw new AssertionError(error.toString());
        }
    }

    /**
     * 规则 2：所有 getter 方法必须正确实现（非空返回类型）
     *
     * <p>确保 getter 方法返回 String 类型，而不是 void 或其他类型。</p>
     */
    @Test
    @DisplayName("ErpConfig getter methods must return String type")
    void erpConfigGetterMethodsMustReturnString() {
        List<JavaClass> allClasses = getAllClasses();
        JavaClass erpConfigClass = findClass(allClasses, "ErpConfig");

        if (erpConfigClass == null) {
            throw new AssertionError("ErpConfig class not found");
        }

        String sourceCode = getSourceCode(erpConfigClass);
        List<String> invalidGetters = new ArrayList<>();

        for (String field : REQUIRED_CONFIG_FIELDS) {
            String expectedMethodName = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);

            // 检查方法签名：public String getXxx()
            Pattern pattern = Pattern.compile(
                    "public\\s+String\\s+" + Pattern.quote(expectedMethodName) + "\\s*\\(\\s*\\)"
            );

            if (!hasMethod(sourceCode, expectedMethodName)) {
                continue; // 在规则 1 中已经报告
            }

            Matcher matcher = pattern.matcher(sourceCode);
            if (!matcher.find()) {
                invalidGetters.add(expectedMethodName + "() 必须返回 String 类型");
            }
        }

        if (!invalidGetters.isEmpty()) {
            StringBuilder error = new StringBuilder();
            error.append("ErpConfig getter 方法签名不正确:\n\n");
            for (String invalid : invalidGetters) {
                error.append("  - ").append(invalid).append("\n");
            }

            throw new AssertionError(error.toString());
        }
    }

    /**
     * 规则 3：ErpConfigService 必须区分对外和对内方法
     *
     * <p>防止敏感信息通过 API 泄露：</p>
     * <ul>
     *   <li>findById() - 用于 API 返回，必须执行 sanitize</li>
     *   <li>getByIdForInternalUse() - 用于内部调用，不执行 sanitize</li>
     * </ul>
     */
    @Test
    @DisplayName("ErpConfigService must have separate methods for API and internal use")
    void erpConfigServiceMustSeparateApiAndInternalMethods() {
        List<JavaClass> allClasses = getAllClasses();
        JavaClass serviceClass = findClass(allClasses, "ErpConfigServiceImpl");
        JavaClass serviceInterface = findClass(allClasses, "ErpConfigService");

        if (serviceClass == null || serviceInterface == null) {
            throw new AssertionError("ErpConfigService implementation not found");
        }

        String serviceSource = getSourceCode(serviceClass);
        String interfaceSource = getSourceCode(serviceInterface);

        List<String> issues = new ArrayList<>();

        // 检查接口声明
        if (!interfaceSource.contains("ErpConfig getByIdForInternalUse")) {
            issues.add("ErpConfigService 接口缺少 getByIdForInternalUse() 方法声明");
        }

        // 检查实现
        if (!serviceSource.contains("public ErpConfig getByIdForInternalUse")) {
            issues.add("ErpConfigServiceImpl 缺少 getByIdForInternalUse() 方法实现");
        }

        // 检查 findById 是否执行 sanitize
        if (!serviceSource.contains("sanitizeSensitiveFields(config)")) {
            issues.add("findById() 方法未执行 sanitizeSensitiveFields()，可能导致敏感信息泄露");
        }

        // 检查 getByIdForInternalUse 是否跳过 sanitize
        if (serviceSource.contains("getByIdForInternalUse") &&
            serviceSource.contains("sanitizeSensitiveFields(config)")) {
            // 需要确认 getByIdForInternalUse 中的 sanitize 调用
            int methodStart = serviceSource.indexOf("public ErpConfig getByIdForInternalUse");
            int methodEnd = serviceSource.indexOf("\n    }", methodStart);
            if (methodEnd > 0 && methodEnd < serviceSource.indexOf("public ErpConfig getByIdForInternalUse") + 500) {
                String methodBody = serviceSource.substring(methodStart, methodEnd);
                if (methodBody.contains("sanitizeSensitiveFields") &&
                    !methodBody.contains("// 不清除敏感信息")) {
                    issues.add("getByIdForInternalUse() 不应执行 sanitizeSensitiveFields()");
                }
            }
        }

        if (!issues.isEmpty()) {
            StringBuilder error = new StringBuilder();
            error.append("ErpConfigService 架构问题:\n\n");
            for (String issue : issues) {
                error.append("  ❌ ").append(issue).append("\n");
            }
            error.append("\n期望的架构:\n");
            error.append("  - findById() → 调用 sanitizeSensitiveFields()（用于 API）\n");
            error.append("  - getByIdForInternalUse() → 不调用 sanitize（用于内部）\n");

            throw new AssertionError(error.toString());
        }
    }

    /**
     * 规则 4：Controller 必须使用正确的 Service 方法
     *
     * <p>确保 testConnection 使用 getByIdForInternalUse() 而不是 findById()。</p>
     */
    @Test
    @DisplayName("Controller must use getByIdForInternalUse for operations needing sensitive data")
    void controllerMustUseInternalMethodForSensitiveOperations() {
        List<JavaClass> allClasses = getAllClasses();
        JavaClass controllerClass = findClass(allClasses, "ErpConfigController");

        if (controllerClass == null) {
            throw new AssertionError("ErpConfigController not found");
        }

        String sourceCode = getSourceCode(controllerClass);
        List<String> issues = new ArrayList<>();

        // testConnection 必须使用 getByIdForInternalUse
        if (sourceCode.contains("testConnection") &&
            sourceCode.contains("erpConfigService.findById")) {
            // 检查是否在 testConnection 方法中调用了 findById
            int testMethodStart = sourceCode.indexOf("public Result<java.util.Map");
            if (testMethodStart < 0) testMethodStart = sourceCode.indexOf("@PostMapping");

            if (testMethodStart >= 0) {
                int testMethodEnd = sourceCode.indexOf("return Result.success", testMethodStart);
                if (testMethodEnd > 0) {
                    String testMethodBody = sourceCode.substring(testMethodStart, testMethodEnd);
                    if (testMethodBody.contains("erpConfigService.findById") &&
                        !testMethodBody.contains("getByIdForInternalUse")) {
                        issues.add("testConnection() 方法必须使用 getByIdForInternalUse() 而不是 findById()");
                    }
                }
            }
        }

        if (!issues.isEmpty()) {
            throw new AssertionError(String.join("\n", issues));
        }
    }

    // ========== Helper Methods ==========

    /**
     * 查找指定名称的类
     */
    private JavaClass findClass(List<JavaClass> classes, String simpleName) {
        for (JavaClass cls : classes) {
            if (cls.getSimpleName().equals(simpleName)) {
                return cls;
            }
        }
        return null;
    }

    /**
     * 获取类的源代码
     */
    private String getSourceCode(JavaClass clazz) {
        try {
            String className = clazz.getName();
            String packagePath = className.replace('.', '/');
            Path[] searchPaths = {
                    Path.of("src/main/java", packagePath + ".java"),
                    Path.of("nexusarchive-java/src/main/java", packagePath + ".java")
            };

            for (Path path : searchPaths) {
                if (Files.exists(path)) {
                    return String.join("\n", Files.readAllLines(path));
                }
            }

            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 检查源代码中是否存在指定的方法
     */
    private boolean hasMethod(String sourceCode, String methodName) {
        // 匹配 public/private/protected + 返回类型 + 方法名 + 参数
        Pattern pattern = Pattern.compile(
                "(public|private|protected)\\s+(?:static\\s+)?[\\w<>\\[\\]]+\\s+" +
                        Pattern.quote(methodName) + "\\s*\\("
        );
        return pattern.matcher(sourceCode).find();
    }
}
