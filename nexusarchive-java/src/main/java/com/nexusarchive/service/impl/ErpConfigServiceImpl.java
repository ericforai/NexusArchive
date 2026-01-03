// nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ErpConfigServiceImpl.java
// Input: ERP type, config ID
// Output: ERP config entities
// Pos: AI 模块 - ERP 配置服务实现
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.service.ErpConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
