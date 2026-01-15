// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/identifier/ScenarioNamer.java
// Input: OpenApiDefinition
// Output: ScenarioName with generated key, display name, and description
// Pos: AI 模块 - 场景命名生成器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.identifier;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 场景命名生成器
 * <p>
 * 从 OpenAPI 定义自动生成场景名称、显示名称和描述
 * </p>
 */
@Component
public class ScenarioNamer {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]");
    private static final Pattern PATH_PARAMETER = Pattern.compile("\\{[^}]+\\}");

    /**
     * 生成场景名称
     *
     * @param definition OpenAPI 定义
     * @return 场景名称对象
     */
    public ScenarioName generateScenarioName(OpenApiDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("OpenApiDefinition cannot be null");
        }

        String scenarioKey = generateScenarioKey(definition.getPath());
        String displayName = generateDisplayName(definition);
        String description = generateDescription(definition);

        return new ScenarioName(scenarioKey, displayName, description);
    }

    /**
     * 生成场景标识码
     * <p>
     * 提取路径的最后2-3段，移除路径参数，转换为大写并使用下划线连接
     * </p>
     *
     * @param path API 路径
     * @return 场景标识码
     */
    private String generateScenarioKey(String path) {
        if (path == null || path.isEmpty()) {
            return "UNKNOWN";
        }

        // 移除路径参数如 {id}
        String cleanedPath = PATH_PARAMETER.matcher(path).replaceAll("");

        // 分割路径
        String[] segments = cleanedPath.split("/");

        // 过滤空段和常见前缀段
        String[] filteredSegments = Arrays.stream(segments)
                .filter(s -> !s.isEmpty())
                .filter(s -> !isCommonPrefix(s))
                .toArray(String[]::new);

        // 提取最后2-3段
        int segmentCount = filteredSegments.length;
        int start = Math.max(0, segmentCount - 3);
        String[] relevantSegments = Arrays.stream(filteredSegments, start, filteredSegments.length)
                .toArray(String[]::new);

        // 清理特殊字符并转换为大写
        return Arrays.stream(relevantSegments)
                .map(segment -> NON_ALPHANUMERIC.matcher(segment).replaceAll(""))
                .filter(segment -> !segment.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.joining("_"));
    }

    /**
     * 判断是否为常见前缀段
     * <p>
     * 跳过如 api, v1, yonbip, digitalModel 等通用前缀
     * </p>
     *
     * @param segment 路径段
     * @return 是否为常见前缀
     */
    private boolean isCommonPrefix(String segment) {
        return segment.equalsIgnoreCase("api") ||
                segment.equalsIgnoreCase("v1") ||
                segment.equalsIgnoreCase("v2") ||
                segment.matches("^v\\d+$") ||
                segment.equalsIgnoreCase("yonbip") ||
                segment.equalsIgnoreCase("digitalModel");
    }

    /**
     * 生成显示名称
     * <p>
     * 优先级：summary > operationId > path
     * </p>
     *
     * @param definition OpenAPI 定义
     * @return 显示名称
     */
    private String generateDisplayName(OpenApiDefinition definition) {
        // 优先使用 summary
        if (definition.getSummary() != null && !definition.getSummary().isBlank()) {
            return definition.getSummary();
        }

        // 降级到 operationId，转换为可读格式
        if (definition.getOperationId() != null && !definition.getOperationId().isBlank()) {
            return convertOperationIdToDisplayName(definition.getOperationId());
        }

        // 最后使用路径
        return generateDisplayNameFromPath(definition.getPath());
    }

    /**
     * 将 operationId 转换为显示名称
     * <p>
     * 例如: getReceiptList -> Get Receipt List
     * </p>
     *
     * @param operationId 操作 ID
     * @return 显示名称
     */
    private String convertOperationIdToDisplayName(String operationId) {
        // 使用驼峰命名转换
        String[] words = operationId.split("(?=[A-Z])");
        return Arrays.stream(words)
                .filter(w -> !w.isEmpty())
                .map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * 从路径生成显示名称
     *
     * @param path API 路径
     * @return 显示名称
     */
    private String generateDisplayNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "Unknown Scenario";
        }

        // 移除路径参数
        String cleanedPath = PATH_PARAMETER.matcher(path).replaceAll("");

        // 提取最后2段
        String[] segments = cleanedPath.split("/");
        int start = Math.max(0, segments.length - 2);
        String[] relevantSegments = Arrays.stream(segments, start, segments.length)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        // 清理特殊字符
        return Arrays.stream(relevantSegments)
                .map(segment -> NON_ALPHANUMERIC.matcher(segment).replaceAll(" "))
                .filter(segment -> !segment.trim().isEmpty())
                .map(segment -> segment.substring(0, 1).toUpperCase() + segment.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * 生成描述
     * <p>
     * 格式: AI 自动识别: {path} ({method})
     * </p>
     *
     * @param definition OpenAPI 定义
     * @return 描述
     */
    private String generateDescription(OpenApiDefinition definition) {
        String path = definition.getPath() != null ? definition.getPath() : "unknown";
        String method = definition.getMethod() != null ? definition.getMethod() : "UNKNOWN";
        return String.format("AI 自动识别: %s (%s)", path, method);
    }
}
