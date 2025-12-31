// Input: MyBatis-Plus IService、SysEntity Entity
// Output: EntityService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexusarchive.entity.SysEntity;

import java.util.List;

/**
 * 法人实体服务接口
 * 
 * PRD 来源: Section 1.1 - 法人仅管理维度
 * 功能: 法人实体的 CRUD 操作，法人与全宗的关联管理
 */
public interface EntityService extends IService<SysEntity> {
    
    /**
     * 获取法人列表（仅活跃状态）
     * @return 法人列表
     */
    List<SysEntity> listActive();
    
    /**
     * 获取指定法人下的全宗ID列表
     * @param entityId 法人ID
     * @return 全宗ID列表
     */
    List<String> getFondsIdsByEntityId(String entityId);
    
    /**
     * 检查法人是否可以删除
     * @param entityId 法人ID
     * @return true = 可删除，false = 存在关联全宗不可删除
     */
    boolean canDelete(String entityId);
}

