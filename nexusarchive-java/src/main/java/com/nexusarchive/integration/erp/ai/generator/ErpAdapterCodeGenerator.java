// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/ErpAdapterCodeGenerator.java
// Input: Scenario mappings, ERP info
// Output: GeneratedCode containing Java source
// Pos: AI 模块 - 代码生成器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import com.nexusarchive.integration.erp.ai.mapper.StandardScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 适配器代码生成器
 * <p>
 * 根据场景映射生成适配器 Java 代码
 * MVP 版本：生成基础框架代码
 * </p>
 */
@Slf4j
@Component
public class ErpAdapterCodeGenerator {

    private static final String BASE_PACKAGE = "com.nexusarchive.integration.erp.adapter";

    /**
     * 生成适配器代码
     *
     * @param mappings 场景映射列表
     * @param erpType ERP 类型标识
     * @param erpName ERP 名称
     * @return 生成的代码
     */
    public GeneratedCode generate(List<BusinessSemanticMapper.ScenarioMapping> mappings,
                                  String erpType,
                                  String erpName) {
        // 生成类名
        String className = generateClassName(erpType);
        String packageName = generatePackageName(erpType);

        // 生成适配器主类
        String adapterClass = generateAdapterClass(mappings, className, packageName, erpName);

        // 生成 DTO 类
        List<GeneratedCode.DtoClass> dtoClasses = generateDtoClasses(mappings, packageName);

        // 生成测试类
        String testClass = generateTestClass(className, packageName);

        // 生成配置 SQL
        String configSql = generateConfigSql(erpType, erpName, mappings);

        return GeneratedCode.builder()
            .adapterClass(adapterClass)
            .className(className)
            .packageName(packageName)
            .erpType(erpType)
            .erpName(erpName)
            .dtoClasses(dtoClasses)
            .testClass(testClass)
            .configSql(configSql)
            .mappings(mappings)
            .build();
    }

    /**
     * 生成类名
     */
    private String generateClassName(String erpType) {
        // 将 erpType 转换为 PascalCase
        // 例如: "kingdee" -> "Kingdee"
        return erpType.substring(0, 1).toUpperCase() + erpType.substring(1).toLowerCase() + "ErpAdapter";
    }

    /**
     * 生成包名
     */
    private String generatePackageName(String erpType) {
        return BASE_PACKAGE + "." + erpType.toLowerCase();
    }

    /**
     * 生成适配器主类
     */
    private String generateAdapterClass(List<BusinessSemanticMapper.ScenarioMapping> mappings,
                                        String className,
                                        String packageName,
                                        String erpName) {
        // 获取支持的场景（去重）
        String scenarios = mappings.stream()
            .map(m -> "\"" + m.getScenario().getCode() + "\"")
            .distinct()
            .collect(Collectors.joining(", "));

        String erpId = erpName.toLowerCase().replace(" ", "-");

        return String.format("""
            // %s/%s.java
            // 自动生成的 ERP 适配器
            // 生成时间: %s
            // 注意: 这是 AI 生成的代码，请人工审核后再部署

            package %s;

            import com.nexusarchive.integration.erp.adapter.ErpAdapter;
            import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
            import com.nexusarchive.integration.erp.dto.AttachmentDTO;
            import com.nexusarchive.integration.erp.dto.ConnectionTestResult;
            import com.nexusarchive.integration.erp.dto.ErpConfig;
            import com.nexusarchive.integration.erp.dto.VoucherDTO;
            import lombok.extern.slf4j.Slf4j;
            import org.springframework.stereotype.Service;

            import java.time.LocalDate;
            import java.util.List;

            /**
             * %s
             * <p>
             * AI 自动生成的 ERP 适配器
             * </p>
             */
            @Slf4j
            @Service
            @ErpAdapterAnnotation(
                identifier = "%s",
                name = "%s",
                supportedScenarios = {%s}
            )
            public class %s implements ErpAdapter {

                @Override
                public String getIdentifier() {
                    return "%s";
                }

                @Override
                public String getName() {
                    return "%s";
                }

                @Override
                public ConnectionTestResult testConnection(ErpConfig config) {
                    // TODO: 实现连接测试逻辑
                    log.info("测试连接: {}", config.getBaseUrl());
                    return ConnectionTestResult.success("连接成功", 0L);
                }

                @Override
                public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
                    // TODO: 实现凭证同步逻辑
                    log.info("同步凭证: {} - {}", startDate, endDate);
                    return List.of();
                }

                @Override
                public VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo) {
                    // TODO: 实现凭证详情获取逻辑
                    return null;
                }

                @Override
                public List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo) {
                    // TODO: 实现附件列表获取逻辑
                    return List.of();
                }
            }
            """,
            packageName.replace('.', '/'),
            className,
            java.time.LocalDateTime.now(),
            packageName,
            erpName,
            erpId,
            erpName,
            scenarios,
            className,
            erpId,
            erpName
        );
    }

    /**
     * 生成 DTO 类
     */
    private List<GeneratedCode.DtoClass> generateDtoClasses(List<BusinessSemanticMapper.ScenarioMapping> mappings,
                                                            String packageName) {
        // MVP: 生成基础的请求响应 DTO
        String requestDtoCode = String.format("""
            package %s.dto;

            import lombok.Data;

            @Data
            public class ApiRequest {
                private String startDate;
                private String endDate;
                private Integer pageSize = 100;
                private Integer pageNo = 1;
            }
            """, packageName);

        String responseDtoCode = String.format("""
            package %s.dto;

            import lombok.Data;
            import java.util.List;

            @Data
            public class ApiResponse<T> {
                private boolean success;
                private String message;
                private T data;
            }
            """, packageName);

        return List.of(
            GeneratedCode.DtoClass.builder()
                .className("ApiRequest")
                .packageName(packageName + ".dto")
                .code(requestDtoCode)
                .build(),
            GeneratedCode.DtoClass.builder()
                .className("ApiResponse")
                .packageName(packageName + ".dto")
                .code(responseDtoCode)
                .build()
        );
    }

    /**
     * 生成测试类
     */
    private String generateTestClass(String className, String packageName) {
        return String.format("""
            // %s/%sTest.java
            // 自动生成的测试类

            package %s;

            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;

            class %sTest {

                @Test
                void shouldTestConnection() {
                    // TODO: 实现测试逻辑
                    assertTrue(true);
                }

                @Test
                void shouldSyncVouchers() {
                    // TODO: 实现测试逻辑
                    assertTrue(true);
                }
            }
            """,
            packageName.replace('.', '/'),
            className,
            packageName,
            className
        );
    }

    /**
     * 生成配置 SQL
     */
    private String generateConfigSql(String erpType, String erpName,
                                     List<BusinessSemanticMapper.ScenarioMapping> mappings) {
        String scenarioSql = "";
        if (mappings != null && !mappings.isEmpty()) {
            scenarioSql = mappings.stream()
                .filter(m -> m != null && m.getScenario() != null)
                .map(m -> String.format("INSERT INTO sys_erp_adapter_scenario (adapter_id, scenario_code) VALUES ('%s', '%s');",
                    erpType.toLowerCase().replace(" ", "-"),
                    m.getScenario().getCode()))
                .collect(Collectors.joining("\n"));
        }

        return String.format("""
            -- ERP 适配器配置
            -- ERP: %s (%s)

            INSERT INTO sys_erp_adapter (adapter_id, adapter_name, erp_type, base_url, enabled)
            VALUES ('%s', '%s', '%s', 'https://api.example.com', true);

            -- 配置支持的场景
            %s
            """,
            erpName,
            erpType,
            erpType.toLowerCase().replace(" ", "-"),
            erpName,
            erpType.toLowerCase(),
            scenarioSql
        );
    }
}
