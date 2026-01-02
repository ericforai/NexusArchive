// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/agent/ErpAdaptationOrchestrator.java
// Input: Files + ERP info
// Output: AdaptationResult containing generated code
// Pos: AI 模块 - Agent 编排器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.agent;

import com.nexusarchive.integration.erp.ai.generator.ErpAdapterCodeGenerator;
import com.nexusarchive.integration.erp.ai.generator.GeneratedCode;
import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import com.nexusarchive.integration.erp.ai.mapper.StandardScenario;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDocumentParser;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 适配编排器
 * <p>
 * 协调各个 AI 组件完成 ERP 适配流程
 * </p>
 */
@Slf4j
@Service
public class ErpAdaptationOrchestrator {

    private final OpenApiDocumentParser documentParser;
    private final BusinessSemanticMapper semanticMapper;
    private final ErpAdapterCodeGenerator codeGenerator;

    public ErpAdaptationOrchestrator(OpenApiDocumentParser documentParser,
                                    BusinessSemanticMapper semanticMapper,
                                    ErpAdapterCodeGenerator codeGenerator) {
        this.documentParser = documentParser;
        this.semanticMapper = semanticMapper;
        this.codeGenerator = codeGenerator;
    }

    /**
     * 执行 ERP 适配
     *
     * @param request 适配请求
     * @return 适配结果
     */
    public AdaptationResult adapt(AdaptationRequest request) throws IOException {
        log.info("开始 ERP 适配: erpType={}, erpName={}", request.getErpType(), request.getErpName());

        // Step 1: 解析 API 文档
        log.info("Step 1: 解析 API 文档");
        List<OpenApiDefinition> definitions = parseApiDocuments(request.getApiFiles());
        if (definitions.isEmpty()) {
            return AdaptationResult.failure("未能从文档中提取任何 API 定义");
        }
        log.info("提取到 {} 个 API 定义", definitions.size());

        // Step 2: 映射到标准场景
        log.info("Step 2: 映射到标准场景");
        List<BusinessSemanticMapper.ScenarioMapping> mappings = mapToScenarios(definitions);
        log.info("识别到 {} 个标准场景", mappings.size());

        // 统计场景分布
        var scenarioCounts = mappings.stream()
            .collect(Collectors.groupingBy(m -> m.getScenario(), Collectors.counting()));
        scenarioCounts.forEach((scenario, count) ->
            log.info("  - {}: {} 个 API", scenario.getDescription(), count));

        // Step 3: 生成适配器代码
        log.info("Step 3: 生成适配器代码");
        GeneratedCode code = codeGenerator.generate(mappings, request.getErpType(), request.getErpName());
        log.info("代码生成完成: className={}, dtoCount={}",
            code.getClassName(), code.getDtoClasses().size());

        // Step 4: 构建结果
        return AdaptationResult.builder()
            .success(true)
            .code(code)
            .mappings(mappings)
            .adapterId(request.getErpType().toLowerCase().replace(" ", "-"))
            .message("ERP 适配完成")
            .build();
    }

    /**
     * 解析 API 文档
     */
    private List<OpenApiDefinition> parseApiDocuments(List<MultipartFile> files) throws IOException {
        List<OpenApiDefinition> allDefinitions = new ArrayList<>();

        for (MultipartFile file : files) {
            log.info("解析文件: {}", file.getOriginalFilename());
            var result = documentParser.parse(file);

            if (result.isSuccess()) {
                allDefinitions.addAll(result.getDefinitions());
                log.info("  成功提取 {} 个 API 定义", result.getDefinitions().size());
            } else {
                log.warn("  文件解析失败: {}", result.getErrorMessage());
            }
        }

        return allDefinitions;
    }

    /**
     * 映射到标准场景
     */
    private List<BusinessSemanticMapper.ScenarioMapping> mapToScenarios(List<OpenApiDefinition> definitions) {
        List<BusinessSemanticMapper.ScenarioMapping> mappings = new ArrayList<>();

        for (OpenApiDefinition definition : definitions) {
            var mapping = semanticMapper.mapToScenario(definition);
            if (mapping.getScenario() != StandardScenario.UNKNOWN) {
                mappings.add(mapping);
            } else {
                log.debug("跳过未知场景 API: {}", definition.getOperationId());
            }
        }

        return mappings;
    }

    /**
     * 适配请求
     */
    @Data
    @Builder
    public static class AdaptationRequest {
        private String erpType;
        private String erpName;
        private List<MultipartFile> apiFiles;
    }

    /**
     * 适配结果
     */
    @Data
    @Builder
    public static class AdaptationResult {
        private boolean success;
        private GeneratedCode code;
        private List<BusinessSemanticMapper.ScenarioMapping> mappings;
        private String adapterId;
        private String message;

        public static AdaptationResult failure(String errorMessage) {
            return AdaptationResult.builder()
                .success(false)
                .message(errorMessage)
                .build();
        }
    }
}
