// nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ErpConfigServiceImpl.java
// Input: ERP type, config ID
// Output: ERP config entities
// Pos: AI 模块 - ERP 配置服务实现
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.service.ErpConfigService;
import com.nexusarchive.util.SM4Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ERP 配置服务实现
 */
@Slf4j
@Service
public class ErpConfigServiceImpl implements ErpConfigService {

    private final ErpConfigMapper erpConfigMapper;

    public ErpConfigServiceImpl(ErpConfigMapper erpConfigMapper) {
        this.erpConfigMapper = erpConfigMapper;
    }

    @Override
    public List<ErpConfig> findConfigsByErpType(String erpType) {
        log.debug("查询 ERP 配置: erpType={}", erpType);

        LambdaQueryWrapper<ErpConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ErpConfig::getErpType, erpType)
                    .eq(ErpConfig::getIsActive, 1)
                    .orderByDesc(ErpConfig::getCreatedTime);

        List<ErpConfig> configs = erpConfigMapper.selectList(queryWrapper);
        log.info("查询到 {} 个 ERP 配置: erpType={}", configs.size(), erpType);
        return configs;
    }

    @Override
    public ErpConfig findById(Long configId) {
        log.debug("查询 ERP 配置: configId={}", configId);
        return erpConfigMapper.selectById(configId);
    }

    @Override
    public List<ErpConfig> getAllConfigs() {
        log.debug("获取所有 ERP 配置");
        return erpConfigMapper.selectList(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(ErpConfig config) {
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

    @Override
    @Transactional(rollbackFor = Exception.class)
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
}
