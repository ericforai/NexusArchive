// Input: ArchUnit 测试框架、JUnit 5
// Output: ComplexityRulesTest 架构测试类
// Pos: 架构测试 - 代码复杂度规则检测
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 代码复杂度架构测试
 *
 * <p>检测代码复杂度违规，包括：</p>
 * <ul>
 *   <li>Service 类行数 <= 500</li>
 *   <li>Controller 类行数 <= 600</li>
 *   <li>Entity 类行数 <= 400</li>
 *   <li>方法行数 <= 50</li>
 *   <li>禁止使用 QueryWrapper（强制 LambdaQueryWrapper）</li>
 * </ul>
 *
 * <p>本测试读取源码文件来计算实际行数。</p>
 */
@Tag("architecture")
public class ComplexityRulesTest {

    private static final int MAX_SERVICE_LINES = 500;
    private static final int MAX_CONTROLLER_LINES = 600;
    private static final int MAX_ENTITY_LINES = 400;
    private static final int MAX_METHOD_LINES = 50;

    private final LineCountCondition lineCountCondition = new LineCountCondition(MAX_SERVICE_LINES);
    private final MethodLineCountCondition methodLineCountCondition = new MethodLineCountCondition(MAX_METHOD_LINES);
    private final QueryWrapperUsageCondition queryWrapperCondition = new QueryWrapperUsageCondition();

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
     * 获取所有类的 ArchUnit 集合（用于规则断言）
     */
    private com.tngtech.archunit.core.domain.JavaClasses getJavaClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.nexusarchive");
    }

    /**
     * 规则 1：Service 类不应超过 500 行
     *
     * <p>Service 类包含业务逻辑，应保持精简以利于维护。</p>
     * <p>违规时测试将失败。</p>
     */
    @Test
    @DisplayName("Service classes should not exceed 500 lines")
    void serviceClassesShouldNotExceed500Lines() {
        classes()
            .that().resideInAPackage("..service..")
            .should(new LineCountCondition(MAX_SERVICE_LINES))
            .as("Service classes should not exceed " + MAX_SERVICE_LINES + " lines")
            .check(getJavaClasses());
    }

    /**
     * 规则 2：Controller 类不应超过 600 行
     *
     * <p>Controller 应专注于请求处理，复杂业务逻辑应下沉到 Service 层。</p>
     * <p>违规时测试将失败。</p>
     */
    @Test
    @DisplayName("Controller classes should not exceed 600 lines")
    void controllerClassesShouldNotExceed600Lines() {
        classes()
            .that().resideInAPackage("..controller..")
            .should(new LineCountCondition(MAX_CONTROLLER_LINES))
            .as("Controller classes should not exceed " + MAX_CONTROLLER_LINES + " lines")
            .check(getJavaClasses());
    }

    /**
     * 规则 3：Entity 类不应超过 400 行
     *
     * <p>Entity 类应只包含数据定义和基本的 JPA 注解。</p>
     * <p>违规时测试将失败。</p>
     */
    @Test
    @DisplayName("Entity classes should not exceed 400 lines")
    void entityClassesShouldNotExceed400Lines() {
        classes()
            .that().resideInAPackage("..entity..")
            .should(new LineCountCondition(MAX_ENTITY_LINES))
            .as("Entity classes should not exceed " + MAX_ENTITY_LINES + " lines")
            .check(getJavaClasses());
    }

    /**
     * 规则 4：方法不应超过 50 行
     *
     * <p>长方法难以理解和测试，应拆分为更小的方法。</p>
     * <p>违规时测试将失败。</p>
     */
    @Test
    @DisplayName("Methods should not exceed 50 lines")
    void methodsShouldNotExceed50Lines() {
        classes()
            .that().resideInAPackage("com.nexusarchive")
            .should(methodLineCountCondition)
            .as("Methods should not exceed " + MAX_METHOD_LINES + " lines")
            .check(getJavaClasses());
    }

    /**
     * 规则 5：禁止使用 QueryWrapper，强制使用 LambdaQueryWrapper
     *
     * <p>LambdaQueryWrapper 提供编译期类型检查，避免字段名拼写错误。</p>
     * <p>违规时测试将失败，除非代码中包含白名单注释：</p>
     * <ul>
     *   <li>{@code // ALLOW-QUERYWRAPPER} - 允许使用 QueryWrapper</li>
     *   <li>{@code // DYNAMIC-FIELD} - 动态字段场景</li>
     * </ul>
     *
     * <p>正确示例：</p>
     * <pre>{@code
     * // ✅ 使用 LambdaQueryWrapper
     * LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
     * wrapper.eq(User::getName, "John");
     *
     * // ✅ 使用 Wrappers.lambdaQuery()
     * wrapper = Wrappers.lambdaQuery();
     * wrapper.like(User::getEmail, "@example.com");
     * }</pre>
     *
     * <p>错误示例：</p>
     * <pre>{@code
     * // ❌ 使用 QueryWrapper（字符串方式）
     * QueryWrapper<User> wrapper = new QueryWrapper<>();
     * wrapper.eq("name", "John");  // 字段名字符串，易出错
     * }</pre>
     */
    @Test
    @DisplayName("Should use LambdaQueryWrapper instead of QueryWrapper")
    void shouldUseLambdaQueryWrapper() {
        classes()
            .that().resideInAPackage("..service..")
            .or().resideInAPackage("..controller..")
            .or().resideInAPackage("..mapper..")
            .should(queryWrapperCondition)
            .as("QueryWrapper should be replaced with LambdaQueryWrapper for type safety")
            .check(getJavaClasses());
    }

    /**
     * 生成复杂度报告
     *
     * <p>汇总所有复杂度违规，按类别和行数排序。</p>
     */
    @Test
    @DisplayName("Generate complexity report")
    void generateComplexityReport() {
        List<JavaClass> allClasses = getAllClasses();

        List<ClassViolation> allViolations = new ArrayList<>();

        // Service 类
        allViolations.addAll(findViolationsInType(
                allClasses, "..service..", MAX_SERVICE_LINES, "Service"));

        // Controller 类
        allViolations.addAll(findViolationsInType(
                allClasses, "..controller..", MAX_CONTROLLER_LINES, "Controller"));

        // Entity 类
        allViolations.addAll(findViolationsInType(
                allClasses, "..entity..", MAX_ENTITY_LINES, "Entity"));

        // Integration 类 (ERP Adapters 等)
        allViolations.addAll(findViolationsInType(
                allClasses, "..integration..", MAX_SERVICE_LINES, "Integration"));

        // 按行数降序排序
        allViolations.sort(Comparator.comparingInt(ClassViolation::actualLines).reversed());

        // 输出报告
        System.out.println("\n" + "============================================================");
        System.out.println("                    COMPLEXITY REPORT");
        System.out.println("============================================================");
        System.out.println("Total classes analyzed: " + allClasses.size());
        System.out.println("Violations found: " + allViolations.size());

        if (allViolations.isEmpty()) {
            System.out.println("\nNo complexity violations found. Great job!");
        } else {
            System.out.println("\nFound " + allViolations.size() + " complexity violation(s):\n");

            Map<String, List<ClassViolation>> byType = allViolations.stream()
                    .collect(Collectors.groupingBy(ClassViolation::type));

            for (Map.Entry<String, List<ClassViolation>> entry : byType.entrySet()) {
                System.out.println("--- " + entry.getKey() + " Classes ---");
                for (ClassViolation v : entry.getValue()) {
                    System.out.println(String.format(
                            "  %s: %d lines (limit: %d) - +%d lines over",
                            v.className(), v.actualLines(), v.limit(), v.overage()
                    ));
                }
                System.out.println();
            }

            System.out.println(String.format(
                    "Summary: %d violation(s) across %d type(s)",
                    allViolations.size(), byType.size()
            ));
        }

        System.out.println("============================================================");
    }

    // ========== Helper Methods ==========

    private List<JavaClass> findViolations(List<JavaClass> classes, String packagePattern, int maxLines) {
        List<JavaClass> filtered = new ArrayList<>();
        for (JavaClass c : classes) {
            if (matchesPackagePattern(c.getPackageName(), packagePattern)) {
                int lines = lineCountCondition.countLines(c);
                if (lines > maxLines) {
                    filtered.add(c);
                }
            }
        }
        // Sort by line count descending
        filtered.sort((a, b) -> Integer.compare(lineCountCondition.countLines(b), lineCountCondition.countLines(a)));
        return filtered;
    }

    private List<ClassViolation> findViolationsInType(
            List<JavaClass> classes, String packagePattern, int maxLines, String type) {
        return classes.stream()
                .filter(c -> matchesPackagePattern(c.getPackageName(), packagePattern))
                .filter(c -> lineCountCondition.countLines(c) > maxLines)
                .map(c -> new ClassViolation(
                        c.getSimpleName(),
                        c.getPackageName(),
                        lineCountCondition.countLines(c),
                        maxLines,
                        type
                ))
                .collect(Collectors.toList());
    }

    private boolean matchesPackagePattern(String packageName, String pattern) {
        // The pattern "..service.." means "any package containing 'service'"
        String midPart = pattern.substring(2, pattern.length() - 2); // Remove leading ".." and trailing ".."
        return packageName.contains(midPart);
    }

    /**
     * 计算方法的实际行数
     *
     * <p>通过读取源码文件并解析方法体来计算行数。
     * 方法体行数包括方法签名后第一个大括号到最后一个大括号之间的所有非空、非注释行。</p>
     *
     * @param method Java 方法
     * @return 方法的实际行数（不包括空行和注释）
     */
    private int countMethodLines(JavaMethod method) {
        try {
            String className = method.getOwner().getName();
            String packagePath = className.replace('.', '/');
            java.nio.file.Path[] searchPaths = {
                java.nio.file.Paths.get("src/main/java", packagePath + ".java"),
                java.nio.file.Paths.get("nexusarchive-java/src/main/java", packagePath + ".java"),
                java.nio.file.Paths.get("../nexusarchive-java/src/main/java", packagePath + ".java"),
                java.nio.file.Paths.get("../../nexusarchive-java/src/main/java", packagePath + ".java")
            };

            java.nio.file.Path sourceFile = null;
            for (java.nio.file.Path path : searchPaths) {
                if (java.nio.file.Files.exists(path)) {
                    sourceFile = path;
                    break;
                }
            }

            if (sourceFile == null) {
                return 0;
            }

            List<String> lines = java.nio.file.Files.readAllLines(sourceFile);
            return countMethodLinesInSource(lines, method.getName(), method.getDescription());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 在源码中计算方法行数
     *
     * @param lines 源码行列表
     * @param methodName 方法名
     * @param methodSignature 方法签名（包含参数类型）
     * @return 方法体行数（不含空行、注释）
     */
    private int countMethodLinesInSource(List<String> lines, String methodName, String methodSignature) {
        // Build method signature pattern to find method start
        String simpleName = methodName;
        // Extract parameter types from method signature like "process(IngestRequest)"
        String paramPattern = "\\(([^)]*)\\)";
        Pattern methodPattern = Pattern.compile(
            "(public|private|protected|\\s)\\s+(static\\s+)?[\\w<>\\[\\]]+\\s+" + Pattern.quote(simpleName) + "\\s*\\([^)]*\\)"
        );

        int methodStartLine = -1;
        int openBraceLine = -1;
        int braceCount = 0;
        int methodEndLine = -1;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            // Skip comments
            if (line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
                continue;
            }

            if (methodStartLine == -1) {
                // Looking for method declaration
                Matcher matcher = methodPattern.matcher(line);
                if (matcher.find()) {
                    methodStartLine = i;
                    // Check if opening brace is on the same line
                    if (line.contains("{")) {
                        openBraceLine = i;
                        braceCount = countBraces(line, '{') - countBraces(line, '}');
                    }
                }
            } else if (openBraceLine == -1) {
                // Found method declaration, looking for opening brace
                if (line.contains("{")) {
                    openBraceLine = i;
                    braceCount = countBraces(line, '{') - countBraces(line, '}');
                }
                // Skip if we haven't found the opening brace within 5 lines
                if (i - methodStartLine > 5) {
                    return 0; // Method pattern not properly matched
                }
            } else {
                // Count braces to find method end
                braceCount += countBraces(line, '{');
                braceCount -= countBraces(line, '}');

                if (braceCount == 0) {
                    methodEndLine = i;
                    break;
                }
            }
        }

        if (methodStartLine == -1 || openBraceLine == -1 || methodEndLine == -1) {
            return 0;
        }

        // Count non-empty, non-comment lines in method body
        int count = 0;
        boolean inBlockComment = false;

        for (int i = openBraceLine + 1; i < methodEndLine; i++) {
            String line = lines.get(i).trim();

            // Handle block comments
            if (line.contains("/*")) {
                inBlockComment = true;
            }
            if (inBlockComment) {
                if (line.contains("*/")) {
                    inBlockComment = false;
                }
                continue;
            }

            // Skip empty lines and line comments
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("*")) {
                continue;
            }

            count++;
        }

        return count;
    }

    /**
     * 统计字符串中某个字符的出现次数
     */
    private int countBraces(String line, char brace) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == brace) {
                count++;
            }
        }
        return count;
    }

    private void reportViolations(List<JavaClass> violations, int maxLines) {
        if (violations.isEmpty()) {
            System.out.println("  No violations found.");
            return;
        }

        for (JavaClass cls : violations) {
            int lines = lineCountCondition.countLines(cls);
            System.out.println(String.format(
                    "  - %s: %d lines (exceeds limit by %d)",
                    cls.getSimpleName(), lines, lines - maxLines
            ));
        }
        System.out.println(String.format("\nTotal: %d class(es) exceed %d lines",
                violations.size(), maxLines));
    }

    // ========== Inner Classes ==========

    record ClassViolation(
            String className,
            String packageName,
            int actualLines,
            int limit,
            String type
    ) {
        int overage() {
            return actualLines - limit;
        }
    }

    record MethodViolation(
            String className,
            String methodName,
            int lineCount
    ) {}
}
