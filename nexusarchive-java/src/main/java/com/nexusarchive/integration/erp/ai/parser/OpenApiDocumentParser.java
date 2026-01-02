// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/parser/OpenApiDocumentParser.java
// Input: MultipartFile, OpenAPI specification
// Output: ParseResult containing OpenApiDefinition list
// Pos: AI 模块 - OpenAPI 文档解析器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.parser;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI 文档解析器
 * <p>
 * 解析 OpenAPI JSON/YAML 文件，提取接口定义
 * </p>
 */
@Slf4j
@Component
public class OpenApiDocumentParser {

    private final OpenAPIV3Parser parser = new OpenAPIV3Parser();

    /**
     * 解析上传的 OpenAPI 文件
     *
     * @param file 上传的文件
     * @return 解析结果
     * @throws IOException 读取文件失败
     */
    public ParseResult parse(MultipartFile file) throws IOException {
        // 1. 保存到临时文件
        Path tempFile = Files.createTempFile("openapi-", ".json");
        file.transferTo(tempFile.toFile());

        try {
            // 2. 解析 OpenAPI 文档
            SwaggerParseResult result = parser.readLocation(tempFile.toString(), null, null);

            OpenAPI openAPI = result.getOpenAPI();
            if (openAPI == null) {
                String errorMsg = result.getMessages() != null ? String.join(", ", result.getMessages()) : "Unknown error";
                log.error("Failed to parse OpenAPI: {}", errorMsg);
                return ParseResult.failure(errorMsg);
            }

            // 3. 提取所有接口定义
            List<OpenApiDefinition> definitions = extractDefinitions(openAPI);

            return ParseResult.success(definitions);

        } finally {
            // 4. 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * 从 OpenAPI 对象提取接口定义
     */
    private List<OpenApiDefinition> extractDefinitions(OpenAPI openAPI) {
        List<OpenApiDefinition> definitions = new ArrayList<>();

        Map<String, PathItem> paths = openAPI.getPaths();
        if (paths == null) {
            return definitions;
        }

        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();

            // 遍历所有 HTTP 方法
            pathItem.readOperationsMap().forEach((method, operation) -> {
                OpenApiDefinition definition = convertToDefinition(path, method, operation);
                definitions.add(definition);
            });
        }

        return definitions;
    }

    /**
     * 转换为 OpenApiDefinition
     */
    private OpenApiDefinition convertToDefinition(String path,
                                                  PathItem.HttpMethod method,
                                                  Operation operation) {
        return OpenApiDefinition.builder()
            .path(path)
            .method(method.name())
            .operationId(operation.getOperationId())
            .summary(operation.getSummary())
            .description(operation.getDescription())
            .tags(operation.getTags() != null ? new ArrayList<>(operation.getTags()) : List.of())
            .build();
    }

    /**
     * 解析结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ParseResult {
        private boolean success;
        private List<OpenApiDefinition> definitions;
        private String errorMessage;

        public static ParseResult success(List<OpenApiDefinition> definitions) {
            return ParseResult.builder()
                .success(true)
                .definitions(definitions)
                .build();
        }

        public static ParseResult failure(String errorMessage) {
            return ParseResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
        }
    }
}
