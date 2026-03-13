// Input: MyBatis-Plus、org.junit、org.mockito、Spring Framework、等
// Output: ErpScenarioServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.dto.request.PageRequest;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.mapper.ErpScenarioMapper;
import com.nexusarchive.mapper.ErpSubInterfaceMapper;
import com.nexusarchive.mapper.SyncHistoryMapper;
import com.nexusarchive.service.erp.ErpChannelService;
import com.nexusarchive.service.erp.ErpSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ERP 场景服务测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ERP场景服务测试")
@Tag("unit")
class ErpScenarioServiceTest {

    @Mock
    private ErpScenarioMapper erpScenarioMapper;

    @Mock
    private ErpConfigMapper erpConfigMapper;

    @Mock
    private ErpAdapterFactory erpAdapterFactory;

    @Mock
    private SyncHistoryMapper syncHistoryMapper;

    @Mock
    private ErpSubInterfaceMapper erpSubInterfaceMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ErpSubInterfaceService erpSubInterfaceService;

    @Mock
    private ErpSyncService erpSyncService;

    @Mock
    @Lazy
    private ErpChannelService erpChannelService;

    @InjectMocks
    private ErpScenarioService erpScenarioService;

    private ErpConfig testConfig;
    private List<ErpScenario> testScenarios;

    @BeforeEach
    void setUp() {
        testConfig = new ErpConfig();
        testConfig.setId(1L);
        testConfig.setName("测试ERP配置");
        testConfig.setErpType("YONSUITE");

        testScenarios = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            ErpScenario scenario = new ErpScenario();
            scenario.setId((long) i);
            scenario.setConfigId(1L);
            scenario.setScenarioKey("SCENARIO_" + i);
            testScenarios.add(scenario);
        }
    }

    @Test
    @DisplayName("分页查询场景 - 第一页")
    void listScenariosByConfigIdPage_FirstPage() {
        // Given
        Long configId = 1L;
        PageRequest request = PageRequest.builder().pageNum(1).pageSize(10).build();

        // 1. listScenariosByConfigId calls selectList
        when(erpScenarioMapper.selectList(any())).thenReturn(testScenarios);

        // 2. selectPage
        Page<ErpScenario> mockPage = new Page<>(1, 10, 25);
        mockPage.setRecords(testScenarios.subList(0, 10));
        when(erpScenarioMapper.selectPage(any(), any())).thenReturn(mockPage);

        // When
        Page<ErpScenario> result = erpScenarioService.listScenariosByConfigIdPage(configId, request);

        // Then
        assertNotNull(result);
        assertEquals(10, result.getRecords().size());
        verify(erpScenarioMapper).selectList(any());
        verify(erpScenarioMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("分页查询场景 - 空结果时初始化默认场景")
    void listScenariosByConfigIdPage_EmptyResult_InitializeDefaults() {
        // Given
        Long configId = 1L;
        PageRequest request = PageRequest.builder().pageNum(1).pageSize(10).build();

        // 1. First selectList returns empty
        when(erpScenarioMapper.selectList(any())).thenReturn(Collections.emptyList()).thenReturn(testScenarios.subList(0, 2));

        // 2. Initialization mocks
        when(erpConfigMapper.selectById(configId)).thenReturn(testConfig);
        when(erpAdapterFactory.isSupported(any())).thenReturn(true);
        ErpAdapter mockAdapter = mock(ErpAdapter.class);
        when(erpAdapterFactory.getAdapter(any())).thenReturn(mockAdapter);
        when(mockAdapter.getAvailableScenarios()).thenReturn(Arrays.asList(new ErpScenario(), new ErpScenario()));

        // 3. selectPage
        Page<ErpScenario> mockPage = new Page<>(1, 10, 2);
        mockPage.setRecords(testScenarios.subList(0, 2));
        when(erpScenarioMapper.selectPage(any(), any())).thenReturn(mockPage);

        // When
        Page<ErpScenario> result = erpScenarioService.listScenariosByConfigIdPage(configId, request);

        // Then
        assertNotNull(result);
        verify(erpScenarioMapper, times(2)).insert(any(ErpScenario.class));
        verify(erpScenarioMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("分页查询场景 - 已有数据不初始化")
    void listScenariosByConfigIdPage_ExistingData_NoInitialization() {
        // Given
        Long configId = 1L;
        PageRequest request = PageRequest.builder().pageNum(1).pageSize(10).build();

        // 1. selectList returns non-empty
        when(erpScenarioMapper.selectList(any())).thenReturn(testScenarios.subList(0, 5));

        // 2. selectPage
        Page<ErpScenario> mockPage = new Page<>(1, 10, 5);
        mockPage.setRecords(testScenarios.subList(0, 5));
        when(erpScenarioMapper.selectPage(any(), any())).thenReturn(mockPage);

        // When
        Page<ErpScenario> result = erpScenarioService.listScenariosByConfigIdPage(configId, request);

        // Then
        assertNotNull(result);
        verify(erpConfigMapper, never()).selectById(any());
        verify(erpScenarioMapper, never()).insert(any(ErpScenario.class));
    }
}
