# 修复集团架构树视图全宗信息缺失问题 - Spec 提案

**日期**: 2026-01-14  
**问题**: 集团架构树视图页面 (`/system/settings/org/architecture`) 没有显示全宗信息  
**状态**: 待执行

---

## 问题分析

### 当前问题
1. **页面显示**: 集团架构树视图只显示法人，没有显示全宗信息
2. **预期行为**: 应该显示"法人 → 全宗 → 档案"三层结构
3. **实际行为**: 只显示法人层级，全宗信息缺失

### 根本原因分析

#### 1. 部门过滤逻辑影响
- `EnterpriseArchitectureServiceImpl.getArchitectureTree()` 使用 `entityService.listActive()` 获取法人
- `listActive()` 方法**没有应用部门过滤逻辑**
- 但之前添加的过滤逻辑在 `getTree()` 方法中，导致：
  - 法人管理页面正确过滤了部门
  - 但集团架构页面可能显示了部门，或者因为过滤不一致导致数据不匹配

#### 2. 全宗关联查询逻辑
- `findFondsIdsByEntityId()` 通过 `SELECT id FROM bas_fonds WHERE entity_id = #{entityId}` 查询
- 如果全宗的 `entity_id` 字段为空或未正确设置，会导致查询不到全宗
- 如果法人的 ID 与全宗的 `entity_id` 不匹配，也会导致查询不到

#### 3. 数据一致性
- 部门数据可能被错误地当作法人处理
- 全宗可能关联到了部门而不是法人

---

## 解决方案 Spec

### Spec 1: 统一法人过滤逻辑

**目标**: 确保所有获取法人的方法都应用部门过滤逻辑

**修改点**:
1. **EntityService.listActive()** - 添加部门过滤
2. **EntityService.list()** - 添加部门过滤（可选，用于一致性）

**实现**:
```java
@Override
public List<SysEntity> listActive() {
    LambdaQueryWrapper<SysEntity> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(SysEntity::getStatus, "ACTIVE");
    List<SysEntity> all = list(wrapper);
    // 应用部门过滤逻辑
    return all.stream()
            .filter(this::isEntityNotDepartment)
            .collect(Collectors.toList());
}
```

**影响范围**:
- `EnterpriseArchitectureServiceImpl.getArchitectureTree()` - 自动获得过滤后的法人列表
- 其他使用 `listActive()` 的地方也会自动过滤部门

---

### Spec 2: 修复集团架构服务实现

**目标**: 确保集团架构服务正确获取和显示全宗信息

**修改点**:
1. **EnterpriseArchitectureServiceImpl.getArchitectureTree()** - 使用过滤后的法人列表
2. **添加日志** - 便于排查全宗关联问题
3. **处理空全宗列表** - 优雅处理没有全宗的情况

**实现**:
```java
@Override
public EnterpriseArchitectureTree getArchitectureTree() {
    EnterpriseArchitectureTree tree = new EnterpriseArchitectureTree();
    
    // 使用 listActive() 会自动应用部门过滤（如果 Spec 1 已实现）
    List<SysEntity> entities = entityService.listActive();
    log.info("ArchitectureTree: Found {} active entities (after department filter)", entities.size());
    
    List<EnterpriseArchitectureTree.EntityNode> entityNodes = entities.stream()
        .map(entity -> {
            EnterpriseArchitectureTree.EntityNode node = new EnterpriseArchitectureTree.EntityNode();
            node.setId(entity.getId());
            node.setName(entity.getName());
            node.setTaxId(entity.getTaxId());
            node.setStatus(entity.getStatus());
            
            // 获取法人下的全宗
            List<String> fondsIds = entityService.getFondsIdsByEntityId(entity.getId());
            log.debug("Entity {} (id={}): Found {} fonds IDs", entity.getName(), entity.getId(), fondsIds.size());
            
            List<BasFonds> fondsList;
            if (fondsIds == null || fondsIds.isEmpty()) {
                fondsList = Collections.emptyList();
                log.debug("Entity {}: No fonds found (entity_id may not be set in bas_fonds)", entity.getName());
            } else {
                fondsList = fondsService.listByIds(fondsIds);
                log.debug("Entity {}: Loaded {} fonds from {} IDs", entity.getName(), fondsList.size(), fondsIds.size());
            }
            
            node.setFondsCount(fondsList.size());
            
            // 计算全宗节点（现有逻辑保持不变）
            // ... 全宗节点构建逻辑 ...
            
            return node;
        })
        .collect(Collectors.toList());
    
    tree.setEntities(entityNodes);
    return tree;
}
```

---

### Spec 3: 数据验证和修复脚本

**目标**: 验证和修复全宗与法人的关联关系

**检查点**:
1. 检查全宗的 `entity_id` 是否为空
2. 检查全宗的 `entity_id` 是否指向有效的法人（而非部门）
3. 提供修复脚本，将部门关联的全宗迁移到正确的法人

**SQL 检查脚本**:
```sql
-- 1. 检查全宗 entity_id 为空的情况
SELECT id, fonds_code, fonds_name, entity_id 
FROM bas_fonds 
WHERE entity_id IS NULL OR entity_id = '';

-- 2. 检查全宗 entity_id 指向部门的情况
SELECT f.id, f.fonds_code, f.fonds_name, f.entity_id, e.name as entity_name
FROM bas_fonds f
LEFT JOIN sys_entity e ON f.entity_id = e.id
WHERE f.entity_id IS NOT NULL
  AND (
    e.name LIKE '%部' AND (e.tax_id IS NULL OR e.tax_id = '')
    OR e.name LIKE '%部门%'
  );

-- 3. 检查全宗 entity_id 指向不存在的法人
SELECT f.id, f.fonds_code, f.fonds_name, f.entity_id
FROM bas_fonds f
LEFT JOIN sys_entity e ON f.entity_id = e.id
WHERE f.entity_id IS NOT NULL AND e.id IS NULL;
```

**修复建议**:
- 如果全宗 `entity_id` 为空，需要手动关联到正确的法人
- 如果全宗关联到部门，需要迁移到该部门的父法人
- 如果全宗关联到不存在的法人，需要更新为正确的法人ID

---

### Spec 4: 前端显示优化

**目标**: 优化前端显示，明确展示全宗信息

**修改点**:
1. **EnterpriseArchitecturePage.tsx** - 添加空状态提示
2. **显示全宗数量** - 即使全宗列表为空，也要显示统计信息

**实现**:
```tsx
{/* Fonds Nodes */}
{expandedEntities.has(entity.id) && (
    <div className="bg-white border-t border-slate-200">
        {entity.fonds && entity.fonds.length > 0 ? (
            entity.fonds.map((fonds: FondsNode) => (
                // ... 现有全宗节点渲染 ...
            ))
        ) : (
            <div className="px-4 py-3 pl-12 text-sm text-slate-400">
                <FolderOpen className="w-4 h-4 inline mr-2" />
                该法人下暂无全宗
            </div>
        )}
    </div>
)}
```

---

## 实施计划

### 阶段 1: 统一过滤逻辑（优先级：P0）
1. 修改 `EntityService.listActive()` 添加部门过滤
2. 测试验证过滤逻辑正确性
3. 检查是否有其他地方需要应用过滤

### 阶段 2: 修复集团架构服务（优先级：P0）
1. 添加详细日志记录
2. 优化全宗查询逻辑
3. 处理边界情况（空全宗列表）

### 阶段 3: 数据验证（优先级：P1）
1. 执行 SQL 检查脚本
2. 分析数据问题
3. 提供修复建议或自动修复脚本

### 阶段 4: 前端优化（优先级：P2）
1. 优化空状态显示
2. 添加加载状态提示
3. 改进用户体验

---

## 验证标准

### 功能验证
- [ ] 集团架构树视图正确显示法人
- [ ] 展开法人后，正确显示该法人下的全宗列表
- [ ] 全宗信息完整（名称、编码、统计信息）
- [ ] 部门数据不显示在集团架构树中

### 数据验证
- [ ] 所有全宗的 `entity_id` 都指向有效的法人（而非部门）
- [ ] 没有全宗的 `entity_id` 为空的情况（或已处理）
- [ ] 全宗统计信息准确（档案数量、容量等）

### 性能验证
- [ ] 页面加载时间合理（< 2秒）
- [ ] 大数据量下性能稳定
- [ ] 缓存机制正常工作

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 过滤逻辑过于严格，误过滤法人 | 高 | 仔细测试过滤规则，确保只过滤明确的部门 |
| 全宗数据关联错误 | 中 | 提供数据验证和修复脚本 |
| 性能影响 | 低 | 使用缓存，优化查询逻辑 |

---

## 参考资料

1. [组织管理重构设计文档](./2026-01-11-org-management-refactor-design.md)
2. [法人管理功能修正 OPEC 方案](./2026-01-14-entity-management-correction-opec.md)
3. [全宗与法人实体关系的法律依据分析](../reports/legal-basis-fonds-entity-relationship.md)

---

**方案状态**: 📋 待执行  
**下一步**: 开始执行阶段 1（统一过滤逻辑）
