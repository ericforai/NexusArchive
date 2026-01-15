// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/parser/OpenApiDefinition.java
// Input: Lombok annotations, OpenAPI specification
// Output: OpenApiDefinition DTO classes
// Pos: AI 模块 - OpenAPI 文档解析器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.parser;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 接口定义
 * <p>
 * 表示从 OpenAPI 文档中提取的接口定义信息
 * </p>
 */
@Data
@Builder
public class OpenApiDefinition {

    /**
     * 接口路径
     */
    private String path;

    /**
     * HTTP 方法
     */
    private String method;

    /**
     * 操作 ID
     */
    private String operationId;

    /**
     * 接口摘要
     */
    private String summary;

    /**
     * 接口描述
     */
    private String description;

    /**
     * 请求参数
     */
    private List<ParameterDefinition> parameters;

    /**
     * 请求体定义
     */
    private RequestBodyDefinition requestBody;

    /**
     * 响应定义
     */
    private Map<String, ResponseDefinition> responses;

    /**
     * 标签
     */
    private List<String> tags;

    // ========== 便捷访问方法 ==========

    /**
     * 获取请求 Schema (JSON 格式)
     */
    public Object getRequestSchema() {
        if (requestBody != null && requestBody.getContent() != null) {
            return requestBody.getContent();
        }
        return null;
    }

    /**
     * 获取响应 Schema (JSON 格式)
     */
    public Object getResponseSchema() {
        if (responses != null && !responses.isEmpty()) {
            // 返回第一个响应的 schema
            ResponseDefinition firstResponse = responses.values().iterator().next();
            return firstResponse.getContent();
        }
        return null;
    }

    /**
     * 参数定义
     */
    @Data
    @Builder
    public static class ParameterDefinition {
        private String name;
        private String in; // query, path, header, cookie
        private String description;
        private boolean required;
        private String type;
        private String format;
        private Object defaultValue;
    }

    /**
     * 请求体定义
     */
    @Data
    @Builder
    public static class RequestBodyDefinition {
        private String description;
        private boolean required;
        private MediaTypeSchema content;
    }

    /**
     * 媒体类型 Schema
     */
    @Data
    @Builder
    public static class MediaTypeSchema {
        private String schemaType;
        private Map<String, Object> properties;
        private String ref; // $schema reference
    }

    /**
     * 响应定义
     */
    @Data
    @Builder
    public static class ResponseDefinition {
        private String description;
        private MediaTypeSchema content;
    }
}
