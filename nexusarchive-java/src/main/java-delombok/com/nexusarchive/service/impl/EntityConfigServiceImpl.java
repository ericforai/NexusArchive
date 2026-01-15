// Input: EntityConfigService、EntityConfigMapper
// Output: EntityConfigServiceImpl 类
// Pos: 业务服务层实现
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexusarchive.entity.EntityConfig;
import com.nexusarchive.mapper.EntityConfigMapper;
import com.nexusarchive.service.EntityConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 法人配置服务实现类
 * 缓存策略: 使用 entityConfig 缓存空间，TTL 30 分钟
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityConfigServiceImpl extends ServiceImpl<EntityConfigMapper, EntityConfig> implements EntityConfigService {
    
    private final EntityConfigMapper configMapper;
    
    /**
     * 获取指定法人的所有配置
     * 缓存键: entityConfig:entity:{entityId}
     */
    @Override
    @Cacheable(value = "entityConfig", key = "'entity:' + #entityId")
    public List<EntityConfig> getConfigsByEntityId(String entityId) {
        return configMapper.findByEntityId(entityId);
    }
    
    @Override
    public List<EntityConfig> getConfigsByEntityIdAndType(String entityId, String configType) {
        return configMapper.findByEntityIdAndType(entityId, configType);
    }
    
    /**
     * 获取指定法人的配置（按类型分组）
     * 缓存键: entityConfig:grouped:{entityId}
     */
    @Override
    @Cacheable(value = "entityConfig", key = "'grouped:' + #entityId")
    public Map<String, List<EntityConfig>> getConfigsGroupedByType(String entityId) {
        List<EntityConfig> configs = getConfigsByEntityId(entityId);
        return configs.stream()
                .collect(Collectors.groupingBy(EntityConfig::getConfigType));
    }
    
    /**
     * 保存或更新配置
     * 清除缓存: entityConfig:entity:{entityId}
     */
    @Override
    @Transactional
    @CacheEvict(value = "entityConfig", key = "'entity:' + #entityId")
    public String saveOrUpdateConfig(String entityId, String configType, String configKey, String configValue, String description) {
        LambdaQueryWrapper<EntityConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EntityConfig::getEntityId, entityId)
               .eq(EntityConfig::getConfigType, configType)
               .eq(EntityConfig::getConfigKey, configKey);
        
        EntityConfig existing = getOne(wrapper);
        
        if (existing != null) {
            existing.setConfigValue(configValue);
            existing.setDescription(description);
            updateById(existing);
            return existing.getId();
        } else {
            EntityConfig config = new EntityConfig();
            config.setEntityId(entityId);
            config.setConfigType(configType);
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setDescription(description);
            save(config);
            return config.getId();
        }
    }
    
    /**
     * 删除指定法人的配置
     * 清除缓存: entityConfig:entity:{entityId}
     */
    @Override
    @Transactional
    @CacheEvict(value = "entityConfig", key = "'entity:' + #entityId")
    public void deleteConfigsByEntityId(String entityId, String configType) {
        LambdaQueryWrapper<EntityConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EntityConfig::getEntityId, entityId);
        if (configType != null && !configType.isEmpty()) {
            wrapper.eq(EntityConfig::getConfigType, configType);
        }
        remove(wrapper);
    }
}





