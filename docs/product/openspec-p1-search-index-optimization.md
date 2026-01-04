# 电子会计档案系统 - 检索索引优化 OpenSpec

> **版本**: v1.0  
> **日期**: 2025-01  
> **对齐基准**: [开发路线图 v1.0](../planning/development_roadmap_v1.0.md)  
> **优先级**: **P1（重要但非阻塞 - 短期优化）**  
> **当前状态**: 待开发

---

## 📊 功能概览

| 功能模块 | 路线图章节 | 优先级 | 预计工作量 | 依赖关系 |
|---------|-----------|--------|-----------|---------|
| 检索索引优化 | 阶段三：检索增强 | **P1** | 1 周 | 无 |

---

## 🎯 业务目标

**用户故事**: 作为系统用户，我需要在大量档案数据中进行快速检索，系统应优化索引配置，确保查询响应时间在可接受范围内。

**业务价值**:
- 提升检索查询性能
- 优化 JSONB 字段查询速度
- 降低数据库负载
- 改善用户体验

**问题背景**:
- PostgreSQL JSONB 字段（`custom_metadata`, `standard_metadata`）可能缺少 GIN 索引
- 复合查询（全宗号 + 年度 + 类型）可能缺少复合索引
- 需要检查现有索引配置，补充缺失的索引

---

## 📋 功能范围

### 1. 索引分析

#### 1.1 当前索引状态检查

**需要检查的索引**:
1. JSONB 字段索引
   - `acc_archive.custom_metadata` - GIN 索引
   - `acc_archive.standard_metadata` - GIN 索引
   
2. 常用查询字段索引
   - `acc_archive.fonds_no` - B-tree 索引
   - `acc_archive.fiscal_year` - B-tree 索引
   - `acc_archive.doc_type` - B-tree 索引
   - `acc_archive.status` - B-tree 索引
   
3. 复合索引
   - `(fonds_no, fiscal_year, doc_type)` - 复合 B-tree 索引
   - `(fonds_no, status)` - 复合索引
   - `(fiscal_year, status)` - 复合索引

#### 1.2 查询性能分析

**需要分析的查询场景**:
1. JSONB 字段查询（如：`custom_metadata->>'bookType' = ?`）
2. 多条件组合查询（全宗号 + 年度 + 类型）
3. 范围查询（年度范围、日期范围）
4. 模糊查询（标题模糊匹配）

**性能基准**:
- JSONB 查询: < 100ms（10万条数据）
- 复合查询: < 50ms（10万条数据）
- 全文检索: < 200ms（10万条数据）

### 2. 索引创建

#### 2.1 JSONB GIN 索引

**索引类型**: GIN (Generalized Inverted Index)

**适用场景**:
- JSONB 字段的包含查询（`@>` 操作符）
- JSONB 字段的键值查询（`->`, `->>` 操作符）
- JSONB 字段的路径查询

**创建语句**:
```sql
-- custom_metadata JSONB GIN 索引
CREATE INDEX IF NOT EXISTS idx_archive_custom_metadata_gin 
ON acc_archive USING GIN (custom_metadata);

-- standard_metadata JSONB GIN 索引
CREATE INDEX IF NOT EXISTS idx_archive_standard_metadata_gin 
ON acc_archive USING GIN (standard_metadata);

-- 如果经常查询特定路径，可以创建表达式索引
CREATE INDEX IF NOT EXISTS idx_archive_custom_booktype 
ON acc_archive ((custom_metadata->>'bookType')) 
WHERE custom_metadata->>'bookType' IS NOT NULL;
```

#### 2.2 复合索引

**索引类型**: B-tree 复合索引

**适用场景**:
- 多条件组合查询（WHERE fonds_no = ? AND fiscal_year = ? AND doc_type = ?）
- 排序查询（ORDER BY fiscal_year DESC, doc_type）

**创建语句**:
```sql
-- 全宗号 + 年度 + 类型的复合索引
CREATE INDEX IF NOT EXISTS idx_archive_fonds_year_type 
ON acc_archive (fonds_no, fiscal_year, doc_type);

-- 全宗号 + 状态的复合索引
CREATE INDEX IF NOT EXISTS idx_archive_fonds_status 
ON acc_archive (fonds_no, status) 
WHERE status IN ('archived', 'pending');

-- 年度 + 状态的复合索引
CREATE INDEX IF NOT EXISTS idx_archive_year_status 
ON acc_archive (fiscal_year, status);
```

#### 2.3 表达式索引

**适用场景**:
- 常用 JSONB 路径查询
- 大小写不敏感查询
- 函数表达式查询

**创建语句**:
```sql
-- 标题大小写不敏感索引（如果需要）
CREATE INDEX IF NOT EXISTS idx_archive_title_lower 
ON acc_archive (LOWER(title));

-- 归档时间年份索引（如果需要按年份查询）
CREATE INDEX IF NOT EXISTS idx_archive_created_year 
ON acc_archive (EXTRACT(YEAR FROM created_time));
```

### 3. 索引优化策略

#### 3.1 索引选择性

**原则**:
- 高选择性字段优先创建索引
- 低选择性字段考虑部分索引（WHERE 条件）
- 避免过度索引（影响写入性能）

**选择性分析**:
- `fonds_no`: 通常高选择性（不同全宗号数量多）
- `fiscal_year`: 中等选择性（年份范围有限）
- `doc_type`: 低选择性（类型种类有限，考虑部分索引）
- `status`: 低选择性（状态种类有限，考虑部分索引）

#### 3.2 部分索引

**使用场景**:
- 只查询特定状态的记录（如：`status = 'archived'`）
- 只查询特定类型的记录
- 减少索引大小，提升性能

**示例**:
```sql
-- 只索引已归档的档案（最常用查询）
CREATE INDEX IF NOT EXISTS idx_archive_archived_fonds_year 
ON acc_archive (fonds_no, fiscal_year) 
WHERE status = 'archived';

-- 只索引特定类型的档案
CREATE INDEX IF NOT EXISTS idx_archive_voucher_type 
ON acc_archive (fonds_no, fiscal_year) 
WHERE doc_type = '凭证';
```

#### 3.3 索引维护

**维护策略**:
- 定期执行 `ANALYZE` 更新统计信息
- 监控索引使用情况（`pg_stat_user_indexes`）
- 删除未使用的索引
- 重建膨胀的索引（`REINDEX`）

---

## 🔧 技术规格

### 4. 数据库迁移脚本

#### 4.1 索引创建脚本

**文件位置**: `nexusarchive-java/src/main/resources/db/migration/V83__create_search_indexes.sql`

```sql
-- Input: 数据库引擎
-- Output: 检索索引创建脚本
-- Pos: Flyway 数据库迁移脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- ============================================
-- 检索索引优化
-- ============================================
-- 说明: 为 acc_archive 表创建优化索引，提升检索性能
-- OpenSpec 来源: openspec-p1-search-index-optimization.md

-- 1. JSONB GIN 索引（用于 JSONB 字段查询）
CREATE INDEX IF NOT EXISTS idx_archive_custom_metadata_gin 
ON acc_archive USING GIN (custom_metadata);

CREATE INDEX IF NOT EXISTS idx_archive_standard_metadata_gin 
ON acc_archive USING GIN (standard_metadata);

-- 2. 常用字段索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_archive_fonds_no 
ON acc_archive (fonds_no);

CREATE INDEX IF NOT EXISTS idx_archive_fiscal_year 
ON acc_archive (fiscal_year);

CREATE INDEX IF NOT EXISTS idx_archive_doc_type 
ON acc_archive (doc_type);

CREATE INDEX IF NOT EXISTS idx_archive_status 
ON acc_archive (status);

-- 3. 复合索引（多条件查询优化）
CREATE INDEX IF NOT EXISTS idx_archive_fonds_year_type 
ON acc_archive (fonds_no, fiscal_year, doc_type);

CREATE INDEX IF NOT EXISTS idx_archive_fonds_status 
ON acc_archive (fonds_no, status) 
WHERE status IN ('archived', 'pending');

CREATE INDEX IF NOT EXISTS idx_archive_year_status 
ON acc_archive (fiscal_year, status);

-- 4. 部分索引（优化特定查询场景）
CREATE INDEX IF NOT EXISTS idx_archive_archived_fonds_year 
ON acc_archive (fonds_no, fiscal_year) 
WHERE status = 'archived';

-- 5. 表达式索引（常用 JSONB 路径查询）
CREATE INDEX IF NOT EXISTS idx_archive_custom_booktype 
ON acc_archive ((custom_metadata->>'bookType')) 
WHERE custom_metadata->>'bookType' IS NOT NULL;

-- 索引注释
COMMENT ON INDEX idx_archive_custom_metadata_gin IS 'custom_metadata JSONB GIN 索引，用于 JSONB 包含和键值查询';
COMMENT ON INDEX idx_archive_standard_metadata_gin IS 'standard_metadata JSONB GIN 索引，用于 JSONB 包含和键值查询';
COMMENT ON INDEX idx_archive_fonds_year_type IS '全宗号+年度+类型复合索引，用于多条件组合查询';
COMMENT ON INDEX idx_archive_archived_fonds_year IS '已归档档案的部分索引，优化常用查询';
```

### 5. 查询优化

#### 5.1 JSONB 查询优化

**优化前**:
```sql
-- 未使用索引，全表扫描
SELECT * FROM acc_archive 
WHERE custom_metadata->>'bookType' = '总账';
```

**优化后**:
```sql
-- 使用表达式索引
SELECT * FROM acc_archive 
WHERE custom_metadata->>'bookType' = '总账';
-- 或使用 GIN 索引
SELECT * FROM acc_archive 
WHERE custom_metadata @> '{"bookType": "总账"}'::jsonb;
```

#### 5.2 复合查询优化

**优化前**:
```sql
-- 可能无法充分利用索引
SELECT * FROM acc_archive 
WHERE fonds_no = 'JD-001' 
  AND fiscal_year = '2024' 
  AND doc_type = '凭证'
ORDER BY created_time DESC;
```

**优化后**:
```sql
-- 使用复合索引 idx_archive_fonds_year_type
SELECT * FROM acc_archive 
WHERE fonds_no = 'JD-001' 
  AND fiscal_year = '2024' 
  AND doc_type = '凭证'
ORDER BY created_time DESC;
-- 如果需要排序，考虑添加 (fonds_no, fiscal_year, doc_type, created_time) 索引
```

### 6. 性能测试

#### 6.1 索引使用情况检查

**SQL 查询**:
```sql
-- 查看索引使用情况
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE tablename = 'acc_archive'
ORDER BY idx_scan DESC;

-- 查看表统计信息
SELECT 
    schemaname,
    tablename,
    n_live_tup as row_count,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE tablename = 'acc_archive';

-- 查看索引大小
SELECT 
    indexname,
    pg_size_pretty(pg_relation_size(indexname::regclass)) as index_size
FROM pg_indexes
WHERE tablename = 'acc_archive'
ORDER BY pg_relation_size(indexname::regclass) DESC;
```

#### 6.2 查询执行计划分析

**EXPLAIN ANALYZE**:
```sql
-- 分析查询执行计划
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT * FROM acc_archive 
WHERE custom_metadata @> '{"bookType": "总账"}'::jsonb
  AND fonds_no = 'JD-001'
  AND fiscal_year = '2024';
```

**关键指标**:
- Index Scan vs Seq Scan（应优先使用 Index Scan）
- 执行时间（应 < 100ms）
- 缓冲区命中率（应 > 95%）

---

## 🧪 测试要求

### 7.1 性能测试

**测试场景**:
- 10万条数据的基础查询性能
- JSONB 字段查询性能（使用索引前后对比）
- 复合查询性能（使用索引前后对比）
- 并发查询性能

**测试脚本**:
- 准备测试数据（生成 10万条测试档案）
- 执行查询性能测试
- 对比索引创建前后的性能差异
- 生成性能测试报告

### 7.2 索引有效性验证

**验证方法**:
- 使用 `EXPLAIN ANALYZE` 验证索引使用
- 检查 `pg_stat_user_indexes` 确认索引被使用
- 对比查询执行时间（索引前后）

---

## 📝 开发检查清单

- [ ] 检查当前数据库索引配置
- [ ] 分析常用查询场景和执行计划
- [ ] 确定需要创建的索引
- [ ] 创建数据库迁移脚本
- [ ] 执行索引创建（在测试环境）
- [ ] 执行性能测试和对比
- [ ] 优化查询语句（如需要）
- [ ] 更新数据库设计文档
- [ ] 生成性能基准报告

---

## 🔗 相关文档

- 开发路线图：`docs/planning/development_roadmap_v1.0.md`
- 缺口分析报告：`docs/reports/roadmap-gap-analysis-2025-01.md`
- 高级检索服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/AdvancedArchiveSearchService.java`
- PostgreSQL JSONB 文档：https://www.postgresql.org/docs/current/datatype-json.html

---

**文档状态**: ✅ 已完成  
**下一步**: 检查当前索引配置，创建数据库迁移脚本



