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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ERP 场景服务测试
 * <p>
 * 测试分页查询功能
 * </p>
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
        // 初始化测试 ERP 配置
        testConfig = new ErpConfig();
        testConfig.setId(1L);
        testConfig.setName("测试ERP配置");
        testConfig.setErpType("YONSUITE");
        testConfig.setIsActive(1);
        testConfig.setCreatedTime(LocalDateTime.now());

        // 初始化测试场景列表
        testScenarios = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            ErpScenario scenario = new ErpScenario();
            scenario.setId((long) i);
            scenario.setConfigId(1L);
            scenario.setScenarioKey("SCENARIO_" + i);
            scenario.setName("场景" + i);
            scenario.setDescription("测试场景描述" + i);
            scenario.setIsActive(true);
            scenario.setSyncStrategy("MANUAL");
            scenario.setLastSyncStatus("NONE");
            scenario.setCreatedTime(LocalDateTime.now());
            scenario.setLastModifiedTime(LocalDateTime.now());
            testScenarios.add(scenario);
        }
    }

    @Test
    @DisplayName("分页查询场景 - 第一页")
    void listScenariosByConfigIdPage_FirstPage() {
        // Given
        Long configId = 1L;
        PageRequest request = PageRequest.builder()
                .pageNum(1)
                .pageSize(10)
                .build();

        // Mock 返回总记录数
        when(erpScenarioMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(25L);

        // Mock 返回分页结果
        Page<ErpScenario> mockPage = new Page<>(1, 10, 25);
        mockPage.setRecords(testScenarios.subList(0, 10));
        when(erpScenarioMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // When
        Page<ErpScenario> result = erpScenarioService.listScenariosByConfigIdPage(configId, request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(25, result.getTotal());
        assertEquals(10, result.getRecords().size());
        assertEquals("SCENARIO_1", result.getRecords().get(0).getScenarioKey());
        verify(erpScenarioMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
        verify(erpScenarioMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询场景 - 第二页")
    void listScenariosByConfigIdPage_SecondPage() {
        // Given
        Long configId = 1L;
        PageRequest request = PageRequest.builder()
                .pageNum(2)
                .pageSize(10)
                .build();

        when(erpScenarioMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(25L);

        Page<ErpScenario> mockPage = new Page<>(2, 10, 25);
        mockPage.setRecords(testScenarios.subList(10, 20));
        when(erpScenarioMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // When
        Page<ErpScenario> result = erpScenarioService.listScenariosByConfigIdPage(configId, request);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(10, result.getRecords().size());
        assertEquals("SCENARIO_11", result.getRecords().get(0).getScenarioKey());
    }

    @Test
    @DisplayName("分页查询场景 - 空结果时初始化默认场景")
    void listScenariosByConfigIdPage_EmptyResult_InitializeDefaults() {
        // Given
        Long configId = 1L;
        PageRequest request = PageRequest.builder()
                .pageNum(1)
                .pageSize(10)
                .build();

        // Mock 没有记录
        when(erpScenarioMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(erpConfigMapper.selectById(configId)).thenReturn(testConfig);

        // Mock 适配器返回默认场景
        ErpAdapter mockAdapter = mock(ErpAdapter.class);
        List<ErpScenario> defaultScenarios = Arrays.asList(
                createDefaultScenario("VOUCHER_SYNC", "凭证同步"),
                createDefaultScenario("INVENTORY_SYNC", "库存同步")
        );
        when(mockAdapter.getAvailableScenarios()).thenReturn(defaultScenarios);
        when(erpAdapterFactory.isSupported(any())).thenReturn(true);
        when(erpAdapterFactory.getAdapter(any())).thenReturn(mockAdapter);

        // Mock 插入成功
        when(erpScenarioMapper.insert(any(ErpScenario.class))).thenReturn(1);

        // Mock 分页查询返回初始化后的数据
        Page<ErpScenario> mockPage = new Page<>(1, 10, 2);
        mockPage.setRecords(defaultScenarios);
        when(erpScenarioMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // When
        Page<ErpScenario> result = erpScenarioService.listScenariosByConfigIdPage(configId, request);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getRecords().size());
        verify(erpScenarioMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
        verify(erpScenarioMapper, times(2)).insert(any(ErpScenario.class)); // 插入2个默认场景
        verify(erpScenarioMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询场景 - 使用默认分页参数")
    void listScenariosByConfigIdPage_DefaultParameters() {
        // Given
        Long configId = 1L;
        PageRequest request = PageRequest.builder().build(); // 使用默认值

        when(erpScenarioMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(25L);

        Page<ErpScenario> mockPage = new Page<>(1, 20, 25); // 默认 pageSize=20
        mockPage.setRecords(testScenarios.subList(0, 20));
        when(erpScenarioMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // When
        Page<ErpScenario> result = erpScenarioService.listScenariosByConfigIdPage(configId, request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCurrent()); // 默认 pageNum=1
        assertEquals(20, result.getSize()); // 默认 pageSize=20
    }

    @Test
    @DisplayName("分页查询场景 - 最大页大小限制")
    void listScenariosByConfigIdPage_MaxPageSize() {
        // Given
        Long configId = 1L;
        PageRequest request = PageRequest.builder()
                .pageNum(1)
                .pageSize(100) // 最大值
                .build();

        when(erpScenarioMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(25L);

        Page<ErpScenario> mockPage = new Page<>(1, 100, 25);
        mockPage.setRecords(testScenarios);
        when(erpScenarioMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // When
        Page<ErpScenario> result = erpScenarioService.listScenariosByConfigIdPage(configId, request);

        // Then
        assertNotNull(result);
        assertEquals(100, result.getSize());
    }

    @Test
    @DisplayName("分页查询场景 - 已有数据不初始化")
    void listScenariosByConfigIdPage_ExistingData_NoInitialization() {
        // Given
        Long configId = 1L;
        PageRequest request = PageRequest.builder()
                .pageNum(1)
                .pageSize(10)
                .build();

        // Mock 已有记录
        when(erpScenarioMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);

        Page<ErpScenario> mockPage = new Page<>(1, 10, 10);
        mockPage.setRecords(testScenarios.subList(0, 10));
        when(erpScenarioMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // When
        Page<ErpScenario> result = erpScenarioService.listScenariosByConfigIdPage(configId, request);

        // Then
        assertNotNull(result);
        // 验证没有调用初始化相关方法
        verify(erpConfigMapper, never()).selectById(any());
        verify(erpAdapterFactory, never()).getAdapter(any());
        verify(erpScenarioMapper, never()).insert(any(ErpScenario.class));
    }

    /**
     * 创建默认测试场景
     */
    private ErpScenario createDefaultScenario(String key, String name) {
        ErpScenario scenario = new ErpScenario();
        scenario.setScenarioKey(key);
        scenario.setName(name);
        scenario.setDescription("默认" + name + "场景");
        scenario.setIsActive(false);
        scenario.setSyncStrategy("MANUAL");
        scenario.setLastSyncStatus("NONE");
        scenario.setCreatedTime(LocalDateTime.now());
        scenario.setLastModifiedTime(LocalDateTime.now());
        return scenario;
    }
}
