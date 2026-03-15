// Input: JUnit 5, Mockito, Java 标准库
// Output: LegacyImportOrchestratorSmokeTest 测试用例类
// Pos: 后端测试用例 - 简单验证测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.request.FieldMappingConfig;
import com.nexusarchive.dto.request.ImportResult;
import com.nexusarchive.entity.LegacyImportTask;
import com.nexusarchive.mapper.LegacyImportTaskMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.FondsAutoCreationService;
import com.nexusarchive.service.ImportValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 历史数据导入流程编排器简单验证测试
 *
 * 用于验证测试基础设施和基本功能
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("历史数据导入流程编排器简单验证测试")
class LegacyImportOrchestratorSmokeTest {

    @Mock
    private LegacyImportTaskMapper importTaskMapper;

    @Mock
    private ImportValidationService validationService;

    @Mock
    private FondsAutoCreationService fondsAutoCreationService;

    @Mock
    private LegacyDataConverter dataConverter;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private MultipartFile mockFile;

    private ObjectMapper objectMapper;
    private LegacyImportOrchestrator orchestrator;
    private FieldMappingConfig mappingConfig;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orchestrator = new LegacyImportOrchestrator(
                importTaskMapper,
                validationService,
                fondsAutoCreationService,
                dataConverter,
                auditLogService,
                objectMapper
        );
        mappingConfig = new FieldMappingConfig();
    }

    @Test
    @DisplayName("应该成功创建编排器实例")
    void shouldSuccessfullyCreateOrchestrator() {
        // Then
        assertThat(orchestrator).isNotNull();
    }

    @Test
    @DisplayName("应该成功设置映射配置")
    void shouldSuccessfullySetMappingConfig() {
        // Given
        mappingConfig.addMapping("全宗号", "fonds_no");

        // Then
        assertThat(mappingConfig).isNotNull();
        assertThat(mappingConfig.getMappedField("全宗号")).isEqualTo("fonds_no");
    }
}
