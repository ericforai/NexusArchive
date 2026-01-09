// Input: ArchUnit 框架、Java 源码分析
// Output: QueryWrapperUsageCondition 检测条件
// Pos: 架构测试 - 检测 QueryWrapper 使用情况
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QueryWrapper 使用检测条件
 *
 * <p>检测 Java 源码中是否使用了字符串方式的 QueryWrapper，
 * 强制使用类型安全的 LambdaQueryWrapper。</p>
 *
 * <p>违规模式：</p>
 * <ul>
 *   <li>{@code new QueryWrapper<>()}</li>
 *   <li>{@code Wrappers.query()}</li>
 *   <li>{@code Wrappers.lambdaQuery().eq("fieldName", ...)} - 字符串参数</li>
 * </ul>
 *
 * <p>允许模式：</p>
 * <ul>
 *   <li>{@code new LambdaQueryWrapper<>()}</li>
 *   <li>{@code Wrappers.lambdaQuery()}</li>
 *   <li>方法引用: {@code Entity::getField}</li>
 * </ul>
 */
public class QueryWrapperUsageCondition extends ArchCondition<JavaClass> {

    /**
     * 违规模式：直接使用 QueryWrapper（非 Lambda 版本）
     */
    private static final Pattern QUERY_WRAPPER_PATTERN = Pattern.compile(
            // 匹配 new QueryWrapper<>() 或 Wrappers.query()
            "\\b(new\\s+QueryWrapper|Wrappers\\.query()|Wrappers\\.<[^>]+>query\\(\\))"
    );

    /**
     * 违规模式：LambdaQueryWrapper 但使用字符串字段名
     * 例如: lambdaQuery().eq("fieldName", value) 而不是 lambdaQuery().eq(Entity::getField, value)
     */
    private static final Pattern STRING_FIELD_PATTERN = Pattern.compile(
            // 匹配 .eq("xxx", 或 .like("xxx", 等方法调用
            "\\.(eq|ne|gt|ge|lt|le|like|notLike|in|notIn|isNull|isNotNull)\\s*\\(\\s*\"[^\"]+\"\\s*,"
    );

    /**
     * 白名单：某些特殊场景下允许使用 QueryWrapper
     * 例如：动态字段名、外部系统字段映射等
     */
    private static final List<String> WHITELIST_COMMENTS = List.of(
            "ALLOW-QUERYWRAPPER",  // 源码中包含此注释时允许使用
            "DYNAMIC-FIELD"         // 动态字段场景
    );

    private final List<String> violations = new ArrayList<>();

    public QueryWrapperUsageCondition() {
        super("禁止使用 QueryWrapper，强制使用 LambdaQueryWrapper");
    }

    @Override
    public void check(JavaClass item, ConditionEvents events) {
        String className = item.getName();
        String sourceCode = readSourceCode(item);

        if (sourceCode == null || sourceCode.isEmpty()) {
            return; // 无法读取源码，跳过
        }

        // 检查白名单注释
        if (containsWhitelistComment(sourceCode)) {
            return;
        }

        List<String> fileViolations = new ArrayList<>();

        // 检测 QueryWrapper 使用
        Matcher queryMatcher = QUERY_WRAPPER_PATTERN.matcher(sourceCode);
        while (queryMatcher.find()) {
            fileViolations.add("使用 QueryWrapper: " + queryMatcher.group(1));
        }

        // 检测字符串字段名（在 Lambda 表达式中）
        Matcher stringMatcher = STRING_FIELD_PATTERN.matcher(sourceCode);
        while (stringMatcher.find()) {
            // 排除测试代码中的使用
            if (!className.endsWith("Test")) {
                fileViolations.add("使用字符串字段名: ." + stringMatcher.group(1) + "(\"...\")");
            }
        }

        boolean satisfied = fileViolations.isEmpty();

        if (!satisfied) {
            String message = String.format(
                    "%s: %n  发现 %d 处 QueryWrapper 违规使用%n  建议: 使用 LambdaQueryWrapper 替代%n  详情: %s",
                    item.getFullName(),
                    fileViolations.size(),
                    String.join(", ", fileViolations)
            );
            events.add(new SimpleConditionEvent(item, false, message));
            violations.add(className);
        } else {
            events.add(new SimpleConditionEvent(item, true, className + ": 使用 LambdaQueryWrapper ✓"));
        }
    }

    /**
     * 读取 Java 类的源码
     */
    private String readSourceCode(JavaClass javaClass) {
        String className = javaClass.getName();
        String packagePath = className.replace('.', '/');
        Path[] searchPaths = {
                Path.of("src/main/java", packagePath + ".java"),
                Path.of("nexusarchive-java/src/main/java", packagePath + ".java"),
                Path.of("../nexusarchive-java/src/main/java", packagePath + ".java")
        };

        for (Path path : searchPaths) {
            if (Files.exists(path)) {
                try {
                    return Files.readString(path);
                } catch (IOException e) {
                    // 继续尝试下一个路径
                }
            }
        }
        return null;
    }

    /**
     * 检查源码是否包含白名单注释
     */
    private boolean containsWhitelistComment(String sourceCode) {
        String upperCode = sourceCode.toUpperCase();
        for (String whitelist : WHITELIST_COMMENTS) {
            if (upperCode.contains(whitelist)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有违规类名
     */
    public List<String> getViolations() {
        return new ArrayList<>(violations);
    }
}
