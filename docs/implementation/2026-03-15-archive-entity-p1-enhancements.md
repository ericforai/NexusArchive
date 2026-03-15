# P1 档案实体增强实施总结

**实施日期**: 2026-03-15
**实施阶段**: Phase 1 - Archive Entity Enhancements
**工作流程**: Everything Claude Code 标准作业流程（4 阶段）

---

## 一、概述

本次 P1 阶段实现了档案实体（`Archive`）的核心增强，包括乐观锁并发控制、状态枚举类型安全和统一的状态转换管理。

### 核心目标

1. **并发安全**: 通过乐观锁防止并发更新冲突
2. **类型安全**: 用枚举替代魔法字符串，实现编译期检查
3. **状态机**: 统一管理档案状态转换规则

---

## 二、技术实现

### 2.1 乐观锁并发控制

#### 数据库变更

**迁移文件**: `V20260315__add_archive_version_and_status_enum.sql`

```sql
-- 添加乐观锁版本号字段
ALTER TABLE public.acc_archive
ADD COLUMN version INT NOT NULL DEFAULT 0;

-- 添加索引优化查询性能
CREATE INDEX idx_acc_archive_version ON public.acc_archive(id, version);
CREATE INDEX idx_acc_archive_status ON public.acc_archive(status);
CREATE INDEX idx_acc_archive_status_fonds ON public.acc_archive(status, fonds_no);
```

#### 实体变更

**文件**: `Archive.java`

```java
@Version
private Integer version;
```

#### MyBatis-Plus 配置

**文件**: `MyBatisPlusConfig.java`

```java
// 添加乐观锁拦截器（必须在分页拦截器之前）
interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
```

### 2.2 状态枚举

**文件**: `com.nexusarchive.common.enums.ArchiveStatus`

```java
public enum ArchiveStatus {
    DRAFT("draft", "草稿"),
    PENDING("pending", "待审核"),
    ARCHIVED("archived", "已归档");

    private final String code;
    private final String description;

    // 状态转换规则
    public boolean canTransitionTo(ArchiveStatus target) {
        return switch (this) {
            case DRAFT -> target == PENDING;
            case PENDING -> target == DRAFT || target == ARCHIVED;
            case ARCHIVED -> false; // 终态不可转换
        };
    }
}
```

**状态机流程图**:
```
    DRAFT ──────► PENDING ──────► ARCHIVED
         ◄───────                 (终态)
      (拒绝审核)
```

### 2.3 状态转换门面

**文件**: `ArchiveStateTransitionService.java`

```java
@Service
@RequiredArgsConstructor
public class ArchiveStateTransitionService implements ArchiveStateTransitionFacade {

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void transitionStatus(String archiveId, ArchiveStatusChangeRequest request, String userId) {
        // 1. 加载档案
        Archive archive = archiveReadService.getArchiveById(archiveId);

        // 2. 验证版本（乐观锁）
        if (request.getExpectedVersion() != null
            && !request.getExpectedVersion().equals(archive.getVersion())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT, "版本冲突");
        }

        // 3. 验证状态转换合法性
        ArchiveStatus currentStatus = ArchiveStatus.fromCode(archive.getStatus());
        if (!currentStatus.canTransitionTo(request.getTargetStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                "非法状态转换: " + currentStatus + " -> " + request.getTargetStatus());
        }

        // 4. 执行状态转换（version 自动递增）
        Archive update = new Archive();
        update.setId(archiveId);
        update.setStatus(request.getTargetStatus().getCode());
        archiveWriteService.updateArchive(archiveId, update);
    }
}
```

---

## 三、代码审查修复

### CRITICAL 级别问题修复

| 问题 | 修复方案 |
|------|---------|
| 乐观锁手动设置版本绕过 MyBatis-Plus | 移除 `update.setVersion()` 调用，让 MyBatis-Plus 自动处理 |

**修复前** (错误):
```java
update.setVersion(archive.getVersion() + 1); // 错误！绕过了 MyBatis-Plus
```

**修复后** (正确):
```java
// 不设置 version，让 MyBatis-Plus @Version 自动处理
update.setId(archiveId);
update.setStatus(request.getTargetStatus().getCode());
```

### HIGH 级别问题修复

| 问题 | 修复方案 |
|------|---------|
| status 列缺少索引 | 添加 `idx_acc_archive_status` 和复合索引 |
| null 状态静默处理 | 改为 fail-fast 抛出 `BusinessException` |
| 批量操作事务隔离不当 | 使用 `REQUIRES_NEW` 传播级别 |

---

## 四、测试覆盖

### 单元测试

**文件**: `ArchiveStateTransitionServiceTest.java`

| 测试用例 | 描述 |
|---------|------|
| `shouldAllowDraftToPendingTransition` | DRAFT → PENDING 转换 |
| `shouldAllowPendingToArchivedTransition` | PENDING → ARCHIVED 转换 |
| `shouldAllowPendingToDraftTransition` | PENDING → DRAFT 转换（拒绝审核） |
| `shouldRejectArchivedToDraftTransition` | ARCHIVED → DRAFT 转换（终态不可逆） |
| `shouldRejectDraftToArchivedTransition` | DRAFT → ARCHIVED 转换（需先经过 PENDING） |
| `shouldThrowOnOptimisticLockConflict` | 乐观锁冲突 |
| `shouldThrowWhenExpectedVersionMismatch` | 预期版本不匹配 |
| `batchTransitionShouldReturnSuccessCount` | 批量状态转换 |

**测试结果**: 303 个测试运行，3 个预存失败（与 P1 无关）

---

## 五、API 变更

### 新增接口

```java
// ArchiveStateTransitionFacade
void transitionStatus(String archiveId, ArchiveStatusChangeRequest request, String userId);
int batchTransitionStatus(List<String> archiveIds, ArchiveStatus targetStatus, String userId);
boolean canTransition(String archiveId, ArchiveStatus targetStatus);
```

### DTO 变更

```java
// ArchiveStatusChangeRequest
public class ArchiveStatusChangeRequest {
    private ArchiveStatus targetStatus;
    private Integer expectedVersion;  // 乐观锁版本检查
    private String reason;
}
```

---

## 六、文件清单

### 新增文件

| 文件 | 描述 |
|------|------|
| `common/enums/ArchiveStatus.java` | 档案状态枚举 |
| `modules/archivecore/app/ArchiveStateTransitionFacade.java` | 状态转换门面接口 |
| `modules/archivecore/app/ArchiveStateTransitionService.java` | 状态转换服务实现 |
| `modules/archivecore/api/dto/ArchiveStatusChangeRequest.java` | 状态变更请求 DTO |
| `src/test/java/.../ArchiveStateTransitionServiceTest.java` | 状态转换测试 |
| `src/test/java/.../ArchiveStatusTest.java` | 枚举测试 |
| `src/main/resources/db/migration/V20260315__add_archive_version_and_status_enum.sql` | 数据库迁移 |

### 修改文件

| 文件 | 变更 |
|------|------|
| `entity/Archive.java` | 添加 `@Version` 字段 |
| `config/MyBatisPlusConfig.java` | 添加乐观锁拦截器 |
| `common/exception/ErrorCode.java` | 添加 `VERSION_CONFLICT`, `INVALID_STATE_TRANSITION` |
| `modules/archivecore/app/ArchiveFacade.java` | 添加 `changeStatus` 方法 |
| `modules/archivecore/app/ArchiveApplicationService.java` | 实现状态转换委托 |

---

## 七、性能影响

| 指标 | 影响 |
|------|------|
| 数据库大小 | +4 bytes/行（version 字段） |
| 查询性能 | 提升（status 索引） |
| 并发冲突 | 检测并失败快速（乐观锁） |
| 更新开销 +1 WHERE 条件 | 忽略不计 |

---

## 八、向后兼容性

### 数据库兼容

- ✅ 现有数据自动设置 `version = 0`
- ✅ 现有 `status` 字段值保持不变（字符串存储）
- ✅ 新代码使用枚举，但数据库仍为字符串

### API 兼容

- ✅ 现有 API 不受影响
- ✅ 新增的状态转换 API 为可选功能

---

## 九、后续工作（P2 阶段）

- [ ] 审计日志记录（状态变更历史）
- [ ] 状态变更事件发布
- [ ] 批量操作异步化
- [ ] 状态转换可视化

---

## 十、参考资料

- [MyBatis-Plus 乐观锁文档](https://baomidou.com/pages/2976a3/)
- [DA/T 94-2022 电子会计档案管理规范](../电子会计档案管理规范.pdf)
- [Everything Claude Code 标准作业流程](https://github.com/anthropics/claude-code)
