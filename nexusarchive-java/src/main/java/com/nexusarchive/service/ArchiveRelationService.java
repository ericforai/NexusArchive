// Input: MyBatis-Plus、Spring Framework、Java 标准库、本地模块
// Output: ArchiveRelationService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexusarchive.entity.ArchiveRelation;
import com.nexusarchive.mapper.ArchiveRelationMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * 档案关联关系服务
 * 
 * 缓存失效策略：
 * - 当关系数据变更时（新增、更新、删除），清除 archiveVoucherMapping 缓存
 * - 因为关系变更会影响档案→凭证的映射结果
 */
@Service
public class ArchiveRelationService extends ServiceImpl<ArchiveRelationMapper, ArchiveRelation> implements IArchiveRelationService {
    
    /**
     * 保存关联关系
     * 清除相关缓存（因为关系变更会影响凭证查找结果）
     */
    @Override
    @CacheEvict(value = "archiveVoucherMapping", allEntries = true)
    public boolean save(ArchiveRelation entity) {
        return super.save(entity);
    }
    
    /**
     * 更新关联关系
     * 清除相关缓存
     */
    @Override
    @CacheEvict(value = "archiveVoucherMapping", allEntries = true)
    public boolean updateById(ArchiveRelation entity) {
        return super.updateById(entity);
    }
    
    /**
     * 删除关联关系
     * 清除相关缓存
     */
    @Override
    @CacheEvict(value = "archiveVoucherMapping", allEntries = true)
    public boolean removeById(java.io.Serializable id) {
        return super.removeById(id);
    }
}
