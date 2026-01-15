// Input: EnterpriseArchitectureService、EntityService、BasFondsService、ArchiveMapper
// Output: EnterpriseArchitectureServiceImpl 类
// Pos: 业务服务层实现
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.EnterpriseArchitectureTree;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.entity.SysEntity;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.BasFondsService;
import com.nexusarchive.service.EntityService;
import com.nexusarchive.service.EnterpriseArchitectureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 集团架构服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseArchitectureServiceImpl implements EnterpriseArchitectureService {
    
    private final EntityService entityService;
    private final BasFondsService fondsService;
    private final ArchiveMapper archiveMapper;
    private final ArcFileContentMapper fileContentMapper;
    
    @Override
    public EnterpriseArchitectureTree getArchitectureTree() {
        EnterpriseArchitectureTree tree = new EnterpriseArchitectureTree();
        
        // 获取所有活跃法人（已自动应用部门过滤）
        List<SysEntity> entities = entityService.listActive();
        log.info("ArchitectureTree: Found {} active entities (after department filter)", entities.size());
        if (!entities.isEmpty()) {
            entities.forEach(e -> log.debug("  - Entity: id={}, name={}, status={}, taxId={}", 
                e.getId(), e.getName(), e.getStatus(), e.getTaxId()));
        } else {
             // Fallback: Try listing ALL entities to warn if status mismatch
             log.warn("ArchitectureTree: No ACTIVE entities found. checking all entities...");
             List<SysEntity> all = entityService.list();
             all.forEach(e -> log.warn("  - Existing Entity: id={}, name={}, status='{}', taxId='{}'", 
                 e.getId(), e.getName(), e.getStatus(), e.getTaxId()));
        }
        
        List<EnterpriseArchitectureTree.EntityNode> entityNodes = entities.stream()
            .map(entity -> {
                EnterpriseArchitectureTree.EntityNode node = new EnterpriseArchitectureTree.EntityNode();
                node.setId(entity.getId());
                node.setName(entity.getName());
                node.setTaxId(entity.getTaxId());
                node.setStatus(entity.getStatus());
                
                // 获取法人下的全宗
                List<String> fondsIds = entityService.getFondsIdsByEntityId(entity.getId());
                log.debug("Entity {} (id={}): Found {} fonds IDs", entity.getName(), entity.getId(), 
                    fondsIds != null ? fondsIds.size() : 0);
                
                List<BasFonds> fondsList;
                if (fondsIds == null || fondsIds.isEmpty()) {
                    fondsList = java.util.Collections.emptyList();
                    log.debug("Entity {}: No fonds found (entity_id may not be set in bas_fonds table)", entity.getName());
                } else {
                    fondsList = fondsService.listByIds(fondsIds);
                    log.debug("Entity {}: Loaded {} fonds from {} IDs", entity.getName(), fondsList.size(), fondsIds.size());
                    if (fondsList.size() < fondsIds.size()) {
                        log.warn("Entity {}: Some fonds IDs not found. Expected {}, got {}", 
                            entity.getName(), fondsIds.size(), fondsList.size());
                    }
                }
                
                node.setFondsCount(fondsList.size());
                
                // 计算全宗节点
                List<EnterpriseArchitectureTree.FondsNode> fondsNodes = fondsList.stream()
                    .map(fonds -> {
                        EnterpriseArchitectureTree.FondsNode fondsNode = new EnterpriseArchitectureTree.FondsNode();
                        fondsNode.setId(fonds.getId());
                        fondsNode.setFondsCode(fonds.getFondsCode());
                        fondsNode.setFondsName(fonds.getFondsName());
                        
                        // 统计档案数量
                        Long archiveCount = archiveMapper.selectCount(
                            new LambdaQueryWrapper<com.nexusarchive.entity.Archive>()
                                .eq(com.nexusarchive.entity.Archive::getFondsNo, fonds.getFondsCode())
                        );
                        fondsNode.setArchiveCount(archiveCount != null ? archiveCount : 0L);
                        
                        // 统计容量（GB）
                        Long totalSize = fileContentMapper.selectObjs(
                            new LambdaQueryWrapper<ArcFileContent>()
                                .select(ArcFileContent::getFileSize)
                                .eq(ArcFileContent::getFondsCode, fonds.getFondsCode())
                        ).stream()
                        .filter(obj -> obj instanceof Long)
                        .mapToLong(obj -> (Long) obj)
                        .sum();
                        fondsNode.setSizeGB(totalSize / (1024.0 * 1024 * 1024));
                        
                        // 统计归档年度数量（简化实现）
                        fondsNode.setArchiveYearCount(1); // TODO: 实际应该从 Archive 表统计不同年度
                        
                        return fondsNode;
                    })
                    .collect(Collectors.toList());
                
                node.setFonds(fondsNodes);
                
                // 计算法人级别的统计
                Long totalArchiveCount = fondsNodes.stream()
                    .mapToLong(EnterpriseArchitectureTree.FondsNode::getArchiveCount)
                    .sum();
                node.setArchiveCount(totalArchiveCount);
                
                Double totalSizeGB = fondsNodes.stream()
                    .mapToDouble(EnterpriseArchitectureTree.FondsNode::getSizeGB)
                    .sum();
                node.setTotalSizeGB(totalSizeGB);
                
                return node;
            })
            .collect(Collectors.toList());
        
        tree.setEntities(entityNodes);
        return tree;
    }
    
    @Override
    public EnterpriseArchitectureTree getArchitectureTreeByEntity(String entityId) {
        EnterpriseArchitectureTree tree = new EnterpriseArchitectureTree();
        
        SysEntity entity = entityService.getById(entityId);
        if (entity == null) {
            return tree;
        }
        
        // 构建单个法人的架构树
        EnterpriseArchitectureTree.EntityNode node = new EnterpriseArchitectureTree.EntityNode();
        node.setId(entity.getId());
        node.setName(entity.getName());
        node.setTaxId(entity.getTaxId());
        node.setStatus(entity.getStatus());
        
        // 获取法人下的全宗
        List<String> fondsIds = entityService.getFondsIdsByEntityId(entityId);
        List<BasFonds> fondsList = fondsService.listByIds(fondsIds);
        
        node.setFondsCount(fondsList.size());
        
        // 计算全宗节点（与 getArchitectureTree 相同的逻辑）
        List<EnterpriseArchitectureTree.FondsNode> fondsNodes = fondsList.stream()
            .map(fonds -> {
                EnterpriseArchitectureTree.FondsNode fondsNode = new EnterpriseArchitectureTree.FondsNode();
                fondsNode.setId(fonds.getId());
                fondsNode.setFondsCode(fonds.getFondsCode());
                fondsNode.setFondsName(fonds.getFondsName());
                
                Long archiveCount = archiveMapper.selectCount(
                    new LambdaQueryWrapper<com.nexusarchive.entity.Archive>()
                        .eq(com.nexusarchive.entity.Archive::getFondsNo, fonds.getFondsCode())
                );
                fondsNode.setArchiveCount(archiveCount != null ? archiveCount : 0L);
                
                Long totalSize = fileContentMapper.selectObjs(
                    new LambdaQueryWrapper<ArcFileContent>()
                        .select(ArcFileContent::getFileSize)
                        .eq(ArcFileContent::getFondsCode, fonds.getFondsCode())
                ).stream()
                .filter(obj -> obj instanceof Long)
                .mapToLong(obj -> (Long) obj)
                .sum();
                fondsNode.setSizeGB(totalSize / (1024.0 * 1024 * 1024));
                fondsNode.setArchiveYearCount(1);
                
                return fondsNode;
            })
            .collect(Collectors.toList());
        
        node.setFonds(fondsNodes);
        
        Long totalArchiveCount = fondsNodes.stream()
            .mapToLong(EnterpriseArchitectureTree.FondsNode::getArchiveCount)
            .sum();
        node.setArchiveCount(totalArchiveCount);
        
        Double totalSizeGB = fondsNodes.stream()
            .mapToDouble(EnterpriseArchitectureTree.FondsNode::getSizeGB)
            .sum();
        node.setTotalSizeGB(totalSizeGB);
        
        tree.setEntities(List.of(node));
        return tree;
    }
}

