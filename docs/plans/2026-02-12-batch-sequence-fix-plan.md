# 使用数据库序列修复批次号生成竞态条件

## 背景

`BatchNumberGenerator.generateBatchNo()` 使用应用层悲观锁 (`FOR UPDATE`) 生成每日递增批次号。在 PostgreSQL `READ COMMITTED` 隔离级别下，并发请求可能产生相同的 `batch_no`，触发 `UNIQUE` 约束冲突返回 409。

**方案**：使用 PostgreSQL `ADVISORY LOCK` + 原子性序列查询替代应用层 `FOR UPDATE`，从根本上消除竞态条件。

---

## 提议变更

### 数据库迁移

#### [NEW] [V2026021201__batch_no_advisory_lock.sql](file:///Users/user/nexusarchive/nexusarchive-java/src/main/resources/db/migration/V2026021201__batch_no_advisory_lock.sql)

创建一个 PostgreSQL 函数 `next_batch_no(date_part)`，内部使用 `pg_advisory_xact_lock` 确保同一日期的序号生成串行化：

```sql
CREATE OR REPLACE FUNCTION next_batch_no(p_date_part VARCHAR)
RETURNS VARCHAR AS $$
DECLARE
    v_lock_key BIGINT;
    v_max_seq INT;
    v_next_seq INT;
    v_batch_no VARCHAR;
BEGIN
    -- 用日期的哈希值作为 advisory lock key，确保同日期串行
    v_lock_key := hashtext('batch_no_' || p_date_part);
    PERFORM pg_advisory_xact_lock(v_lock_key);

    -- 在锁保护下查询当前最大序号
    SELECT COALESCE(MAX(
        CAST(SUBSTRING(batch_no FROM LENGTH('COL-' || p_date_part || '-') + 1) AS INT)
    ), 0) INTO v_max_seq
    FROM collection_batch
    WHERE batch_no LIKE 'COL-' || p_date_part || '-%';

    v_next_seq := v_max_seq + 1;
    v_batch_no := 'COL-' || p_date_part || '-' || LPAD(v_next_seq::TEXT, 5, '0');

    RETURN v_batch_no;
END;
$$ LANGUAGE plpgsql;
```

> 关键点：`pg_advisory_xact_lock` 是**事务级锁**，在事务提交/回滚时自动释放。比 `FOR UPDATE` 更安全——它锁的是**逻辑概念**而非具体行，因此不存在"新行不可见"的幻读问题。

---

### Java 代码

#### [MODIFY] [CollectionBatchMapper.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/mapper/CollectionBatchMapper.java)

新增方法调用 PG 函数：

```java
@Select("SELECT next_batch_no(#{datePart})")
String nextBatchNo(@Param("datePart") String datePart);
```

#### [MODIFY] [BatchNumberGenerator.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/collection/BatchNumberGenerator.java)

简化 `generateBatchNo()` 为单行调用：

```diff
-List<String> lockedBatchNos = batchMapper.selectBatchNosForUpdate(datePart);
-// ... 30行解析逻辑 ...
-String batchNo = prefix + String.format("%05d", nextSeq);
+String batchNo = batchMapper.nextBatchNo(datePart);
```

---

## 验证计划

### 自动化测试

现有单元测试 `CollectionBatchServiceTest.CreateBatchTests` 使用 Mockito mock `BatchNumberGenerator`，不会受影响。运行命令：

```bash
cd nexusarchive-java && mvn test -pl . -Dtest="CollectionBatchServiceTest" -Dsurefire.failIfNoSpecifiedTests=false
```

### 手动验证（curl）

1. 重启后端 (`mvn spring-boot:run`)
2. 发送两个**并发**批次创建请求，验证两个都成功且序号不冲突：

```bash
TOKEN=... # 登录获取
curl -s ... /batch/create ... &  # 请求 A
curl -s ... /batch/create ... &  # 请求 B（同时）
wait
```

3. 查询数据库确认两条记录的 `batch_no` 连续且无重复。
