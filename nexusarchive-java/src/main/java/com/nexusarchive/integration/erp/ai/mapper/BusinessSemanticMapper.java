// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/BusinessSemanticMapper.java
// Input: OpenApiDefinition
// Output: ScenarioMapping
// Pos: AI 模块 - 业务语义映射器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.mapper;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务语义映射器
 * <p>
 * 核心 AI 组件：理解接口的业务意图，映射到系统标准场景
 * MVP 版本使用基于规则的关键词匹配
 * 未来版本可扩展为 LLM 智能分析
 * </p>
 */
@Slf4j
@Component
public class BusinessSemanticMapper {

    /**
     * 将 API 定义映射到标准场景
     *
     * @param definition API 定义
     * @return 场景映射结果
     */
    public ScenarioMapping mapToScenario(OpenApiDefinition definition) {
        // Step 1: 理解接口意图（基于规则分析）
        ApiIntent intent = understandIntent(definition);

        // Step 2: 匹配标准场景
        StandardScenario scenario = StandardScenario.fromIntent(intent);

        // Step 3: 生成映射配置
        MappingConfig config = generateMappingConfig(definition, scenario);

        log.info("API {} → 场景 {}", definition.getOperationId(), scenario.getCode());

        return ScenarioMapping.builder()
            .apiDefinition(definition)
            .intent(intent)
            .scenario(scenario)
            .config(config)
            .build();
    }

    /**
     * 理解接口意图（使用基于规则的分析）
     * MVP 版本：使用基于规则的关键词匹配
     * 未来版本：调用 Claude API 进行智能分析
     */
    private ApiIntent understandIntent(OpenApiDefinition definition) {
        // MVP: 使用基于规则的关键词匹配

        String path = definition.getPath() != null ? definition.getPath().toLowerCase() : "";
        String operationId = definition.getOperationId() != null ? definition.getOperationId().toLowerCase() : "";
        String summary = definition.getSummary() != null ? definition.getSummary().toLowerCase() : "";
        String tags = definition.getTags() != null ? String.join(" ", definition.getTags()).toLowerCase() : "";

        // 识别操作类型
        ApiIntent.OperationType operationType = detectOperationType(operationId, summary);

        // 识别业务对象
        ApiIntent.BusinessObject businessObject = detectBusinessObject(path, operationId, summary, tags);

        // 识别触发时机
        ApiIntent.TriggerTiming triggerTiming = detectTriggerTiming(definition.getParameters());

        // 识别数据流向
        ApiIntent.DataFlowDirection dataFlow = ApiIntent.DataFlowDirection.ERP_TO_SYSTEM;

        return ApiIntent.builder()
            .operationType(operationType)
            .businessObject(businessObject)
            .triggerTiming(triggerTiming)
            .dataFlowDirection(dataFlow)
            .build();
    }

    /**
     * 检测操作类型
     */
    private ApiIntent.OperationType detectOperationType(String operationId, String summary) {
        if (operationId.contains("list") || operationId.contains("query") || operationId.contains("get")) {
            return ApiIntent.OperationType.QUERY;
        } else if (operationId.contains("sync") || summary.contains("同步")) {
            return ApiIntent.OperationType.SYNC;
        } else if (operationId.contains("submit") || operationId.contains("create")) {
            return ApiIntent.OperationType.SUBMIT;
        } else if (operationId.contains("webhook") || operationId.contains("callback")) {
            return ApiIntent.OperationType.CALLBACK;
        }
        return ApiIntent.OperationType.QUERY; // 默认
    }

    /**
     * 检测业务对象类型
     */
    private ApiIntent.BusinessObject detectBusinessObject(String path, String operationId,
                                                           String summary, String tags) {
        String combined = (path + " " + operationId + " " + summary + " " + tags).toLowerCase();

        if (combined.contains("voucher") || combined.contains("凭证")) {
            return ApiIntent.BusinessObject.ACCOUNTING_VOUCHER;
        } else if (combined.contains("invoice") || combined.contains("发票")) {
            return ApiIntent.BusinessObject.INVOICE;
        } else if (combined.contains("receipt") || combined.contains("收据")) {
            return ApiIntent.BusinessObject.RECEIPT;
        } else if (combined.contains("contract") || combined.contains("合同")) {
            return ApiIntent.BusinessObject.CONTRACT;
        } else if (combined.contains("attachment") || combined.contains("附件")) {
            return ApiIntent.BusinessObject.ATTACHMENT;
        } else if (combined.contains("account") || combined.contains("科目") || combined.contains("余额")) {
            return ApiIntent.BusinessObject.ACCOUNT_BALANCE;
        } else if (combined.contains("salesout") || combined.contains("销售出库") ||
                   combined.contains("sales_out") || combined.contains("出库")) {
            return ApiIntent.BusinessObject.SALES_OUT;
        }
        return ApiIntent.BusinessObject.UNKNOWN;
    }

    /**
     * 检测触发时机
     */
    private ApiIntent.TriggerTiming detectTriggerTiming(java.util.List<OpenApiDefinition.ParameterDefinition> parameters) {
        if (parameters == null) {
            return ApiIntent.TriggerTiming.ON_DEMAND;
        }

        boolean hasDateRange = parameters.stream()
            .anyMatch(p -> p.getName().toLowerCase().contains("date") || p.getName().toLowerCase().contains("time"));

        return hasDateRange ? ApiIntent.TriggerTiming.SCHEDULED : ApiIntent.TriggerTiming.ON_DEMAND;
    }

    /**
     * 生成映射配置
     */
    private MappingConfig generateMappingConfig(OpenApiDefinition definition,
                                                 StandardScenario scenario) {
        return MappingConfig.builder()
            .scenarioCode(scenario.getCode())
            .sourcePath(definition.getPath())
            .httpMethod(definition.getMethod())
            .needsDataMapping(true)
            .needsFileDownload(false) // MVP 暂不支持
            .build();
    }

    /**
     * 场景映射结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ScenarioMapping {
        private OpenApiDefinition apiDefinition;
        private ApiIntent intent;
        private StandardScenario scenario;
        private MappingConfig config;
    }

    /**
     * 映射配置
     */
    @lombok.Data
    @lombok.Builder
    public static class MappingConfig {
        private String scenarioCode;
        private String sourcePath;
        private String httpMethod;
        private boolean needsDataMapping;
        private boolean needsFileDownload;
    }
}
