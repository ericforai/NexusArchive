// nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ErpConfigServiceImpl.java
// Input: ERP type, config ID
// Output: ERP config entities / DTOs
// Pos: AI 模块 - ERP 配置服务实现
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.ErpConfigDto;
import com.nexusarchive.dto.ErpConfigApiDtoBuilder;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.service.ErpConfigService;
import com.nexusarchive.util.SM4Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 配置服务实现
 * 缓存策略: 使用 erpConfig 缓存空间，TTL 30 分钟
 */
@Slf4j
@Service
public class ErpConfigServiceImpl implements ErpConfigService {

    private final ErpConfigMapper erpConfigMapper;

    public ErpConfigServiceImpl(ErpConfigMapper erpConfigMapper) {
        this.erpConfigMapper = erpConfigMapper;
    }

    // ========== API 方法（返回 DTO，已清理敏感信息） ==========

    /**
     * 获取所有配置（API 响应，已清理敏感信息）
     * 缓存键: erpConfig:all:dto
     */
    @Override
    @Cacheable(value = "erpConfig", key = "'all:dto'")
    public List<ErpConfigDto> getConfigs() {
        log.debug("获取所有 ERP 配置（DTO）");
        List<ErpConfig> configs = erpConfigMapper.selectList(null);
        return configs.stream()
                .map(ErpConfigApiDtoBuilder::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ERP 类型查询配置列表（API 响应，已清理敏感信息）
     * 缓存键: erpConfig:type:{erpType}:dto
     */
    @Override
    @Cacheable(value = "erpConfig", key = "'type:' + #erpType + ':dto'")
    public List<ErpConfigDto> getConfigsByErpType(String erpType) {
        log.debug("查询 ERP 配置（DTO）: erpType={}", erpType);

        LambdaQueryWrapper<ErpConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ErpConfig::getErpType, erpType)
                    .eq(ErpConfig::getIsActive, 1)
                    .orderByDesc(ErpConfig::getCreatedTime);

        List<ErpConfig> configs = erpConfigMapper.selectList(queryWrapper);
        List<ErpConfigDto> result = configs.stream()
                .map(ErpConfigApiDtoBuilder::toDto)
                .collect(Collectors.toList());
        log.info("查询到 {} 个 ERP 配置（DTO）: erpType={}", result.size(), erpType);
        return result;
    }

    /**
     * 根据 ID 查询配置（API 响应，已清理敏感信息）
     * 缓存键: erpConfig:id:{configId}:dto
     */
    @Override
    @Cacheable(value = "erpConfig", key = "'id:' + #configId + ':dto'")
    public ErpConfigDto getConfig(Long configId) {
        log.debug("查询 ERP 配置（DTO）: configId={}", configId);
        ErpConfig config = erpConfigMapper.selectById(configId);
        return ErpConfigApiDtoBuilder.toDto(config);
    }

    // ========== 内部方法（返回 Entity，包含敏感信息） ==========

    /**
     * 根据 ERP 类型查询配置列表
     * 缓存键: erpConfig:type:{erpType}
     */
    @Override
    @Cacheable(value = "erpConfig", key = "'type:' + #erpType")
    public List<ErpConfig> findConfigsByErpType(String erpType) {
        log.debug("查询 ERP 配置: erpType={}", erpType);

        LambdaQueryWrapper<ErpConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ErpConfig::getErpType, erpType)
                    .eq(ErpConfig::getIsActive, 1)
                    .orderByDesc(ErpConfig::getCreatedTime);

        List<ErpConfig> configs = erpConfigMapper.selectList(queryWrapper);
        // 清除敏感信息
        configs.forEach(this::sanitizeSensitiveFields);
        log.info("查询到 {} 个 ERP 配置: erpType={}", configs.size(), erpType);
        return configs;
    }

    /**
     * 根据 ID 查询配置
     * 缓存键: erpConfig:id:{configId}
     */
    @Override
    @Cacheable(value = "erpConfig", key = "'id:' + #configId")
    public ErpConfig findById(Long configId) {
        log.debug("查询 ERP 配置: configId={}", configId);
        ErpConfig config = erpConfigMapper.selectById(configId);
        // 清除敏感信息
        if (config != null) {
            sanitizeSensitiveFields(config);
        }
        return config;
    }

    /**
     * 根据 ID 查询完整配置（包含敏感信息）
     * 用于内部调用（如适配器连接测试），不清除敏感字段
     * 缓存键: erpConfig:internal:id:{configId}
     */
    @Override
    public ErpConfig getByIdForInternalUse(Long configId) {
        log.debug("查询完整 ERP 配置（内部使用）: configId={}", configId);
        ErpConfig config = erpConfigMapper.selectById(configId);
        // 不清除敏感信息，直接返回
        return config;
    }

    /**
     * 获取所有配置
     * 缓存键: erpConfig:all
     */
    @Override
    @Cacheable(value = "erpConfig", key = "'all'")
    public List<ErpConfig> getAllConfigs() {
        log.debug("获取所有 ERP 配置");
        List<ErpConfig> configs = erpConfigMapper.selectList(null);
        // 清除敏感信息
        configs.forEach(this::sanitizeSensitiveFields);
        return configs;
    }

    /**
     * 保存或更新配置
     * 清除缓存: erpConfig:all (全量清除)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "erpConfig", allEntries = true)
    public void saveConfig(ErpConfig config) {
        // 输入验证
        if (config == null) {
            throw new IllegalArgumentException("配置不能为空");
        }
        if (config.getErpType() == null || config.getErpType().isBlank()) {
            throw new IllegalArgumentException("ERP类型不能为空");
        }

        log.info("保存 ERP 配置: id={}, name={}, erpType={}",
                config.getId(), config.getName(), config.getErpType());

        // 加密敏感信息
        if (config.getConfigJson() != null && !config.getConfigJson().isEmpty()) {
            try {
                JSONObject json = JSONUtil.parseObj(config.getConfigJson());
                encryptSecretIfNeeded(json, "appSecret");
                encryptSecretIfNeeded(json, "clientSecret");
                config.setConfigJson(json.toString());
            } catch (Exception e) {
                log.warn("配置 JSON 解析失败，跳过加密: {}", e.getMessage());
            }
        }

        if (config.getId() == null) {
            erpConfigMapper.insert(config);
            log.info("新增 ERP 配置成功: id={}", config.getId());
        } else {
            erpConfigMapper.updateById(config);
            log.info("更新 ERP 配置成功: id={}", config.getId());
        }
    }

    /**
     * 删除配置
     * 清除缓存: erpConfig:all
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "erpConfig", allEntries = true)
    public void deleteConfig(Long id) {
        log.info("删除 ERP 配置: id={}", id);
        erpConfigMapper.deleteById(id);
    }

    @Override
    public Long countConfigs() {
        return erpConfigMapper.selectCount(null);
    }

    @Override
    public Long countActiveConfigs() {
        return erpConfigMapper.selectCount(
                new LambdaQueryWrapper<ErpConfig>()
                        .eq(ErpConfig::getIsActive, 1));
    }

    @Override
    public String getFondsCodeByAccbook(String accbookCode) {
        if (accbookCode == null || accbookCode.isEmpty()) {
            return accbookCode;
        }
        java.util.Map<String, String> mapping = getAccbookFondsMapping();
        return mapping.getOrDefault(accbookCode, accbookCode);
    }

    @Override
    public java.util.Map<String, String> getAccbookFondsMapping() {
        java.util.Map<String, String> result = new java.util.HashMap<>();

        // 查询所有激活的 ERP 配置
        List<ErpConfig> configs = erpConfigMapper.selectList(
                new LambdaQueryWrapper<ErpConfig>().eq(ErpConfig::getIsActive, 1));

        for (ErpConfig config : configs) {
            if (config.getAccbookMapping() != null && !config.getAccbookMapping().isEmpty()) {
                try {
                    JSONObject mappingJson = JSONUtil.parseObj(config.getAccbookMapping());
                    mappingJson.forEach((key, value) -> {
                        if (value != null) {
                            result.put(key, value.toString());
                        }
                    });
                } catch (Exception e) {
                    log.warn("解析 accbookMapping 失败: configId={}, error={}", config.getId(), e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * 加密敏感字段（如果尚未加密）
     *
     * @param json JSON 对象
     * @param key 字段名
     */
    private void encryptSecretIfNeeded(JSONObject json, String key) {
        String secret = json.getStr(key);
        if (secret == null || secret.isEmpty()) {
            return;
        }

        // 检查是否有加密标记
        String encryptedFlag = json.getStr(key + "_encrypted");
        if ("true".equals(encryptedFlag)) {
            log.debug("密钥已加密，跳过: key={}", key);
            return;
        }

        // 加密并设置标记
        String encrypted = SM4Utils.encrypt(secret);
        json.set(key, encrypted);
        json.set(key + "_encrypted", "true");
        log.info("密钥已加密: key={}", key);
    }

    /**
     * 清除敏感字段（防止通过 API 暴露）
     *
     * @param config ERP 配置对象
     */
    private void sanitizeSensitiveFields(ErpConfig config) {
        if (config == null || config.getConfigJson() == null) {
            return;
        }
        try {
            JSONObject json = JSONUtil.parseObj(config.getConfigJson());
            json.remove("appSecret");
            json.remove("clientSecret");
            json.remove("appSecret_encrypted");
            json.remove("clientSecret_encrypted");
            config.setConfigJson(json.toString());
            log.debug("已清除敏感字段: configId={}", config.getId());
        } catch (Exception e) {
            log.warn("清除敏感字段失败: {}", e.getMessage());
        }
    }
}
