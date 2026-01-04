// Input: MyBatis-Plus、org.junit、org.mockito、Spring Security、等
// Output: ErpConfigServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.service.impl.ErpConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ERP配置服务测试")
class ErpConfigServiceTest {

    @Mock
    private ErpConfigMapper erpConfigMapper;

    @InjectMocks
    private ErpConfigServiceImpl erpConfigService;

    private ErpConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new ErpConfig();
        testConfig.setId(1L);
        testConfig.setName("测试ERP配置");
        testConfig.setErpType("YONSUITE");
        testConfig.setIsActive(1);
        testConfig.setCreatedTime(LocalDateTime.now());
        testConfig.setLastModifiedTime(LocalDateTime.now());
        testConfig.setOrgId("org-001");
    }

    @Test
    @DisplayName("获取所有配置 - 成功")
    void getAllConfigs_Success() {
        // Given
        List<ErpConfig> expectedConfigs = Arrays.asList(testConfig);
        when(erpConfigMapper.selectList(null)).thenReturn(expectedConfigs);

        // When
        List<ErpConfig> result = erpConfigService.getAllConfigs();

        // Then
        assertEquals(1, result.size());
        assertEquals(testConfig.getName(), result.get(0).getName());
        verify(erpConfigMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("根据ERP类型查询配置 - 成功")
    void findConfigsByErpType_Success() {
        // Given
        String erpType = "YONSUITE";
        List<ErpConfig> expectedConfigs = Arrays.asList(testConfig);
        when(erpConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expectedConfigs);

        // When
        List<ErpConfig> result = erpConfigService.findConfigsByErpType(erpType);

        // Then
        assertEquals(1, result.size());
        assertEquals(testConfig.getErpType(), result.get(0).getErpType());
        verify(erpConfigMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("根据ID查询配置 - 成功")
    void findById_Success() {
        // Given
        Long configId = 1L;
        when(erpConfigMapper.selectById(configId)).thenReturn(testConfig);

        // When
        ErpConfig result = erpConfigService.findById(configId);

        // Then
        assertNotNull(result);
        assertEquals(configId, result.getId());
        verify(erpConfigMapper, times(1)).selectById(configId);
    }

    @Test
    @DisplayName("根据ID查询配置 - 不存在")
    void findById_NotFound() {
        // Given
        Long configId = 999L;
        when(erpConfigMapper.selectById(configId)).thenReturn(null);

        // When
        ErpConfig result = erpConfigService.findById(configId);

        // Then
        assertNull(result);
        verify(erpConfigMapper, times(1)).selectById(configId);
    }

    @Test
    @DisplayName("保存配置 - 新增（无敏感信息）")
    void saveConfig_InsertWithoutSecrets() {
        // Given
        ErpConfig newConfig = new ErpConfig();
        newConfig.setName("新配置");
        newConfig.setErpType("KINGDEE");
        newConfig.setConfigJson("{\"host\":\"localhost\"}");

        when(erpConfigMapper.insert(any(ErpConfig.class))).thenReturn(1);

        // When
        erpConfigService.saveConfig(newConfig);

        // Then
        verify(erpConfigMapper, times(1)).insert(any(ErpConfig.class));
        verify(erpConfigMapper, never()).updateById(any(ErpConfig.class));
    }

    @Test
    @DisplayName("保存配置 - 更新（带敏感信息加密）")
    void saveConfig_UpdateWithSecretEncryption() {
        // Given
        ErpConfig existingConfig = new ErpConfig();
        existingConfig.setId(1L);
        existingConfig.setName("更新配置");
        existingConfig.setErpType("YONSUITE");

        // 构建包含敏感信息的JSON
        JSONObject configJson = new JSONObject();
        configJson.set("host", "https://example.com");
        configJson.set("appKey", "test-key");
        configJson.set("appSecret", "plain-secret");  // 明文密钥
        configJson.set("clientSecret", "plain-client-secret");  // 明文密钥
        existingConfig.setConfigJson(configJson.toString());

        when(erpConfigMapper.updateById(any(ErpConfig.class))).thenReturn(1);

        // When
        erpConfigService.saveConfig(existingConfig);

        // Then
        ArgumentCaptor<ErpConfig> captor = ArgumentCaptor.forClass(ErpConfig.class);
        verify(erpConfigMapper, times(1)).updateById(captor.capture());

        ErpConfig savedConfig = captor.getValue();
        JSONObject savedJson = new JSONObject(savedConfig.getConfigJson());

        // 验证敏感字段已被加密
        assertNotEquals("plain-secret", savedJson.getStr("appSecret"));
        assertNotEquals("plain-client-secret", savedJson.getStr("clientSecret"));

        // 验证加密标记已设置
        assertEquals("true", savedJson.getStr("appSecret_encrypted"));
        assertEquals("true", savedJson.getStr("clientSecret_encrypted"));

        verify(erpConfigMapper, never()).insert(any(ErpConfig.class));
    }

    @Test
    @DisplayName("保存配置 - 已加密的密钥不应重复加密")
    void saveConfig_SkipAlreadyEncryptedSecrets() {
        // Given
        ErpConfig config = new ErpConfig();
        config.setId(1L);
        config.setName("测试配置");

        // 模拟已加密的JSON（包含加密标记）
        JSONObject configJson = new JSONObject();
        configJson.set("appSecret", "encrypted-secret-value");
        configJson.set("appSecret_encrypted", "true");  // 已加密标记
        config.setConfigJson(configJson.toString());

        when(erpConfigMapper.updateById(any(ErpConfig.class))).thenReturn(1);

        // When
        erpConfigService.saveConfig(config);

        // Then
        ArgumentCaptor<ErpConfig> captor = ArgumentCaptor.forClass(ErpConfig.class);
        verify(erpConfigMapper, times(1)).updateById(captor.capture());

        ErpConfig savedConfig = captor.getValue();
        JSONObject savedJson = new JSONObject(savedConfig.getConfigJson());

        // 验证密钥值未改变（未重复加密）
        assertEquals("encrypted-secret-value", savedJson.getStr("appSecret"));
        assertEquals("true", savedJson.getStr("appSecret_encrypted"));
    }

    @Test
    @DisplayName("删除配置 - 成功")
    void deleteConfig_Success() {
        // Given
        Long configId = 1L;
        when(erpConfigMapper.deleteById(configId)).thenReturn(1);

        // When
        erpConfigService.deleteConfig(configId);

        // Then
        verify(erpConfigMapper, times(1)).deleteById(configId);
    }

    @Test
    @DisplayName("统计配置总数 - 成功")
    void countConfigs_Success() {
        // Given
        Long expectedCount = 5L;
        when(erpConfigMapper.selectCount(null)).thenReturn(expectedCount);

        // When
        Long result = erpConfigService.countConfigs();

        // Then
        assertEquals(expectedCount, result);
        verify(erpConfigMapper, times(1)).selectCount(null);
    }

    @Test
    @DisplayName("统计活跃配置数 - 成功")
    void countActiveConfigs_Success() {
        // Given
        Long expectedCount = 3L;
        when(erpConfigMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(expectedCount);

        // When
        Long result = erpConfigService.countActiveConfigs();

        // Then
        assertEquals(expectedCount, result);
        verify(erpConfigMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("保存配置 - 无效的JSON格式")
    void saveConfig_InvalidJsonFormat() {
        // Given
        ErpConfig config = new ErpConfig();
        config.setId(1L);
        config.setName("测试配置");
        config.setConfigJson("invalid-json-string");

        when(erpConfigMapper.updateById(any(ErpConfig.class))).thenReturn(1);

        // When - 不应抛出异常
        erpConfigService.saveConfig(config);

        // Then - 仍应保存，只是跳过加密处理
        verify(erpConfigMapper, times(1)).updateById(any(ErpConfig.class));
    }

    @Test
    @DisplayName("保存配置 - 空配置JSON")
    void saveConfig_EmptyConfigJson() {
        // Given
        ErpConfig config = new ErpConfig();
        config.setId(1L);
        config.setName("测试配置");
        config.setConfigJson("");

        when(erpConfigMapper.updateById(any(ErpConfig.class))).thenReturn(1);

        // When
        erpConfigService.saveConfig(config);

        // Then
        verify(erpConfigMapper, times(1)).updateById(any(ErpConfig.class));
    }

    @Test
    @DisplayName("保存配置 - null配置JSON")
    void saveConfig_NullConfigJson() {
        // Given
        ErpConfig config = new ErpConfig();
        config.setId(1L);
        config.setName("测试配置");
        config.setConfigJson(null);

        when(erpConfigMapper.updateById(any(ErpConfig.class))).thenReturn(1);

        // When
        erpConfigService.saveConfig(config);

        // Then
        verify(erpConfigMapper, times(1)).updateById(any(ErpConfig.class));
    }
}
