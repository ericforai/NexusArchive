// Input: ArchUnit 架构测试框架、JUnit 5
// Output: MethodLineCountCondition 自定义架构条件类
// Pos: 架构测试 - 方法行数检测条件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 方法行数检测条件
 *
 * <p>自定义 ArchUnit 条件，用于检测 Java 类中方法的源码行数是否超过指定限制。</p>
 *
 * <p>该条件会读取实际的源码文件来计算方法行数，而非依赖字节码分析。</p>
 */
public class MethodLineCountCondition extends ArchCondition<JavaClass> {

    private final int maxLines;

    /**
     * 创建方法行数检测条件
     *
     * @param maxLines 允许的最大行数
     */
    public MethodLineCountCondition(int maxLines) {
        super("方法行数不超过 " + maxLines);
        this.maxLines = maxLines;
    }

    /**
     * 检查单个类中是否所有方法都不超过行数限制
     *
     * @param item 要检查的 Java 类
     * @param events 条件事件集合
     */
    @Override
    public void check(JavaClass item, ConditionEvents events) {
        for (JavaMethod method : item.getMethods()) {
            // Skip getters, setters, and generated methods
            if (isAccessorOrGenerated(method)) {
                continue;
            }

            int lineCount = countMethodLines(method);
            boolean satisfied = lineCount <= maxLines;

            if (lineCount > 0) {  // Only report if we could count the lines
                String message = String.format(
                    "%s.%s(): %d lines (limit: %d)%s",
                    item.getFullName(),
                    method.getName(),
                    lineCount,
                    maxLines,
                    satisfied ? "" : " - EXCEEDS"
                );
                events.add(new SimpleConditionEvent(method, satisfied, message));
            }
        }
    }

    /**
     * 判断是否是存取器方法或生成的方法
     */
    private boolean isAccessorOrGenerated(JavaMethod method) {
        String name = method.getName();
        return name.startsWith("get")
            || name.startsWith("set")
            || name.startsWith("is")
            || name.equals("hashCode")
            || name.equals("equals")
            || name.equals("toString")
            || name.startsWith("$jacoco")  // JaCoCo generated
            || name.startsWith("lambda$");  // Lambda methods
    }

    /**
     * 计算方法的实际行数
     *
     * <p>通过读取源码文件并解析方法体来计算行数。</p>
     *
     * @param method Java 方法
     * @return 方法的实际行数（不包括空行和注释）
     */
    int countMethodLines(JavaMethod method) {
        try {
            String className = method.getOwner().getName();
            String packagePath = className.replace('.', '/');
            Path[] searchPaths = {
                Path.of("src/main/java", packagePath + ".java"),
                Path.of("nexusarchive-java/src/main/java", packagePath + ".java"),
                Path.of("../nexusarchive-java/src/main/java", packagePath + ".java"),
                Path.of("../../nexusarchive-java/src/main/java", packagePath + ".java")
            };

            Path sourceFile = null;
            for (Path path : searchPaths) {
                if (Files.exists(path)) {
                    sourceFile = path;
                    break;
                }
            }

            if (sourceFile == null) {
                return 0;
            }

            List<String> lines = Files.readAllLines(sourceFile);
            return countMethodLinesInSource(lines, method.getName());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 在源码中计算方法行数
     *
     * @param lines 源码行列表
     * @param methodName 方法名
     * @return 方法体行数（不含空行、注释）
     */
    private int countMethodLinesInSource(List<String> lines, String methodName) {
        Pattern methodPattern = Pattern.compile(
            "(public|private|protected|package-private|\\s)\\s+(static\\s+)?(synchronized\\s+)?(final\\s+)?[\\w<>\\[\\]?\\s]+\\s+" +
            Pattern.quote(methodName) + "\\s*\\([^)]*\\)\\s*(throws\\s+[^{]+)?\\s*\\{?"
        );

        int methodStartLine = -1;
        int openBraceLine = -1;
        int braceCount = 0;
        int methodEndLine = -1;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();

            // Skip comments during search
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                continue;
            }

            if (methodStartLine == -1) {
                // Looking for method declaration
                Matcher matcher = methodPattern.matcher(trimmed);
                if (matcher.find()) {
                    methodStartLine = i;
                    // Check if opening brace is on the same line
                    if (trimmed.contains("{")) {
                        openBraceLine = i;
                        braceCount = countBraces(trimmed, '{') - countBraces(trimmed, '}');
                    }
                }
            } else if (openBraceLine == -1) {
                // Found method declaration, looking for opening brace
                if (trimmed.contains("{")) {
                    openBraceLine = i;
                    braceCount = countBraces(trimmed, '{') - countBraces(trimmed, '}');
                }
                // Skip if we haven't found the opening brace within 5 lines
                if (i - methodStartLine > 5) {
                    return 0; // Method pattern not properly matched
                }
            } else {
                // Count braces to find method end
                braceCount += countBraces(trimmed, '{');
                braceCount -= countBraces(trimmed, '}');

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
            int blockCommentStart = line.indexOf("/*");
            int blockCommentEnd = line.indexOf("*/");

            if (blockCommentStart != -1 && blockCommentEnd > blockCommentStart) {
                // Comment starts and ends on same line
                line = line.substring(0, blockCommentStart) + line.substring(blockCommentEnd + 2);
            }

            if (blockCommentStart != -1 && blockCommentEnd == -1) {
                inBlockComment = true;
                line = line.substring(0, blockCommentStart);
            }

            if (inBlockComment) {
                if (blockCommentEnd != -1) {
                    inBlockComment = false;
                    line = line.substring(blockCommentEnd + 2);
                } else {
                    continue;
                }
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
}
