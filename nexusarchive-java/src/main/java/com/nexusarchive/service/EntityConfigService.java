// Input: MyBatis-Plus IService、EntityConfig Entity
// Output: EntityConfigService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexusarchive.entity.EntityConfig;

import java.util.List;
import java.util.Map;

/**
 * 法人配置服务接口
 * 
 * 功能: 管理每个法人的独立配置（ERP接口、业务规则、合规策略等）
 */
public interface EntityConfigService extends IService<EntityConfig> {
    
    /**
     * 获取指定法人的所有配置
     * @param entityId 法人ID
     * @return 配置列表
     */
    List<EntityConfig> getConfigsByEntityId(String entityId);
    
    /**
     * 获取指定法人和配置类型的配置
     * @param entityId 法人ID
     * @param configType 配置类型
     * @return 配置列表
     */
    List<EntityConfig> getConfigsByEntityIdAndType(String entityId, String configType);
    
    /**
     * 获取指定法人的配置（按类型分组）
     * @param entityId 法人ID
     * @return 配置Map，key为配置类型，value为配置列表
     */
    Map<String, List<EntityConfig>> getConfigsGroupedByType(String entityId);
    
    /**
     * 保存或更新配置
     * @param entityId 法人ID
     * @param configType 配置类型
     * @param configKey 配置键
     * @param configValue 配置值（JSON格式）
     * @param description 配置描述
     * @return 配置ID
     */
    String saveOrUpdateConfig(String entityId, String configType, String configKey, String configValue, String description);
    
    /**
     * 删除指定法人的配置
     * @param entityId 法人ID
     * @param configType 配置类型（可选，为空则删除所有配置）
     */
    void deleteConfigsByEntityId(String entityId, String configType);
}





