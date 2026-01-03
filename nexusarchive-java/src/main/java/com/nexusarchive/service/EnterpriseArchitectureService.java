// Input: EntityService、BasFondsService、ArchiveService
// Output: EnterpriseArchitectureService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.EnterpriseArchitectureTree;

import java.util.List;

/**
 * 集团架构服务接口
 * 
 * 功能: 提供集团架构树视图数据（法人 -> 全宗 -> 档案）
 */
public interface EnterpriseArchitectureService {
    
    /**
     * 获取完整的集团架构树
     * @return 集团架构树
     */
    EnterpriseArchitectureTree getArchitectureTree();
    
    /**
     * 获取指定法人下的架构树
     * @param entityId 法人ID
     * @return 法人架构树
     */
    EnterpriseArchitectureTree getArchitectureTreeByEntity(String entityId);
}


