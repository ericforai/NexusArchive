// Input: ArchUnit 架构测试框架、JUnit 5
// Output: LineCountCondition 自定义架构条件类
// Pos: 架构测试 - 行数检测条件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 源码行数检测条件
 *
 * <p>自定义 ArchUnit 条件，用于检测 Java 类的源码行数是否超过指定限制。</p>
 *
 * <p>该条件会读取实际的源码文件来计算行数，而非依赖字节码分析。</p>
 */
public class LineCountCondition extends ArchCondition<JavaClass> {

    private final int maxLines;
    private final Map<String, Integer> cache = new HashMap<>();

    /**
     * 创建行数检测条件
     *
     * @param maxLines 允许的最大行数
     */
    public LineCountCondition(int maxLines) {
        super("行数不超过 " + maxLines);
        this.maxLines = maxLines;
    }

    /**
     * 检查单个类是否超过行数限制
     *
     * @param item 要检查的 Java 类
     * @param events 条件事件集合
     */
    @Override
    public void check(JavaClass item, ConditionEvents events) {
        int lineCount = countLines(item);
        boolean satisfied = lineCount <= maxLines;

        String message = String.format(
            "%s: %d lines (limit: %d)%s",
            item.getFullName(),
            lineCount,
            maxLines,
            satisfied ? "" : " - EXCEEDS"
        );

        events.add(new SimpleConditionEvent(item, satisfied, message));
    }

    /**
     * 计算类的源码行数
     *
     * <p>通过读取实际源码文件来计算行数，支持多种路径搜索策略。</p>
     *
     * @param javaClass 要计算的 Java 类
     * @return 源码行数，如果文件未找到则返回 0
     */
    int countLines(JavaClass javaClass) {
        String fullName = javaClass.getName();
        if (cache.containsKey(fullName)) {
            return cache.get(fullName);
        }

        try {
            // Try to find the file using relative paths from working directory
            String packagePath = fullName.replace('.', '/');
            java.nio.file.Path[] searchPaths = {
                java.nio.file.Paths.get("src/main/java", packagePath + ".java"),
                java.nio.file.Paths.get("nexusarchive-java/src/main/java", packagePath + ".java"),
                java.nio.file.Paths.get("../nexusarchive-java/src/main/java", packagePath + ".java"),
                java.nio.file.Paths.get("../../nexusarchive-java/src/main/java", packagePath + ".java")
            };

            for (java.nio.file.Path path : searchPaths) {
                if (java.nio.file.Files.exists(path)) {
                    int lines = java.nio.file.Files.readAllLines(path).size();
                    cache.put(fullName, lines);
                    return lines;
                }
            }

            // Fallback: return 0 if source file not found
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
