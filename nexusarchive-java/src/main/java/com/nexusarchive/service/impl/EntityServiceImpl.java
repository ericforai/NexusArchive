// Input: EntityService、SysEntityMapper、BasFondsMapper
// Output: EntityServiceImpl 类
// Pos: 业务服务层实现
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.SysEntity;
import com.nexusarchive.mapper.SysEntityMapper;
import com.nexusarchive.service.EntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 法人实体服务实现类
 *
 * PRD 来源: Section 1.1 - 法人仅管理维度
 * 缓存策略: 使用 entityTree 缓存空间，TTL 30 分钟
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
        List<SysEntity> all = list(wrapper);
        // 应用部门过滤逻辑：电子会计档案系统只管理法人实体，不管理部门
        return all.stream()
                .filter(this::isEntityNotDepartment)
                .collect(Collectors.toList());
    }

    /**
     * 获取法人树形结构
     * 缓存键: entityTree:tree
     * 
     * 过滤逻辑：电子会计档案系统只管理法人实体，不管理部门
     * - 过滤掉名称以"部"结尾且没有税号的记录（通常是部门）
     * - 过滤掉名称包含"部门"的记录
     */
    @Override
    @Cacheable(value = "entityTree", key = "'tree'")
    public List<EntityTreeNode> getTree() {
        List<SysEntity> all = list(new LambdaQueryWrapper<>());
        // 过滤部门数据：电子会计档案系统没有部门概念
        List<SysEntity> entities = all.stream()
                .filter(this::isEntityNotDepartment)
                .collect(Collectors.toList());
        
        List<EntityTreeNode> nodes = entities.stream().map(this::toNode).collect(Collectors.toList());
        // build tree
        return buildTree(nodes);
    }

    /**
     * 判断是否为法人实体（而非部门）
     * 电子会计档案系统只管理法人实体，不管理部门
     * 
     * 判断规则：
     * 1. 有税号（统一社会信用代码）的，视为法人实体
     * 2. 名称以"部"结尾且没有税号的，视为部门（过滤）
     * 3. 名称包含"部门"的，视为部门（过滤）
     * 4. 其他情况视为法人实体
     */
    private boolean isEntityNotDepartment(SysEntity entity) {
        String name = entity.getName();
        String taxId = entity.getTaxId();
        
        // 有税号的，肯定是法人实体
        if (StringUtils.hasText(taxId)) {
            return true;
        }
        
        // 没有税号，但名称以"部"结尾的，很可能是部门
        if (StringUtils.hasText(name) && name.endsWith("部")) {
            log.debug("过滤部门数据: id={}, name={} (名称以'部'结尾且无税号)", entity.getId(), name);
            return false;
        }
        
        // 名称包含"部门"的，视为部门
        if (StringUtils.hasText(name) && name.contains("部门")) {
            log.debug("过滤部门数据: id={}, name={} (名称包含'部门')", entity.getId(), name);
            return false;
        }
        
        // 其他情况视为法人实体
        return true;
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
        // 检查是否有下级法人
        long childCount = count(new LambdaQueryWrapper<SysEntity>()
                .eq(SysEntity::getParentId, entityId));
        if (childCount > 0) {
            log.warn("法人 {} 下存在 {} 个下级法人，无法删除", entityId, childCount);
            return false;
        }
        return true;
    }

    /**
     * 更新法人父节点
     * 清除缓存: entityTree:tree
     */
    @Override
    @Transactional
    @CacheEvict(value = "entityTree", allEntries = true)
    public void updateParent(String entityId, String parentId) {
        SysEntity entity = getById(entityId);
        if (entity == null) {
            throw new BusinessException("法人不存在");
        }
        // 防止循环引用
        if (StringUtils.hasText(parentId)) {
            if (parentId.equals(entityId)) {
                throw new BusinessException("不能将自己设置为父节点");
            }
            // TODO: 检查更深层级的循环引用
        }
        entity.setParentId(parentId);
        updateById(entity);
        log.info("法人 {} 的父节点更新为 {}", entityId, parentId);
    }

    /**
     * 更新法人排序
     * 清除缓存: entityTree:tree
     */
    @Override
    @Transactional
    @CacheEvict(value = "entityTree", allEntries = true)
    public void updateOrder(String entityId, Integer orderNum) {
        SysEntity entity = getById(entityId);
        if (entity == null) {
            throw new BusinessException("法人不存在");
        }
        entity.setOrderNum(orderNum);
        updateById(entity);
    }

    /**
     * 构建树形结构
     */
    private List<EntityTreeNode> buildTree(List<EntityTreeNode> nodes) {
        List<EntityTreeNode> roots = nodes.stream()
                .filter(n -> !StringUtils.hasText(n.getParentId()))
                .sorted(Comparator.comparing(EntityTreeNode::getOrderNum, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        for (EntityTreeNode root : roots) {
            root.setChildren(findChildren(root.getId(), nodes));
        }
        return roots;
    }

    /**
     * 递归查找子节点
     */
    private List<EntityTreeNode> findChildren(String parentId, List<EntityTreeNode> nodes) {
        List<EntityTreeNode> children = nodes.stream()
                .filter(n -> parentId.equals(n.getParentId()))
                .sorted(Comparator.comparing(EntityTreeNode::getOrderNum, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        for (EntityTreeNode child : children) {
            child.setChildren(findChildren(child.getId(), nodes));
        }
        return children;
    }

    /**
     * 转换为树节点
     */
    private EntityTreeNode toNode(SysEntity entity) {
        EntityTreeNode node = new EntityTreeNode();
        node.setId(entity.getId());
        node.setName(entity.getName());
        node.setTaxId(entity.getTaxId());
        node.setParentId(entity.getParentId());
        node.setOrderNum(entity.getOrderNum());
        node.setStatus(entity.getStatus());
        // 获取关联的全宗数量
        List<String> fondsIds = getFondsIdsByEntityId(entity.getId());
        node.setFondsCount(fondsIds != null ? fondsIds.size() : 0);
        return node;
    }
}

