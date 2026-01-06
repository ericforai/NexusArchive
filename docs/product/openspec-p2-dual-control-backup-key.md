# 电子会计档案系统 - 双人双控备份密钥管理 OpenSpec

> **版本**: v1.0  
> **日期**: 2025-01  
> **对齐基准**: [开发路线图 v1.0](../planning/development_roadmap_v1.0.md)、[PRD v1.0](prd-v1.0.md)  
> **优先级**: **P2（可选功能 - 中长期规划）**  
> **当前状态**: 待开发

---

## 📊 功能概览

| 功能模块 | 路线图章节 | PRD 章节 | 优先级 | 预计工作量 | 依赖关系 |
|---------|-----------|---------|--------|-----------|---------|
| 双人双控备份密钥管理 | 阶段四（增强） | Section 2.3 | **P2** | 3-4 周 | 备份恢复功能 |

---

## 🎯 业务目标

**用户故事**: 作为安全管理员，我需要确保备份密钥的安全管理，系统应实现双人双控机制，任何备份密钥的操作都需要两人审批，防止单人权限过大。

**业务价值**:
- 增强备份密钥的安全性
- 防止单人权限过大导致的安全风险
- 满足合规要求（审计验收）
- 提供完整的审批流程和审计日志

**合规要求**:
- **PRD Section 2.3**: 备份介质全量加密，解密密钥由 `SecAdmin` 托管，P2 引入双人双控
- **审计验收要求**: 备份恢复需绑定审批票据与 TraceID，全链路留痕

**当前状态**:
- 备份功能已实现（假设）
- 密钥管理基础功能已存在
- 双人双控机制待实现

---

## 📋 功能范围

### 1. 双人双控机制设计

#### 1.1 密钥分片机制

**分片策略**:
- **Shamir 密钥分片**: 使用 Shamir Secret Sharing (SSS) 算法
- **分片数量**: 密钥分成 2 个分片（Shamir 2-of-2）
- **分片存储**: 每个分片由不同的管理员保管
- **密钥恢复**: 需要 2 个分片才能恢复完整密钥

**安全要求**:
- 单个分片无法恢复密钥
- 分片独立存储（不同的密钥库或物理设备）
- 分片传输加密

#### 1.2 审批流程

**审批角色**:
- **发起人**: 执行备份/恢复操作的人员（`SysAdmin`）
- **审批人1**: 密钥分片持有者 1（`SecAdmin1`）
- **审批人2**: 密钥分片持有者 2（`SecAdmin2`）

**审批流程**:
```
1. 发起人申请备份/恢复操作
   ↓
2. 系统生成审批申请（包含操作类型、资源范围等）
   ↓
3. 审批人1审批（提供密钥分片1）
   ↓
4. 审批人2审批（提供密钥分片2）
   ↓
5. 系统合并密钥分片，恢复完整密钥
   ↓
6. 执行备份/恢复操作
   ↓
7. 记录操作日志和审计信息
```

**审批状态**:
- `PENDING` - 待审批
- `APPROVED_BY_ONE` - 已通过一人审批
- `APPROVED_BY_BOTH` - 已通过两人审批
- `REJECTED` - 已拒绝
- `EXPIRED` - 已过期
- `EXECUTED` - 已执行

### 2. 密钥分片服务

#### 2.1 服务接口设计

```java
/**
 * 密钥分片服务
 */
public interface KeyShardService {
    /**
     * 生成密钥分片（Shamir 分片）
     * 
     * @param masterKey 主密钥
     * @param totalShards 总分片数
     * @param requiredShards 恢复所需分片数
     * @return 密钥分片列表
     */
    List<KeyShard> generateKeyShards(byte[] masterKey, int totalShards, int requiredShards);
    
    /**
     * 合并密钥分片，恢复主密钥
     * 
     * @param shards 密钥分片列表
     * @return 恢复的主密钥
     */
    byte[] combineKeyShards(List<KeyShard> shards);
    
    /**
     * 加密密钥分片（使用审批人公钥）
     * 
     * @param shard 密钥分片
     * @param publicKey 审批人公钥
     * @return 加密后的分片
     */
    EncryptedKeyShard encryptKeyShard(KeyShard shard, PublicKey publicKey);
    
    /**
     * 解密密钥分片（使用审批人私钥）
     * 
     * @param encryptedShard 加密的分片
     * @param privateKey 审批人私钥
     * @return 解密后的分片
     */
    KeyShard decryptKeyShard(EncryptedKeyShard encryptedShard, PrivateKey privateKey);
}
```

#### 2.2 密钥分片数据结构

```java
public class KeyShard {
    private String shardId;           // 分片ID
    private int shardIndex;           // 分片索引（1, 2, ...）
    private byte[] shardData;         // 分片数据
    private String holderId;          // 持有者ID（审批人ID）
    private LocalDateTime createdAt;  // 创建时间
}

public class EncryptedKeyShard {
    private String shardId;
    private byte[] encryptedData;     // 加密后的分片数据
    private String algorithm;         // 加密算法（SM2）
    private String holderId;          // 持有者ID
}
```

### 3. 审批流程服务

#### 3.1 服务接口设计

```java
/**
 * 双人双控审批服务
 */
public interface DualControlApprovalService {
    /**
     * 创建备份/恢复审批申请
     * 
     * @param operationType 操作类型（BACKUP, RESTORE）
     * @param resourceScope 资源范围
     * @param applicantId 申请人ID
     * @return 审批申请ID
     */
    String createApprovalRequest(String operationType, String resourceScope, String applicantId);
    
    /**
     * 审批（提供密钥分片）
     * 
     * @param approvalId 审批申请ID
     * @param approverId 审批人ID
     * @param keyShard 密钥分片（加密传输）
     * @param comment 审批意见
     */
    void approve(String approvalId, String approverId, EncryptedKeyShard keyShard, String comment);
    
    /**
     * 拒绝审批
     * 
     * @param approvalId 审批申请ID
     * @param approverId 审批人ID
     * @param reason 拒绝原因
     */
    void reject(String approvalId, String approverId, String reason);
    
    /**
     * 执行备份/恢复操作（需要两个分片都已审批）
     * 
     * @param approvalId 审批申请ID
     * @return 操作结果（包含 TraceID）
     */
    BackupRestoreResult executeOperation(String approvalId);
    
    /**
     * 查询审批状态
     * 
     * @param approvalId 审批申请ID
     * @return 审批状态
     */
    ApprovalStatus getApprovalStatus(String approvalId);
}
```

#### 3.2 审批申请实体

```java
public class BackupKeyApproval {
    private String id;                    // 审批ID
    private String operationType;         // 操作类型: BACKUP, RESTORE
    private String resourceScope;         // 资源范围
    private String applicantId;           // 申请人ID
    private String applicantName;         // 申请人姓名
    private ApprovalStatus status;        // 审批状态
    private String approver1Id;           // 审批人1 ID
    private String approver2Id;           // 审批人2 ID
    private EncryptedKeyShard shard1;     // 分片1（审批人1提供）
    private EncryptedKeyShard shard2;     // 分片2（审批人2提供）
    private LocalDateTime shard1ApprovedAt; // 分片1审批时间
    private LocalDateTime shard2ApprovedAt; // 分片2审批时间
    private String traceId;               // 操作追踪ID
    private LocalDateTime createdAt;      // 创建时间
    private LocalDateTime expiresAt;      // 过期时间
}
```

### 4. 数据库变更

#### 4.1 审批申请表

```sql
-- 备份密钥审批申请表
CREATE TABLE IF NOT EXISTS sys_backup_key_approval (
    id VARCHAR(64) PRIMARY KEY,
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型: BACKUP, RESTORE',
    resource_scope TEXT COMMENT '资源范围（JSON）',
    applicant_id VARCHAR(32) NOT NULL COMMENT '申请人ID',
    applicant_name VARCHAR(100) COMMENT '申请人姓名',
    status VARCHAR(20) NOT NULL COMMENT '审批状态: PENDING, APPROVED_BY_ONE, APPROVED_BY_BOTH, REJECTED, EXPIRED, EXECUTED',
    approver1_id VARCHAR(32) COMMENT '审批人1 ID',
    approver2_id VARCHAR(32) COMMENT '审批人2 ID',
    shard1_encrypted TEXT COMMENT '分片1（加密）',
    shard2_encrypted TEXT COMMENT '分片2（加密）',
    shard1_approved_at TIMESTAMP COMMENT '分片1审批时间',
    shard2_approved_at TIMESTAMP COMMENT '分片2审批时间',
    shard1_comment TEXT COMMENT '审批人1意见',
    shard2_comment TEXT COMMENT '审批人2意见',
    trace_id VARCHAR(64) COMMENT '操作追踪ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP COMMENT '过期时间',
    executed_at TIMESTAMP COMMENT '执行时间',
    
    INDEX idx_approval_status (status, created_at),
    INDEX idx_approval_applicant (applicant_id, created_at),
    INDEX idx_approval_trace (trace_id)
);

COMMENT ON TABLE sys_backup_key_approval IS '备份密钥双人双控审批申请表';
```

#### 4.2 审批日志表

```sql
-- 审批日志表
CREATE TABLE IF NOT EXISTS sys_backup_key_approval_log (
    id VARCHAR(64) PRIMARY KEY,
    approval_id VARCHAR(64) NOT NULL COMMENT '审批申请ID',
    action VARCHAR(20) NOT NULL COMMENT '操作: CREATE, APPROVE, REJECT, EXECUTE',
    operator_id VARCHAR(32) NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) COMMENT '操作人姓名',
    comment TEXT COMMENT '操作意见',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_log_approval (approval_id, created_at),
    INDEX idx_log_operator (operator_id, created_at)
);

COMMENT ON TABLE sys_backup_key_approval_log IS '备份密钥审批日志表';
```

### 5. 集成到备份恢复流程

#### 5.1 备份流程

```java
@Service
@RequiredArgsConstructor
public class BackupService {
    
    private final DualControlApprovalService approvalService;
    private final BackupKeyShardService keyShardService;
    private final AuditLogService auditLogService;
    
    /**
     * 申请备份（需要双人双控审批）
     */
    public String requestBackup(String resourceScope, String applicantId) {
        // 1. 创建审批申请
        String approvalId = approvalService.createApprovalRequest(
            "BACKUP", 
            resourceScope, 
            applicantId
        );
        
        // 2. 记录审计日志
        auditLogService.saveAuditLog(
            "BACKUP_REQUEST",
            "BACKUP",
            approvalId,
            "PENDING",
            "备份申请已提交，等待双人双控审批",
            applicantId
        );
        
        return approvalId;
    }
    
    /**
     * 执行备份（需要两个分片都已审批）
     */
    @Transactional(rollbackFor = Exception.class)
    public BackupResult executeBackup(String approvalId) {
        // 1. 检查审批状态
        ApprovalStatus status = approvalService.getApprovalStatus(approvalId);
        if (status != ApprovalStatus.APPROVED_BY_BOTH) {
            throw new BusinessException("备份操作需要双人双控审批");
        }
        
        // 2. 执行备份操作（使用合并后的密钥）
        BackupKeyApproval approval = getApproval(approvalId);
        byte[] masterKey = mergeKeyShards(approval);
        
        // 3. 执行备份
        String traceId = generateTraceId();
        BackupResult result = doBackup(masterKey, traceId);
        
        // 4. 更新审批状态
        markAsExecuted(approvalId, traceId);
        
        // 5. 记录审计日志
        auditLogService.saveAuditLog(
            "BACKUP_EXECUTE",
            "BACKUP",
            approvalId,
            "SUCCESS",
            String.format("备份操作已执行，TraceID: %s", traceId),
            "SYSTEM"
        );
        
        return result;
    }
    
    private byte[] mergeKeyShards(BackupKeyApproval approval) {
        // 1. 解密分片1和分片2（使用审批人私钥）
        KeyShard shard1 = decryptShard(approval.getShard1(), approval.getApprover1Id());
        KeyShard shard2 = decryptShard(approval.getShard2(), approval.getApprover2Id());
        
        // 2. 合并分片，恢复主密钥
        return keyShardService.combineKeyShards(List.of(shard1, shard2));
    }
}
```

---

## 🧪 测试要求

### 6.1 单元测试

**测试用例**:
- 密钥分片生成和合并
- 审批流程状态转换
- 分片加密解密
- 审批超时处理

### 6.2 集成测试

**测试场景**:
- 完整的双人双控审批流程
- 备份/恢复操作集成
- 审批拒绝场景
- 审批超时场景

### 6.3 安全测试

**测试内容**:
- 单个分片无法恢复密钥
- 分片传输加密
- 审批流程的不可绕过性
- 审计日志完整性

---

## 📝 开发检查清单

- [ ] 设计双人双控流程
- [ ] 实现密钥分片机制（Shamir Secret Sharing）
- [ ] 实现密钥分片服务
- [ ] 实现审批流程服务
- [ ] 创建审批申请表和日志表（数据库迁移脚本）
- [ ] 集成到备份恢复流程
- [ ] 实现前端审批界面
- [ ] 实现前端分片输入界面（安全传输）
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 编写安全测试
- [ ] 更新相关文档

---

## 🔗 相关文档

- 开发路线图：`docs/planning/development_roadmap_v1.0.md`
- PRD v1.0：`docs/product/prd-v1.0.md` (Section 2.3)
- 缺口分析报告：`docs/reports/roadmap-gap-analysis-2025-01.md`
- Shamir Secret Sharing: https://en.wikipedia.org/wiki/Shamir%27s_Secret_Sharing

---

**文档状态**: ✅ 已完成  
**下一步**: 设计详细的双人双控流程，开始开发实现





