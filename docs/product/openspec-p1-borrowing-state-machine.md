# 电子会计档案系统 - 借阅状态机完整性 OpenSpec

> **版本**: v1.0  
> **日期**: 2025-01  
> **对齐基准**: [开发路线图 v1.0](../planning/development_roadmap_v1.0.md)  
> **优先级**: **P1（重要但非阻塞 - 短期优化）**  
> **当前状态**: 待开发

---

## 📊 功能概览

| 功能模块 | 路线图章节 | 优先级 | 预计工作量 | 依赖关系 |
|---------|-----------|--------|-----------|---------|
| 借阅状态机完整性 | 阶段三：借阅与流程 | **P1** | 1-2 周 | 无 |

---

## 🎯 业务目标

**用户故事**: 作为档案管理员，我需要确保借阅流程的状态转换完整、可追溯，系统应正确记录每个状态变更的时间、操作人和原因，防止非法状态转换。

**业务价值**:
- 确保借阅流程的完整性和可追溯性
- 防止非法状态转换，提高数据一致性
- 完善审计日志，满足合规要求
- 提升用户体验（清晰的状态显示和操作按钮）

**问题背景**:
- 借阅服务已实现，但状态机完整性需要确认
- 需要验证所有合法的状态转换路径
- 需要补充状态流转的审计日志记录

---

## 📋 功能范围

### 1. 状态机定义

#### 1.1 借阅状态枚举

**状态定义**:
```java
public enum BorrowingStatus {
    PENDING,        // 待审批
    APPROVED,       // 已批准
    REJECTED,       // 已拒绝
    BORROWED,       // 已借出
    RETURNED,       // 已归还
    OVERDUE,        // 逾期
    CANCELLED,      // 已取消
    LOST            // 丢失
}
```

#### 1.2 状态转换规则

**合法状态转换路径**:

```
PENDING
  ├─> APPROVED (审批通过)
  ├─> REJECTED (审批拒绝)
  └─> CANCELLED (申请人取消)

APPROVED
  ├─> BORROWED (确认借出)
  └─> CANCELLED (取消借阅)

BORROWED
  ├─> RETURNED (正常归还)
  ├─> OVERDUE (逾期，系统自动或手动标记)
  ├─> LOST (标记丢失)
  └─> CANCELLED (特殊情况取消)

REJECTED (终态)
RETURNED (终态)
OVERDUE (可转换回 RETURNED 或 LOST)
LOST (终态)
CANCELLED (终态)
```

**状态转换约束**:
- `PENDING` 状态只能转换为 `APPROVED`、`REJECTED` 或 `CANCELLED`
- `APPROVED` 状态只能转换为 `BORROWED` 或 `CANCELLED`
- `BORROWED` 状态可以转换为 `RETURNED`、`OVERDUE`、`LOST` 或 `CANCELLED`
- `OVERDUE` 状态可以转换为 `RETURNED` 或 `LOST`
- 终态（`REJECTED`、`RETURNED`、`LOST`、`CANCELLED`）不能再次转换（除非特殊情况）

### 2. 状态转换服务

#### 2.1 服务接口设计

```java
/**
 * 借阅状态转换服务
 */
public interface BorrowingStateTransitionService {
    /**
     * 审批借阅申请
     * 
     * @param borrowingId 借阅ID
     * @param approved 是否批准
     * @param comment 审批意见
     * @param approverId 审批人ID
     */
    void approveBorrowing(String borrowingId, boolean approved, String comment, String approverId);
    
    /**
     * 确认借出
     * 
     * @param borrowingId 借阅ID
     * @param operatorId 操作人ID
     */
    void confirmBorrowed(String borrowingId, String operatorId);
    
    /**
     * 归还档案
     * 
     * @param borrowingId 借阅ID
     * @param returnDate 归还日期
     * @param condition 归还状态（良好/损坏/丢失）
     * @param operatorId 操作人ID
     */
    void returnArchive(String borrowingId, LocalDate returnDate, String condition, String operatorId);
    
    /**
     * 标记逾期
     * 
     * @param borrowingId 借阅ID
     * @param operatorId 操作人ID
     */
    void markOverdue(String borrowingId, String operatorId);
    
    /**
     * 标记丢失
     * 
     * @param borrowingId 借阅ID
     * @param reason 丢失原因
     * @param operatorId 操作人ID
     */
    void markLost(String borrowingId, String reason, String operatorId);
    
    /**
     * 取消借阅
     * 
     * @param borrowingId 借阅ID
     * @param reason 取消原因
     * @param operatorId 操作人ID
     */
    void cancelBorrowing(String borrowingId, String reason, String operatorId);
    
    /**
     * 验证状态转换是否合法
     * 
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否合法
     */
    boolean isTransitionValid(BorrowingStatus currentStatus, BorrowingStatus targetStatus);
}
```

#### 2.2 状态转换表

| 当前状态 | 目标状态 | 触发方式 | 权限要求 |
|---------|---------|---------|---------|
| PENDING | APPROVED | 审批通过 | 审批权限 |
| PENDING | REJECTED | 审批拒绝 | 审批权限 |
| PENDING | CANCELLED | 取消申请 | 申请人或管理员 |
| APPROVED | BORROWED | 确认借出 | 档案管理员 |
| APPROVED | CANCELLED | 取消借阅 | 申请人或管理员 |
| BORROWED | RETURNED | 归还档案 | 档案管理员 |
| BORROWED | OVERDUE | 标记逾期 | 系统自动或管理员 |
| BORROWED | LOST | 标记丢失 | 档案管理员 |
| BORROWED | CANCELLED | 特殊情况取消 | 管理员 |
| OVERDUE | RETURNED | 归还档案 | 档案管理员 |
| OVERDUE | LOST | 标记丢失 | 档案管理员 |

### 3. 审计日志记录

#### 3.1 状态变更审计

**记录内容**:
- 借阅ID
- 原状态
- 新状态
- 操作时间
- 操作人ID
- 操作人姓名
- 操作原因/备注
- 操作类型（APPROVE、REJECT、BORROW、RETURN、OVERDUE、LOST、CANCEL）

**审计日志格式**:
```java
public class BorrowingStateChangeLog {
    private String id;
    private String borrowingId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime changeTime;
    private String operatorId;
    private String operatorName;
    private String reason;
    private String changeType; // APPROVE, REJECT, BORROW, RETURN, etc.
}
```

### 4. 定时任务：逾期检测

#### 4.1 自动标记逾期

**实现要求**:
- 定时任务（每日凌晨执行）
- 扫描 `status = 'BORROWED'` 且 `expectedReturnDate < 当前日期` 的记录
- 自动将状态更新为 `OVERDUE`
- 记录状态变更审计日志
- 发送逾期提醒通知（可选）

**技术规格**:
```java
@Service
public class BorrowingOverdueDetectionService {
    
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    @DistributedLock(key = "borrowing:overdue:scan", timeout = 3600)
    public void scanAndMarkOverdue() {
        // 扫描逾期记录
        List<Borrowing> overdueList = findOverdueBorrowings();
        
        for (Borrowing borrowing : overdueList) {
            stateTransitionService.markOverdue(borrowing.getId(), "SYSTEM");
            // 发送通知（可选）
            notificationService.sendOverdueNotification(borrowing);
        }
    }
}
```

---

## 🔧 技术规格

### 5. 状态机实现

#### 5.1 状态转换验证器

```java
@Component
public class BorrowingStateTransitionValidator {
    
    // 定义合法的状态转换
    private static final Map<BorrowingStatus, Set<BorrowingStatus>> VALID_TRANSITIONS = Map.of(
        BorrowingStatus.PENDING, Set.of(
            BorrowingStatus.APPROVED,
            BorrowingStatus.REJECTED,
            BorrowingStatus.CANCELLED
        ),
        BorrowingStatus.APPROVED, Set.of(
            BorrowingStatus.BORROWED,
            BorrowingStatus.CANCELLED
        ),
        BorrowingStatus.BORROWED, Set.of(
            BorrowingStatus.RETURNED,
            BorrowingStatus.OVERDUE,
            BorrowingStatus.LOST,
            BorrowingStatus.CANCELLED
        ),
        BorrowingStatus.OVERDUE, Set.of(
            BorrowingStatus.RETURNED,
            BorrowingStatus.LOST
        )
    );
    
    /**
     * 验证状态转换是否合法
     */
    public boolean isValidTransition(BorrowingStatus from, BorrowingStatus to) {
        // 终态不能转换
        if (isTerminalStatus(from)) {
            return false;
        }
        
        Set<BorrowingStatus> allowed = VALID_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
    
    private boolean isTerminalStatus(BorrowingStatus status) {
        return status == BorrowingStatus.REJECTED ||
               status == BorrowingStatus.RETURNED ||
               status == BorrowingStatus.LOST ||
               status == BorrowingStatus.CANCELLED;
    }
}
```

#### 5.2 状态转换服务实现

```java
@Service
@RequiredArgsConstructor
public class BorrowingStateTransitionServiceImpl implements BorrowingStateTransitionService {
    
    private final BorrowingMapper borrowingMapper;
    private final BorrowingStateTransitionValidator validator;
    private final AuditLogService auditLogService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveBorrowing(String borrowingId, boolean approved, String comment, String approverId) {
        Borrowing borrowing = borrowingMapper.selectById(borrowingId);
        if (borrowing == null) {
            throw new BusinessException("借阅记录不存在");
        }
        
        BorrowingStatus currentStatus = BorrowingStatus.valueOf(borrowing.getStatus());
        BorrowingStatus targetStatus = approved ? BorrowingStatus.APPROVED : BorrowingStatus.REJECTED;
        
        // 验证状态转换
        if (!validator.isValidTransition(currentStatus, targetStatus)) {
            throw new BusinessException(
                String.format("非法状态转换: %s -> %s", currentStatus, targetStatus)
            );
        }
        
        // 更新状态
        borrowing.setStatus(targetStatus.name());
        if (approved) {
            borrowing.setApprovedAt(LocalDateTime.now());
            borrowing.setApproverId(approverId);
        } else {
            borrowing.setRejectedAt(LocalDateTime.now());
            borrowing.setRejectorId(approverId);
        }
        borrowingMapper.updateById(borrowing);
        
        // 记录审计日志
        auditLogService.saveAuditLog(
            "BORROWING_STATUS_CHANGE",
            "BORROWING",
            borrowingId,
            "SUCCESS",
            String.format("状态变更: %s -> %s, 原因: %s", currentStatus, targetStatus, comment),
            approverId
        );
        
        // 记录状态变更日志
        saveStateChangeLog(borrowingId, currentStatus, targetStatus, approverId, comment);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBorrowed(String borrowingId, String operatorId) {
        Borrowing borrowing = borrowingMapper.selectById(borrowingId);
        BorrowingStatus currentStatus = BorrowingStatus.valueOf(borrowing.getStatus());
        
        if (!validator.isValidTransition(currentStatus, BorrowingStatus.BORROWED)) {
            throw new BusinessException("当前状态不允许确认借出");
        }
        
        borrowing.setStatus(BorrowingStatus.BORROWED.name());
        borrowing.setBorrowedAt(LocalDateTime.now());
        borrowingMapper.updateById(borrowing);
        
        saveStateChangeLog(borrowingId, currentStatus, BorrowingStatus.BORROWED, operatorId, "确认借出");
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnArchive(String borrowingId, LocalDate returnDate, String condition, String operatorId) {
        Borrowing borrowing = borrowingMapper.selectById(borrowingId);
        BorrowingStatus currentStatus = BorrowingStatus.valueOf(borrowing.getStatus());
        
        BorrowingStatus targetStatus = BorrowingStatus.RETURNED;
        if (!validator.isValidTransition(currentStatus, targetStatus)) {
            throw new BusinessException("当前状态不允许归还");
        }
        
        borrowing.setStatus(targetStatus.name());
        borrowing.setReturnedAt(returnDate != null ? returnDate.atStartOfDay() : LocalDateTime.now());
        borrowing.setReturnCondition(condition);
        borrowingMapper.updateById(borrowing);
        
        saveStateChangeLog(borrowingId, currentStatus, targetStatus, operatorId, 
            String.format("归还档案，状态: %s", condition));
    }
    
    // ... 其他方法实现类似
    
    private void saveStateChangeLog(String borrowingId, BorrowingStatus from, BorrowingStatus to, 
                                   String operatorId, String reason) {
        // 保存状态变更日志到数据库
        BorrowingStateChangeLog log = new BorrowingStateChangeLog();
        log.setBorrowingId(borrowingId);
        log.setOldStatus(from.name());
        log.setNewStatus(to.name());
        log.setChangeTime(LocalDateTime.now());
        log.setOperatorId(operatorId);
        log.setReason(reason);
        // ... 保存到数据库
    }
}
```

### 6. 数据库变更

#### 6.1 状态变更日志表

```sql
-- 借阅状态变更日志表
CREATE TABLE IF NOT EXISTS borrowing_state_change_log (
    id VARCHAR(32) PRIMARY KEY,
    borrowing_id VARCHAR(32) NOT NULL COMMENT '借阅ID',
    old_status VARCHAR(20) NOT NULL COMMENT '原状态',
    new_status VARCHAR(20) NOT NULL COMMENT '新状态',
    change_time TIMESTAMP NOT NULL COMMENT '变更时间',
    operator_id VARCHAR(32) NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) COMMENT '操作人姓名',
    reason TEXT COMMENT '变更原因',
    change_type VARCHAR(20) NOT NULL COMMENT '变更类型: APPROVE, REJECT, BORROW, RETURN, OVERDUE, LOST, CANCEL',
    
    INDEX idx_borrowing_log_borrowing (borrowing_id, change_time),
    INDEX idx_borrowing_log_status (old_status, new_status),
    INDEX idx_borrowing_log_time (change_time)
);

COMMENT ON TABLE borrowing_state_change_log IS '借阅状态变更日志表';
```

---

## 🧪 测试要求

### 7.1 单元测试

**测试用例**:
- 所有合法的状态转换
- 非法的状态转换（应抛出异常）
- 状态转换验证器逻辑
- 审计日志记录

### 7.2 集成测试

**测试场景**:
- 完整的借阅流程（申请 -> 审批 -> 借出 -> 归还）
- 审批拒绝流程
- 取消流程
- 逾期标记流程
- 丢失标记流程

### 7.3 状态机测试

**测试矩阵**:
- 测试所有状态对（from, to）的组合
- 验证合法转换成功，非法转换失败
- 验证终态不能转换

---

## 📝 开发检查清单

- [ ] 审查现有 `BorrowingService` 实现
- [ ] 定义状态枚举和转换规则
- [ ] 创建状态转换验证器
- [ ] 实现状态转换服务
- [ ] 创建状态变更日志表（数据库迁移脚本）
- [ ] 实现状态变更审计日志记录
- [ ] 实现定时任务：逾期检测
- [ ] 更新 `BorrowingService` 使用状态转换服务
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 更新前端借阅状态显示
- [ ] 更新前端操作按钮（根据状态显示/隐藏）
- [ ] 更新相关文档

---

## 🔗 相关文档

- 开发路线图：`docs/planning/development_roadmap_v1.0.md`
- 缺口分析报告：`docs/reports/roadmap-gap-analysis-2025-01.md`
- 借阅服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/BorrowingService.java`
- 借阅实体：`nexusarchive-java/src/main/java/com/nexusarchive/entity/Borrowing.java`

---

**文档状态**: ✅ 已完成  
**下一步**: 审查现有代码，开始开发实现





