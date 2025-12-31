// Input: EntityService、SysEntityMapper、BasFondsMapper
// Output: EntityServiceImpl 类
// Pos: 业务服务层实现
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexusarchive.entity.SysEntity;
import com.nexusarchive.mapper.SysEntityMapper;
import com.nexusarchive.service.EntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 法人实体服务实现类
 * 
 * PRD 来源: Section 1.1 - 法人仅管理维度
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityServiceImpl extends ServiceImpl<SysEntityMapper, SysEntity> implements EntityService {
    
    private final SysEntityMapper entityMapper;
    
    @Override
    public List<SysEntity> listActive() {
        LambdaQueryWrapper<SysEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEntity::getStatus, "ACTIVE");
        return list(wrapper);
    }
    
    @Override
    public List<String> getFondsIdsByEntityId(String entityId) {
        return entityMapper.findFondsIdsByEntityId(entityId);
    }
    
    @Override
    public boolean canDelete(String entityId) {
        List<String> fondsIds = getFondsIdsByEntityId(entityId);
        if (fondsIds != null && !fondsIds.isEmpty()) {
            log.warn("法人 {} 下存在 {} 个全宗，无法删除", entityId, fondsIds.size());
            return false;
        }
        return true;
    }
}

