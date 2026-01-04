// Input: MyBatis-Plus BaseMapper
// Output: SysEntityMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.SysEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 法人实体 Mapper
 * 对应表: sys_entity
 */
@Mapper
public interface SysEntityMapper extends BaseMapper<SysEntity> {
    
    /**
     * 查询指定法人下的全宗列表
     * @param entityId 法人ID
     * @return 全宗ID列表
     */
    @Select("SELECT id FROM bas_fonds WHERE entity_id = #{entityId} AND deleted = 0")
    List<String> findFondsIdsByEntityId(String entityId);
}



