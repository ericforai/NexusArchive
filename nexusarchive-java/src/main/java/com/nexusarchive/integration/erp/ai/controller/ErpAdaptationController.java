// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java
// Input: HTTP requests
// Output: JSON responses
// Pos: AI 模块 - REST API 控制器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.integration.erp.ai.agent.ErpAdaptationOrchestrator;
import com.nexusarchive.integration.erp.ai.agent.ErpAdaptationOrchestrator.AdaptationRequest;
import com.nexusarchive.integration.erp.ai.agent.ErpAdaptationOrchestrator.AdaptationResult;
import com.nexusarchive.integration.erp.ai.dto.AiGenerationSession;
import com.nexusarchive.integration.erp.ai.identifier.ErpTypeIdentifier;
import com.nexusarchive.integration.erp.ai.identifier.ScenarioName;
import com.nexusarchive.integration.erp.ai.identifier.ScenarioNamer;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDocumentParser;
import com.nexusarchive.integration.erp.ai.service.AiGenerationSessionService;
import com.nexusarchive.service.ErpConfigService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ERP 适配 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping(value = "/erp-ai", produces = "application/json;charset=UTF-8")
public class ErpAdaptationController {

    private final ErpAdaptationOrchestrator orchestrator;
    private final OpenApiDocumentParser openApiDocumentParser;
    private final ErpTypeIdentifier erpTypeIdentifier;
    private final ScenarioNamer scenarioNamer;
    private final ErpConfigService erpConfigService;
    private final AiGenerationSessionService aiGenerationSessionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ErpAdaptationController(
            ErpAdaptationOrchestrator orchestrator,
            OpenApiDocumentParser openApiDocumentParser,
            ErpTypeIdentifier erpTypeIdentifier,
            ScenarioNamer scenarioNamer,
            ErpConfigService erpConfigService,
            AiGenerationSessionService aiGenerationSessionService) {
        this.orchestrator = orchestrator;
        this.openApiDocumentParser = openApiDocumentParser;
        this.erpTypeIdentifier = erpTypeIdentifier;
        this.scenarioNamer = scenarioNamer;
        this.erpConfigService = erpConfigService;
        this.aiGenerationSessionService = aiGenerationSessionService;
    }

    /**
     * 上传接口文件并生成适配器
     */
    @PostMapping("/adapt")
    public ResponseEntity<ApiResponse> adaptErp(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("erpType") String erpType,
            @RequestParam("erpName") String erpName
    ) {
        try {
            log.info("收到 ERP 适配请求: erpType={}, erpName={}, fileCount={}",
                erpType, erpName, files.size());

            // 构建请求
            AdaptationRequest request = AdaptationRequest.builder()
                .erpType(erpType)
                .erpName(erpName)
                .apiFiles(files)
                .build();

            // 执行适配
            AdaptationResult result = orchestrator.adapt(request);

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(result.getMessage()));
            }

        } catch (IOException e) {
            log.error("文件处理失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("文件处理失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("ERP 适配失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 上传接口文件、生成适配器并自动部署
     *
     * 功能包括：
     * 1. 解析 OpenAPI 文档
     * 2. 生成适配器代码
     * 3. 保存代码到源码目录
     * 4. 自动编译验证
     * 5. 自动运行测试
     * 6. 数据库自动注册
     * 7. 热加载适配器（MVP 版本需手动重启）
     */
    @PostMapping("/deploy")
    public ResponseEntity<ApiResponse> adaptAndDeployErp(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("erpType") String erpType,
            @RequestParam("erpName") String erpName
    ) {
        try {
            log.info("收到 ERP 适配+自动部署请求: erpType={}, erpName={}, fileCount={}",
                erpType, erpName, files.size());

            // 构建请求
            AdaptationRequest request = AdaptationRequest.builder()
                .erpType(erpType)
                .erpName(erpName)
                .apiFiles(files)
                .build();

            // 执行适配+自动部署
            AdaptationResult result = orchestrator.adaptAndDeploy(request);

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(result.getMessage()));
            }

        } catch (IOException e) {
            log.error("文件处理失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("文件处理失败: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("部署过程被中断", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("部署被中断: " + e.getMessage()));
        } catch (Exception e) {
            log.error("ERP 适配或部署失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 预览 API 接口场景
     * <p>
     * 上传 OpenAPI 文档，识别 ERP 类型和场景，返回预览信息
     * </p>
     *
     * @param file OpenAPI 文档文件
     * @param targetConfigId 目标连接器 ID（可选）
     * @param packageName 包名（可选，用于代码生成）
     * @return 预览响应
     */
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse> previewScenarios(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetConfigId", required = false) Long targetConfigId,
            @RequestParam(value = "packageName", required = false) String packageName) {

        try {
            log.info("收到预览请求: fileName={}, targetConfigId={}", file.getOriginalFilename(), targetConfigId);

            // 1. 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件不能为空"));
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件名不能为空"));
            }

            // 2. 解析 OpenAPI 文档
            OpenApiDocumentParser.ParseResult parseResult = openApiDocumentParser.parse(file);
            if (!parseResult.isSuccess()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("OpenAPI 解析失败: " + parseResult.getErrorMessage()));
            }

            List<OpenApiDefinition> definitions = parseResult.getDefinitions();
            if (definitions == null || definitions.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("未找到任何 API 定义"));
            }

            // 3. 识别 ERP 类型
            ErpTypeIdentifier.ErpType erpType = erpTypeIdentifier.identify(fileName, null);
            log.info("识别到 ERP 类型: {}", erpType.getCode());

            // 4. 生成场景预览
            List<ScenarioPreviewItem> scenarios = definitions.stream()
                .map(def -> {
                    ScenarioName scenarioName = scenarioNamer.generateScenarioName(def);
                    return ScenarioPreviewItem.builder()
                        .scenarioKey(scenarioName.scenarioKey())
                        .displayName(scenarioName.displayName())
                        .description(scenarioName.description())
                        .apiPath(def.getPath())
                        .operationId(def.getOperationId())
                        .method(def.getMethod())
                        .build();
                })
                .toList();

            // 5. 查找现有配置
            List<ErpConfig> existingConfigs = erpConfigService.findConfigsByErpType(erpType.getCode());
            List<ExistingConfigItem> configItems = existingConfigs.stream()
                .map(config -> {
                    String baseUrl = extractBaseUrl(config.getConfigJson());
                    return ExistingConfigItem.builder()
                        .configId(config.getId())
                        .name(config.getName())
                        .baseUrl(baseUrl)
                        .build();
                })
                .toList();

            // 6. 建议配置 ID
            Long suggestedConfigId = null;
            if (!configItems.isEmpty()) {
                suggestedConfigId = configItems.get(0).getConfigId();
                log.info("建议使用现有配置: configId={}", suggestedConfigId);
            }

            // 7. 如果用户指定了 targetConfigId，验证它
            if (targetConfigId != null) {
                boolean found = configItems.stream()
                    .anyMatch(c -> c.getConfigId().equals(targetConfigId));
                if (!found) {
                    log.warn("用户指定的配置 ID 不存在或不匹配: targetConfigId={}", targetConfigId);
                }
            }

            // 8. 构建响应
            PreviewResponse previewResponse = PreviewResponse.builder()
                .erpType(erpType.getCode())
                .erpDisplayName(erpType.getDisplayName())
                .scenarios(scenarios)
                .existingConfigs(configItems)
                .suggestedConfigId(suggestedConfigId)
                .build();

            return ResponseEntity.ok(ApiResponse.success(previewResponse));

        } catch (IOException e) {
            log.error("文件处理失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("文件处理失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("预览失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 简化的部署接口
     * <p>
     * 上传 OpenAPI 文档，自动识别并部署到指定连接器
     * </p>
     *
     * @param file OpenAPI 文档文件
     * @param erpSystem ERP 系统类型
     * @param targetConfigId 目标连接器 ID（可选，不提供则创建新连接器）
     * @return 部署结果
     */
    @PostMapping("/adapt-deploy")
    public ResponseEntity<ApiResponse> adaptAndDeploySimple(
            @RequestParam("file") MultipartFile file,
            @RequestParam("erpSystem") String erpSystem,
            @RequestParam(value = "targetConfigId", required = false) Long targetConfigId) {

        try {
            log.info("收到简化部署请求: fileName={}, erpSystem={}, targetConfigId={}",
                file.getOriginalFilename(), erpSystem, targetConfigId);

            // 1. 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件不能为空"));
            }

            // 2. 解析 OpenAPI 文档
            OpenApiDocumentParser.ParseResult parseResult = openApiDocumentParser.parse(file);
            if (!parseResult.isSuccess()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("OpenAPI 解析失败: " + parseResult.getErrorMessage()));
            }

            // 3. 生成适配器名称
            String timestamp = String.valueOf(System.currentTimeMillis());
            String erpName = targetConfigId != null ? "existing-" + timestamp : erpSystem + "-" + timestamp;

            // 4. 调用完整的部署逻辑
            List<MultipartFile> files = List.of(file);
            AdaptationRequest request = AdaptationRequest.builder()
                .erpType(erpSystem)
                .erpName(erpName)
                .apiFiles(files)
                .build();

            // 5. 执行适配+部署
            AdaptationResult result = orchestrator.adaptAndDeploy(request);

            if (result.isSuccess()) {
                String message = targetConfigId != null
                    ? "成功部署 " + erpSystem + " 适配器到连接器 ID: " + targetConfigId
                    : "成功创建并部署新的 " + erpSystem + " 连接器";

                return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "message", message,
                    "erpType", erpSystem,
                    "adapterName", erpName,
                    "scenarioCount", parseResult.getDefinitions().size()
                )));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(result.getMessage()));
            }

        } catch (IOException e) {
            log.error("文件处理失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("文件处理失败: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("部署过程被中断", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("部署被中断: " + e.getMessage()));
        } catch (Exception e) {
            log.error("部署失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 从 configJson 中提取 baseUrl
     */
    private String extractBaseUrl(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> config = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
            return (String) config.get("host");
        } catch (Exception e) {
            log.debug("无法解析 configJson: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 预览生成的代码
     * MVP: 简化实现，返回基本信息
     */
    @GetMapping("/preview/{sessionId}")
    public ResponseEntity<ApiResponse> previewCode(@PathVariable String sessionId) {
        // MVP: 简化实现， sessionId 在当前版本中未使用
        // 完整版本会从会话存储中获取生成的代码
        return ResponseEntity.ok(ApiResponse.success("预览功能（MVP 简化版本）"));
    }

    /**
     * 生成代码（AI 生成，返回会话 ID）
     * <p>
     * 已废弃：AI 生成功能已被移除。
     * 请使用 /adapt-with-deploy 端点进行基于模板的代码生成。
     * </p>
     */
    @PostMapping("/generate-ai")
    @Deprecated
    public ResponseEntity<ApiResponse> generateWithAi(
            @RequestParam("file") MultipartFile file,
            @RequestParam("erpType") String erpType,
            @RequestParam("erpName") String erpName,
            @RequestParam(value = "baseUrl", required = false) String baseUrl,
            @RequestParam(value = "authType", required = false) String authType) {

        log.warn("收到已废弃的 AI 生成请求: erpType={}, erpName={}", erpType, erpName);

        return ResponseEntity.badRequest().body(ApiResponse.error(
            "AI code generation is no longer available. " +
            "Please use the /adapt-with-deploy endpoint for template-based code generation. " +
            "External LLM API clients have been removed from the system."
        ));
    }

    /**
     * 提供反馈并重新生成
     */
    @PostMapping("/regenerate-ai/{sessionId}")
    public ResponseEntity<ApiResponse> regenerateWithFeedback(
            @PathVariable String sessionId,
            @RequestBody FeedbackRequest feedback) {

        try {
            log.info("收到重新生成请求: sessionId={}", sessionId);

            AiGenerationSession updatedSession = aiGenerationSessionService.regenerate(
                sessionId,
                feedback.getUserFeedback()
            );

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "sessionId", updatedSession.getSessionId(),
                "iterationCount", updatedSession.getIterationCount(),
                "generatedCode", updatedSession.getGeneratedCode()
            )));

        } catch (Exception e) {
            log.error("重新生成失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("重新生成失败: " + e.getMessage()));
        }
    }

    /**
     * 批准生成的代码
     */
    @PostMapping("/approve/{sessionId}")
    public ResponseEntity<ApiResponse> approveCode(@PathVariable String sessionId) {
        try {
            aiGenerationSessionService.approve(sessionId);
            return ResponseEntity.ok(ApiResponse.success("代码已批准，将继续部署"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("批准失败: " + e.getMessage()));
        }
    }

    @Data
    private static class FeedbackRequest {
        private String userFeedback;
    }

    /**
     * API 响应封装
     */
    @Data
    @lombok.Builder
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public static ApiResponse success(Object data) {
            return ApiResponse.builder()
                .success(true)
                .message("操作成功")
                .data(data)
                .build();
        }

        public static ApiResponse error(String message) {
            return ApiResponse.builder()
                .success(false)
                .message(message)
                .build();
        }
    }

    /**
     * 预览响应
     */
    @Data
    @lombok.Builder
    public static class PreviewResponse {
        private String erpType;
        private String erpDisplayName;
        private List<ScenarioPreviewItem> scenarios;
        private List<ExistingConfigItem> existingConfigs;
        private Long suggestedConfigId;
    }

    /**
     * 场景预览项
     */
    @Data
    @lombok.Builder
    public static class ScenarioPreviewItem {
        private String scenarioKey;
        private String displayName;
        private String description;
        private String apiPath;
        private String operationId;
        private String method;
    }

    /**
     * 现有配置项
     */
    @Data
    @lombok.Builder
    public static class ExistingConfigItem {
        private Long configId;
        private String name;
        private String baseUrl;
    }
}
