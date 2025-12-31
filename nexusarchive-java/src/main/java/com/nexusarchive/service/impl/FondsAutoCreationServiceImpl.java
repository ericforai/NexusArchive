// Input: BasFondsService、EntityService、BasFondsMapper、SysEntityMapper、Lombok、Spring Framework
// Output: FondsAutoCreationServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.entity.SysEntity;
import com.nexusarchive.mapper.BasFondsMapper;
import com.nexusarchive.mapper.SysEntityMapper;
import com.nexusarchive.service.BasFondsService;
import com.nexusarchive.service.EntityService;
import com.nexusarchive.service.FondsAutoCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 全宗自动创建服务实现
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FondsAutoCreationServiceImpl implements FondsAutoCreationService {
    
    private final BasFondsService fondsService;
    private final BasFondsMapper fondsMapper;
    private final EntityService entityService;
    private final SysEntityMapper entityMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String ensureFondsExists(String fondsNo, String fondsName, 
                                   String entityName, String entityTaxCode,
                                   String operatorId) {
        if (!StringUtils.hasText(fondsNo)) {
            throw new BusinessException("全宗号不能为空");
        }
        if (!StringUtils.hasText(fondsName)) {
            throw new BusinessException("全宗名称不能为空");
        }
        
        // 检查全宗是否已存在（通过 fondsCode）
        LambdaQueryWrapper<BasFonds> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasFonds::getFondsCode, fondsNo);
        BasFonds existingFonds = fondsService.getOne(wrapper);
        
        if (existingFonds != null) {
            log.debug("全宗已存在: fondsNo={}, fondsId={}", fondsNo, existingFonds.getId());
            return existingFonds.getId();
        }
        
        // 创建新全宗
        BasFonds newFonds = new BasFonds();
        newFonds.setFondsCode(fondsNo);
        newFonds.setFondsName(fondsName);
        newFonds.setCompanyName(fondsName); // 立档单位名称默认使用全宗名称
        newFonds.setCreatedBy(operatorId);
        newFonds.setCreatedTime(LocalDateTime.now());
        
        // 如果提供了实体信息，关联实体
        if (StringUtils.hasText(entityName) || StringUtils.hasText(entityTaxCode)) {
            String entityId = ensureEntityExists(entityName, entityTaxCode);
            // 注意: BasFonds 实体中如果有 entityId 字段，需要设置
            // 当前 BasFonds 实体中没有 entityId 字段，可能需要扩展
        }
        
        fondsService.save(newFonds);
        log.info("自动创建全宗: fondsNo={}, fondsId={}", fondsNo, newFonds.getId());
        
        return newFonds.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String ensureEntityExists(String entityName, String entityTaxCode) {
        // 优先使用 taxCode 进行精确匹配
        if (StringUtils.hasText(entityTaxCode)) {
            LambdaQueryWrapper<SysEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysEntity::getTaxId, entityTaxCode);
            SysEntity existingEntity = entityService.getOne(wrapper);
            
            if (existingEntity != null) {
                log.debug("实体已存在（通过税号）: taxCode={}, entityId={}", entityTaxCode, existingEntity.getId());
                return existingEntity.getId();
            }
        }
        
        // 如果 taxCode 为空或未找到，使用名称进行模糊匹配
        if (StringUtils.hasText(entityName)) {
            LambdaQueryWrapper<SysEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysEntity::getName, entityName);
            SysEntity existingEntity = entityService.getOne(wrapper);
            
            if (existingEntity != null) {
                log.debug("实体已存在（通过名称）: name={}, entityId={}", entityName, existingEntity.getId());
                return existingEntity.getId();
            }
        }
        
        // 创建新实体
        SysEntity newEntity = new SysEntity();
        newEntity.setName(entityName);
        newEntity.setTaxId(entityTaxCode);
        newEntity.setStatus("ACTIVE");
        newEntity.setCreatedBy("SYSTEM"); // 系统自动创建
        newEntity.setCreatedTime(LocalDateTime.now());
        
        entityService.save(newEntity);
        log.info("自动创建实体: name={}, taxCode={}, entityId={}", entityName, entityTaxCode, newEntity.getId());
        
        return newEntity.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void associateFondsWithEntity(String fondsId, String entityId) {
        // 注意: 当前 BasFonds 实体中没有 entityId 字段
        // 如果需要关联，可能需要：
        // 1. 扩展 BasFonds 实体添加 entityId 字段
        // 2. 或者创建关联表 bas_fonds_entity
        // 暂时记录日志，待实体扩展后实现
        log.warn("全宗与实体关联功能待实现: fondsId={}, entityId={}", fondsId, entityId);
    }
}

