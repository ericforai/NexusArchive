// nexusarchive-java/src/test/java/com/nexusarchive/integration/erp/ai/generator/ErpAdapterCodeGeneratorTest.java
// Input: -
// Output: Test results
// Pos: AI 模块 - 代码生成器测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import com.nexusarchive.integration.erp.ai.mapper.StandardScenario;
import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErpAdapterCodeGenerator 测试
 */
class ErpAdapterCodeGeneratorTest {

    private ErpAdapterCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ErpAdapterCodeGenerator();
    }

    @Test
    void shouldGenerateAdapterClass() {
        // Given
        BusinessSemanticMapper mapper = new BusinessSemanticMapper();
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/vouchers")
            .method("get")
            .operationId("listVouchers")
            .summary("获取凭证列表")
            .tags(List.of("vouchers"))
            .build();

        BusinessSemanticMapper.ScenarioMapping mapping = mapper.mapToScenario(definition);
        List<BusinessSemanticMapper.ScenarioMapping> mappings = List.of(mapping);

        // When
        GeneratedCode code = generator.generate(mappings, "testerp", "Test ERP");

        // Then
        assertNotNull(code.getAdapterClass());
        assertTrue(code.getAdapterClass().contains("public class TesterpErpAdapter"));
        assertTrue(code.getAdapterClass().contains("@ErpAdapter"));
        assertEquals("TesterpErpAdapter", code.getClassName());
        assertEquals("com.nexusarchive.integration.erp.adapter.testerp", code.getPackageName());
    }

    @Test
    void shouldGenerateDtoClasses() {
        // Given
        BusinessSemanticMapper mapper = new BusinessSemanticMapper();
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/vouchers")
            .method("get")
            .operationId("listVouchers")
            .build();

        BusinessSemanticMapper.ScenarioMapping mapping = mapper.mapToScenario(definition);
        List<BusinessSemanticMapper.ScenarioMapping> mappings = List.of(mapping);

        // When
        GeneratedCode code = generator.generate(mappings, "testerp", "Test ERP");

        // Then
        assertNotNull(code.getDtoClasses());
        assertFalse(code.getDtoClasses().isEmpty());
        assertTrue(code.getDtoClasses().stream().anyMatch(dto -> dto.getClassName().equals("ApiRequest")));
        assertTrue(code.getDtoClasses().stream().anyMatch(dto -> dto.getClassName().equals("ApiResponse")));
    }

    @Test
    void shouldGenerateTestClass() {
        // Given
        List<BusinessSemanticMapper.ScenarioMapping> mappings = List.of();

        // When
        GeneratedCode code = generator.generate(mappings, "testerp", "Test ERP");

        // Then
        assertNotNull(code.getTestClass());
        assertTrue(code.getTestClass().contains("class TesterpErpAdapterTest"));
    }

    @Test
    void shouldGenerateConfigSql() {
        // Given
        BusinessSemanticMapper mapper = new BusinessSemanticMapper();
        OpenApiDefinition definition = OpenApiDefinition.builder()
            .path("/api/v1/vouchers")
            .method("get")
            .operationId("listVouchers")
            .build();

        BusinessSemanticMapper.ScenarioMapping mapping = mapper.mapToScenario(definition);
        List<BusinessSemanticMapper.ScenarioMapping> mappings = List.of(mapping);

        // When
        GeneratedCode code = generator.generate(mappings, "testerp", "Test ERP");

        // Then
        assertNotNull(code.getConfigSql());
        assertTrue(code.getConfigSql().contains("INSERT INTO sys_erp_adapter"));
        assertTrue(code.getConfigSql().contains("INSERT INTO sys_erp_adapter_scenario"));
    }
}
