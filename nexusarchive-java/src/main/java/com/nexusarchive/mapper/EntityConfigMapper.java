// Input: MyBatis-Plus BaseMapper
// Output: EntityConfigMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.EntityConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 法人配置 Mapper
 * 对应表: sys_entity_config
 */
@Mapper
public interface EntityConfigMapper extends BaseMapper<EntityConfig> {
    
    /**
     * 查询指定法人的所有配置
     * @param entityId 法人ID
     * @return 配置列表
     */
    @Select("SELECT * FROM sys_entity_config WHERE entity_id = #{entityId} AND deleted = 0")
    List<EntityConfig> findByEntityId(@Param("entityId") String entityId);
    
    /**
     * 查询指定法人和配置类型的配置
     * @param entityId 法人ID
     * @param configType 配置类型
     * @return 配置列表
     */
    @Select("SELECT * FROM sys_entity_config WHERE entity_id = #{entityId} AND config_type = #{configType} AND deleted = 0")
    List<EntityConfig> findByEntityIdAndType(@Param("entityId") String entityId, @Param("configType") String configType);
}

