# 电子会计档案系统 - P0 关键功能 OpenSpec

> **版本**: v1.0  
> **日期**: 2025-01  
> **对齐基准**: [PRD v1.0](prd-v1.0.md)  
> **优先级**: **P0（必须实现）**  
> **当前状态**: 待开发

---

## 📊 功能概览

| 功能模块 | PRD 章节 | 优先级 | 预计工作量 | 依赖关系 |
|---------|---------|--------|-----------|---------|
| 跨全宗访问授权票据 | Section 2.4 | **P0** | 5-7 天 | 无 |
| 审计证据链验真接口 | Section 6.2 | **P0** | 3-5 天 | 审计日志服务 |

---

## 🎯 Sprint 5 目标：P0 关键功能实现

### 5.1 业务目标

**用户故事 1**: 作为审计人员，我需要跨全宗查询档案，但必须经过审批并获得授权票据，系统记录所有跨全宗访问行为。

**用户故事 2**: 作为审计管理员，我需要验证审计日志的完整性，确保日志未被篡改，并能导出证据包用于合规审计。

---

## 📋 功能一：跨全宗访问授权票据 (P0)

### 5.2.1 PRD 来源与合规要求

**PRD 来源**: Section 2.4 - 跨全宗访问授权票据（Auth Ticket）

**合规要求**:
- **GB/T 39784-2021**: 跨组织数据访问必须经过授权审批
- **DA/T 94-2022**: 跨全宗访问必须可追溯、可审计
- **安全要求**: 默认拒绝（Default Deny），未授权不得访问

**核心约束**:
- 跨全宗访问**必须**绑定 `auth_ticket_id`
- 票据必须在有效期内（`expires_at`）
- 必须经过双人审批（审批链记录）
- 所有跨全宗访问必须记录审计日志

### 5.2.2 功能范围

#### 5.2.2.1 授权票据申请 (P0)

**实现要求**:
- 用户申请跨全宗访问权限
- 填写申请信息：目标全宗、访问范围（期间/类型/关键词）、有效期
- 系统生成授权票据（状态：`PENDING`）
- 触发审批流程（双人审批）

**技术规格**:

```java
@Service
@Transactional(rollbackFor = Exception.class)
public class AuthTicketService {
    /**
     * 创建授权票据申请
     * 
     * @param applicantId 申请人ID
     * @param sourceFonds 源全宗号（申请人所属全宗）
     * @param targetFonds 目标全宗号
     * @param scope 访问范围（JSON格式）
     * @param expiresAt 有效期（必须 >= 当前时间 + 1天，<= 当前时间 + 90天）
     * @param reason 申请原因
     * @return 授权票据ID
     */
    String createAuthTicket(String applicantId, String sourceFonds, 
                            String targetFonds, AuthScope scope, 
                            LocalDateTime expiresAt, String reason);
    
    /**
     * 查询授权票据详情
     */
    AuthTicketDetail getAuthTicketDetail(String ticketId);
    
    /**
     * 撤销授权票据（仅申请人或管理员可撤销）
     */
    void revokeAuthTicket(String ticketId, String operatorId, String reason);
}
```

**数据模型**:

```java
public class AuthTicket {
    private String id;
    private String applicantId;
    private String applicantName;
    private String sourceFonds;
    private String targetFonds;
    private String scope;  // JSON格式：{ "archiveYears": [2020, 2021], "docTypes": ["凭证", "报表"], "keywords": ["财务报表"] }
    private LocalDateTime expiresAt;
    private String status;  // PENDING, APPROVED, REJECTED, REVOKED, EXPIRED
    private String approvalSnapshot;  // JSON格式的审批链
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedTime;
}

public class AuthScope {
    private List<Integer> archiveYears;  // 可选：归档年度列表
    private List<String> docTypes;  // 可选：档案类型列表
    private List<String> keywords;  // 可选：关键词列表
    private String accessType;  // READ_ONLY, READ_WRITE（默认只读）
}
```

**数据库表结构** (参考 PRD 4.1):

```sql
-- 授权票据表
CREATE TABLE IF NOT EXISTS auth_ticket (
    id VARCHAR(32) PRIMARY KEY,
    applicant_id VARCHAR(32) NOT NULL,
    applicant_name VARCHAR(100),
    source_fonds VARCHAR(50) NOT NULL,
    target_fonds VARCHAR(50) NOT NULL,
    scope TEXT NOT NULL,  -- JSON格式
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_snapshot TEXT,  -- JSON格式的审批链
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    
    -- 索引
    INDEX idx_auth_ticket_applicant (applicant_id, status),
    INDEX idx_auth_ticket_target (target_fonds, status, expires_at),
    INDEX idx_auth_ticket_expires (expires_at, status)
);

COMMENT ON TABLE auth_ticket IS '跨全宗访问授权票据表';
COMMENT ON COLUMN auth_ticket.scope IS '访问范围（JSON格式）：{ "archiveYears": [2020, 2021], "docTypes": ["凭证"], "keywords": [] }';
COMMENT ON COLUMN auth_ticket.approval_snapshot IS '审批链快照（JSON格式）：{ "firstApprover": {...}, "secondApprover": {...} }';
COMMENT ON COLUMN auth_ticket.status IS '状态: PENDING(待审批), APPROVED(已批准), REJECTED(已拒绝), REVOKED(已撤销), EXPIRED(已过期)';
```

#### 5.2.2.2 授权票据审批 (P0)

**实现要求**:
- 双人审批流程（第一审批人 + 第二审批人/复核人）
- 审批链记录（审批人、审批时间、审批意见、审批结果）
- 审批通过后，票据状态更新为 `APPROVED`
- 审批拒绝后，票据状态更新为 `REJECTED`

**技术规格**:

```java
@Service
@Transactional(rollbackFor = Exception.class)
public class AuthTicketApprovalService {
    /**
     * 第一审批人审批
     * 
     * @param ticketId 授权票据ID
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 审批意见
     * @param approved 是否批准
     */
    void firstApproval(String ticketId, String approverId, String approverName, 
                      String comment, boolean approved);
    
    /**
     * 第二审批人审批（复核）
     * 
     * @param ticketId 授权票据ID
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 审批意见
     * @param approved 是否批准
     */
    void secondApproval(String ticketId, String approverId, String approverName, 
                       String comment, boolean approved);
    
    /**
     * 获取审批链
     */
    ApprovalChain getApprovalChain(String ticketId);
}
```

**审批链 JSON 格式**:

```json
{
  "firstApprover": {
    "approverId": "user-001",
    "approverName": "张三",
    "approvalTime": "2025-01-15T10:30:00",
    "comment": "同意跨全宗查询",
    "approved": true
  },
  "secondApprover": {
    "approverId": "user-002",
    "approverName": "李四",
    "approvalTime": "2025-01-15T11:00:00",
    "comment": "复核通过",
    "approved": true
  },
  "finalStatus": "APPROVED",
  "approvedAt": "2025-01-15T11:00:00"
}
```

#### 5.2.2.3 授权票据验证与使用 (P0)

**实现要求**:
- 跨全宗访问时，必须验证授权票据
- 检查票据状态（必须是 `APPROVED`）
- 检查票据有效期（`expires_at >= 当前时间`）
- 检查访问范围（是否符合 `scope` 限制）
- 记录跨全宗访问审计日志（绑定 `auth_ticket_id`）

**技术规格**:

```java
@Service
public class AuthTicketValidationService {
    /**
     * 验证授权票据是否有效
     * 
     * @param ticketId 授权票据ID
     * @param targetFonds 目标全宗号
     * @param accessScope 本次访问范围
     * @return 验证结果
     * @throws AuthTicketException 票据无效时抛出异常
     */
    AuthTicketValidationResult validateTicket(String ticketId, String targetFonds, 
                                              AuthScope accessScope);
    
    /**
     * 检查访问范围是否在授权范围内
     */
    boolean isAccessScopeAllowed(AuthScope ticketScope, AuthScope accessScope);
}

public class AuthTicketValidationResult {
    private boolean valid;
    private String ticketId;
    private String applicantId;
    private String sourceFonds;
    private String targetFonds;
    private LocalDateTime expiresAt;
    private String reason;  // 如果无效，说明原因
}
```

**拦截器实现**:

```java
@Component
public class CrossFondsAccessInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                           Object handler) throws Exception {
        // 1. 检查是否为跨全宗访问（从请求中解析 target_fonds）
        String targetFonds = extractTargetFonds(request);
        String currentFonds = getCurrentFonds(request);
        
        if (targetFonds != null && !targetFonds.equals(currentFonds)) {
            // 2. 提取授权票据ID（从 Header 或请求参数）
            String ticketId = extractAuthTicketId(request);
            
            if (ticketId == null || ticketId.isEmpty()) {
                throw new AuthTicketRequiredException("跨全宗访问必须提供授权票据");
            }
            
            // 3. 验证授权票据
            AuthTicketValidationResult result = authTicketValidationService
                .validateTicket(ticketId, targetFonds, extractAccessScope(request));
            
            if (!result.isValid()) {
                throw new AuthTicketInvalidException(result.getReason());
            }
            
            // 4. 将票据信息存入请求上下文，供后续审计日志使用
            request.setAttribute("auth_ticket_id", ticketId);
            request.setAttribute("auth_ticket_info", result);
        }
        
        return true;
    }
}
```

#### 5.2.2.4 授权票据过期处理 (P0)

**实现要求**:
- 定时任务扫描过期票据（每日凌晨执行）
- 自动将过期票据状态更新为 `EXPIRED`
- 发送过期通知（可选）

**技术规格**:

```java
@Service
public class AuthTicketExpirationService {
    /**
     * 扫描并标记过期票据
     * 
     * @return 本次标记的过期票据数量
     */
    @Scheduled(cron = "0 0 1 * * ?")  // 每天凌晨1点
    @DistributedLock(key = "auth_ticket:expiration:scan", timeout = 3600)
    int scanAndMarkExpired();
}
```

#### 5.2.2.5 跨全宗访问审计日志 (P0)

**实现要求**:
- 所有跨全宗访问必须记录审计日志
- 审计日志必须包含：`auth_ticket_id`、`source_fonds`、`target_fonds`、`trace_id`
- 审计日志格式符合 PRD Section 6.3 要求

**技术规格**:

```java
// 在 AuditLogService 中扩展
public class AuditLogService {
    /**
     * 记录跨全宗访问审计日志
     * 
     * @param userId 用户ID
     * @param sourceFonds 源全宗号
     * @param targetFonds 目标全宗号
     * @param authTicketId 授权票据ID
     * @param action 操作类型
     * @param resourceId 资源ID
     * @param result 操作结果
     */
    void logCrossFondsAccess(String userId, String sourceFonds, String targetFonds,
                            String authTicketId, String action, String resourceId, 
                            String result);
}
```

**审计日志格式** (PRD Section 6.3):

```json
{
  "user_id": "user-001",
  "source_fonds": "JD",
  "target_fonds": "JD-SUB",
  "auth_ticket_id": "ticket-001",
  "trace_id": "trace-xxx",
  "action": "ARCHIVE_SEARCH",
  "resource_id": "arch-001",
  "result": "SUCCESS",
  "timestamp": "2025-01-15T14:30:00",
  "ip": "192.168.1.100",
  "user_agent": "Mozilla/5.0..."
}
```

### 5.2.3 API 设计

#### 5.2.3.1 授权票据申请接口

```
POST /api/auth-ticket/apply
请求体:
{
  "targetFonds": "JD-SUB",
  "scope": {
    "archiveYears": [2020, 2021],
    "docTypes": ["凭证", "报表"],
    "keywords": ["财务报表"],
    "accessType": "READ_ONLY"
  },
  "expiresAt": "2025-02-15T23:59:59",
  "reason": "审计需要查询子公司2020-2021年度财务报表"
}

响应:
{
  "code": 200,
  "data": {
    "ticketId": "ticket-001",
    "status": "PENDING",
    "createdAt": "2025-01-15T10:00:00"
  }
}
```

#### 5.2.3.2 授权票据审批接口

```
POST /api/auth-ticket/{ticketId}/first-approval
请求体:
{
  "approverId": "user-001",
  "comment": "同意跨全宗查询",
  "approved": true
}

POST /api/auth-ticket/{ticketId}/second-approval
请求体:
{
  "approverId": "user-002",
  "comment": "复核通过",
  "approved": true
}
```

#### 5.2.3.3 授权票据查询接口

```
GET /api/auth-ticket/{ticketId}
响应:
{
  "code": 200,
  "data": {
    "id": "ticket-001",
    "applicantId": "user-003",
    "applicantName": "王五",
    "sourceFonds": "JD",
    "targetFonds": "JD-SUB",
    "scope": {...},
    "expiresAt": "2025-02-15T23:59:59",
    "status": "APPROVED",
    "approvalChain": {...},
    "createdAt": "2025-01-15T10:00:00"
  }
}

GET /api/auth-ticket/list?applicantId=user-003&status=APPROVED
响应: 分页列表
```

#### 5.2.3.4 跨全宗访问接口（示例）

```
GET /api/archive/search?fondsNo=JD-SUB&keyword=财务报表
Header: X-Auth-Ticket-Id: ticket-001

响应: 正常搜索结果（包含审计日志记录）
```

### 5.2.4 验收标准

1. **功能测试**:
   - ✅ 创建授权票据申请，状态为 `PENDING`
   - ✅ 第一审批人审批，状态更新为 `FIRST_APPROVED` 或 `REJECTED`
   - ✅ 第二审批人审批，状态更新为 `APPROVED` 或 `REJECTED`
   - ✅ 授权票据过期后，状态自动更新为 `EXPIRED`
   - ✅ 撤销授权票据，状态更新为 `REVOKED`

2. **安全测试**:
   - ✅ 跨全宗访问未提供授权票据 → 返回 403 Forbidden
   - ✅ 跨全宗访问使用过期票据 → 返回 403 Forbidden
   - ✅ 跨全宗访问使用已撤销票据 → 返回 403 Forbidden
   - ✅ 跨全宗访问超出授权范围 → 返回 403 Forbidden

3. **审计测试**:
   - ✅ 所有跨全宗访问都记录审计日志
   - ✅ 审计日志包含 `auth_ticket_id`、`source_fonds`、`target_fonds`
   - ✅ 审计日志格式符合 PRD Section 6.3 要求

4. **性能测试**:
   - ✅ 授权票据验证响应时间 < 50ms
   - ✅ 支持并发 1000+ 次/秒的票据验证

---

## 📋 功能二：审计证据链验真接口 (P0)

### 5.3.1 PRD 来源与合规要求

**PRD 来源**: Section 6.2 - 审计日志防篡改要求

**合规要求**:
- **GB/T 39784-2021**: 审计日志必须防篡改，支持验真
- **DA/T 94-2022**: 审计证据链必须可验证、可追溯
- **安全要求**: 提供链路校验接口，支持抽检与证据包导出

**核心约束**:
- 哈希链必须完整（`prev_hash` → `curr_hash`）
- 验真接口必须支持批量验证
- 证据包导出必须包含完整审计日志和哈希链

### 5.3.2 功能范围

#### 5.3.2.1 审计日志哈希链验真 (P0)

**实现要求**:
- 验证单条审计日志的哈希值是否正确
- 验证审计日志哈希链的完整性（前后关联）
- 支持批量验证多条日志
- 返回验证结果和异常详情

**技术规格**:

```java
@Service
public class AuditLogVerificationService {
    /**
     * 验证单条审计日志的哈希值
     * 
     * @param logId 审计日志ID
     * @return 验证结果
     */
    VerificationResult verifySingleLog(String logId);
    
    /**
     * 验证审计日志哈希链的完整性
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param fondsNo 全宗号（可选）
     * @return 验证结果（包含所有异常日志）
     */
    ChainVerificationResult verifyChain(LocalDate startDate, LocalDate endDate, 
                                        String fondsNo);
    
    /**
     * 验证指定范围内的审计日志链
     * 
     * @param logIds 审计日志ID列表
     * @return 验证结果
     */
    ChainVerificationResult verifyChainByLogIds(List<String> logIds);
}

// 注意：AuditLogService 中已有 verifyLogChain 方法，需要扩展为独立的服务接口

public class VerificationResult {
    private boolean valid;
    private String logId;
    private String expectedHash;
    private String actualHash;
    private String reason;  // 如果无效，说明原因
    private LocalDateTime verifiedAt;
}

public class ChainVerificationResult {
    private boolean chainIntact;  // 哈希链是否完整
    private int totalLogs;  // 总日志数
    private int validLogs;  // 有效日志数
    private int invalidLogs;  // 无效日志数
    private List<VerificationResult> invalidResults;  // 无效日志详情
    private LocalDateTime verifiedAt;
}
```

**现有实现**:
- `AuditLogService.verifyLogChain()` 已实现基础哈希链验真
- 需要扩展为独立的 `AuditLogVerificationService` 接口
- 需要添加单条日志验真、证据包导出、抽检等功能

**验真逻辑**:

```java
public class AuditLogVerificationServiceImpl implements AuditLogVerificationService {
    
    // 复用 AuditLogService 中的验真逻辑
    private final AuditLogService auditLogService;
    
    @Override
    public VerificationResult verifySingleLog(String logId) {
        // 1. 查询审计日志
        SysAuditLog log = auditLogMapper.selectById(logId);
        if (log == null) {
            return VerificationResult.invalid("审计日志不存在: " + logId);
        }
        
        // 2. 重新计算当前日志的哈希值
        String recalculatedHash = calculateLogHash(log);
        
        // 3. 比较计算出的哈希值与存储的哈希值
        if (!recalculatedHash.equals(log.getLogHash())) {
            return VerificationResult.invalid("哈希值不匹配", 
                log.getLogHash(), recalculatedHash);
        }
        
        // 4. 验证与前一条日志的关联（如果存在）
        if (log.getPrevLogHash() != null) {
            String prevLogHash = getPreviousLogHash(log);
            if (!log.getPrevLogHash().equals(prevLogHash)) {
                return VerificationResult.invalid("与前一条日志的哈希关联不匹配");
            }
        }
        
        return VerificationResult.valid(logId, log.getLogHash());
    }
    
    @Override
    public ChainVerificationResult verifyChain(LocalDate startDate, LocalDate endDate, 
                                              String fondsNo) {
        // 复用 AuditLogService.verifyLogChain() 方法
        LogChainVerifyResult result = auditLogService.verifyLogChain(startDate, endDate);
        
        // 如果指定了全宗号，需要额外过滤（AuditLogService 当前不支持全宗过滤）
        // TODO: 扩展 AuditLogService.verifyLogChain() 支持全宗过滤
        
        return ChainVerificationResult.builder()
            .chainIntact(result.isValid())
            .totalLogs(result.getTotalLogs())
            .validLogs(result.getVerifiedLogs())
            .invalidLogs(result.getTotalLogs() - result.getVerifiedLogs())
            .invalidResults(parseInvalidResults(result))
            .verifiedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 计算审计日志的哈希值（与 AuditLogService 中的逻辑一致）
     */
    private String calculateLogHash(SysAuditLog log) {
        // 使用与 AuditLogService 相同的哈希计算逻辑
        String payload = buildHashPayload(log);
        return sm3Utils.hmac(auditLogHmacKey, payload);
    }
    
    private String buildHashPayload(SysAuditLog log) {
        StringBuilder sb = new StringBuilder();
        sb.append(log.getUserId()).append("|");
        sb.append(log.getAction()).append("|");
        sb.append(log.getObjectDigest() != null ? log.getObjectDigest() : "").append("|");
        sb.append(log.getCreatedTime().toString()).append("|");
        sb.append(log.getPrevLogHash() != null ? log.getPrevLogHash() : "");
        return sb.toString();
    }
}
```

#### 5.3.2.2 证据包导出 (P0)

**实现要求**:
- 导出指定日期范围内的审计日志
- 包含完整的哈希链信息
- 生成可验证的证据包（ZIP 格式）
- 包含验真报告（JSON 格式）

**技术规格**:

```java
@Service
public class AuditEvidencePackageService {
    /**
     * 导出审计证据包
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param fondsNo 全宗号（可选）
     * @param includeVerificationReport 是否包含验真报告
     * @return 证据包文件（ZIP格式）
     */
    byte[] exportEvidencePackage(LocalDate startDate, LocalDate endDate, 
                                String fondsNo, boolean includeVerificationReport);
    
    /**
     * 导出审计证据包（异步，返回任务ID）
     */
    String exportEvidencePackageAsync(LocalDate startDate, LocalDate endDate, 
                                     String fondsNo, boolean includeVerificationReport);
}
```

**证据包结构**:

```
evidence-package-2025-01-15.zip
├── audit-logs/
│   ├── audit-log-001.json
│   ├── audit-log-002.json
│   └── ...
├── verification-report.json
├── manifest.json
└── README.txt
```

**manifest.json 格式**:

```json
{
  "packageId": "evidence-2025-01-15-001",
  "exportDate": "2025-01-15T16:00:00",
  "exportBy": "audit-admin-001",
  "dateRange": {
    "startDate": "2025-01-01",
    "endDate": "2025-01-15"
  },
  "fondsNo": "JD",
  "totalLogs": 1000,
  "verificationStatus": "INTACT",
  "hashAlgorithm": "SM3",
  "files": [
    {
      "filename": "audit-log-001.json",
      "logId": "log-001",
      "hash": "abc123..."
    }
  ]
}
```

**verification-report.json 格式**:

```json
{
  "verificationDate": "2025-01-15T16:00:00",
  "verificationResult": {
    "chainIntact": true,
    "totalLogs": 1000,
    "validLogs": 1000,
    "invalidLogs": 0,
    "invalidResults": []
  },
  "verificationMethod": "FULL_CHAIN_VERIFICATION",
  "hashAlgorithm": "SM3"
}
```

#### 5.3.2.3 抽检验真接口 (P0)

**实现要求**:
- 支持随机抽检指定数量的审计日志
- 支持按条件抽检（按用户、操作类型、时间范围等）
- 返回抽检结果和验真报告

**技术规格**:

```java
@Service
public class AuditLogSamplingService {
    /**
     * 随机抽检审计日志
     * 
     * @param sampleSize 抽检数量
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 抽检结果
     */
    SamplingResult randomSample(int sampleSize, LocalDate startDate, LocalDate endDate);
    
    /**
     * 按条件抽检审计日志
     * 
     * @param criteria 抽检条件
     * @param sampleSize 抽检数量
     * @return 抽检结果
     */
    SamplingResult sampleByCriteria(SamplingCriteria criteria, int sampleSize);
}

public class SamplingCriteria {
    private String userId;  // 可选
    private String action;  // 可选
    private String resourceType;  // 可选
    private LocalDate startDate;
    private LocalDate endDate;
    private String fondsNo;  // 可选
}

public class SamplingResult {
    private int totalLogs;  // 符合条件的总日志数
    private int sampledLogs;  // 实际抽检的日志数
    private ChainVerificationResult verificationResult;  // 验真结果
    private List<String> sampledLogIds;  // 抽检的日志ID列表
}
```

### 5.3.3 API 设计

#### 5.3.3.1 单条日志验真接口

```
POST /api/audit-log/verify
请求体:
{
  "logId": "log-001"
}

响应:
{
  "code": 200,
  "data": {
    "valid": true,
    "logId": "log-001",
    "expectedHash": "abc123...",
    "actualHash": "abc123...",
    "verifiedAt": "2025-01-15T16:00:00"
  }
}
```

#### 5.3.3.2 哈希链验真接口

```
POST /api/audit-log/verify-chain
请求体:
{
  "startDate": "2025-01-01",
  "endDate": "2025-01-15",
  "fondsNo": "JD"  // 可选
}

响应:
{
  "code": 200,
  "data": {
    "chainIntact": true,
    "totalLogs": 1000,
    "validLogs": 1000,
    "invalidLogs": 0,
    "invalidResults": [],
    "verifiedAt": "2025-01-15T16:00:00"
  }
}
```

#### 5.3.3.3 证据包导出接口

```
POST /api/audit-log/export-evidence
请求体:
{
  "startDate": "2025-01-01",
  "endDate": "2025-01-15",
  "fondsNo": "JD",  // 可选
  "includeVerificationReport": true
}

响应:
{
  "code": 200,
  "data": {
    "taskId": "task-001",
    "status": "PROCESSING",
    "estimatedCompletionTime": "2025-01-15T16:05:00"
  }
}

GET /api/audit-log/export-evidence/{taskId}/download
响应: ZIP 文件流
```

#### 5.3.3.4 抽检验真接口

```
POST /api/audit-log/sample-verify
请求体:
{
  "sampleSize": 100,
  "startDate": "2025-01-01",
  "endDate": "2025-01-15",
  "criteria": {
    "userId": "user-001",  // 可选
    "action": "ARCHIVE_SEARCH"  // 可选
  }
}

响应:
{
  "code": 200,
  "data": {
    "totalLogs": 1000,
    "sampledLogs": 100,
    "verificationResult": {
      "chainIntact": true,
      "validLogs": 100,
      "invalidLogs": 0
    },
    "sampledLogIds": ["log-001", "log-002", ...]
  }
}
```

### 5.3.4 验收标准

1. **功能测试**:
   - ✅ 验证单条审计日志的哈希值，返回正确结果
   - ✅ 验证审计日志哈希链的完整性，检测出篡改的日志
   - ✅ 导出证据包，包含完整的审计日志和验真报告
   - ✅ 随机抽检审计日志，返回抽检结果和验真报告

2. **安全测试**:
   - ✅ 篡改审计日志后，验真接口能检测出异常
   - ✅ 删除审计日志后，哈希链验真能检测出断裂
   - ✅ 证据包导出需要 `AuditAdmin` 权限

3. **性能测试**:
   - ✅ 单条日志验真响应时间 < 10ms
   - ✅ 1000 条日志的哈希链验真响应时间 < 5s
   - ✅ 证据包导出（10000 条日志）完成时间 < 30s

4. **合规测试**:
   - ✅ 证据包格式符合审计要求
   - ✅ 验真报告包含完整的验证信息
   - ✅ 证据包可独立验证（不依赖系统）

---

## 📝 实施计划

### Sprint 5.1: 跨全宗访问授权票据（5-7 天）

**Day 1-2**: 数据库迁移 + 实体类 + Mapper
- 创建 `auth_ticket` 表迁移脚本
- 创建 `AuthTicket` 实体类
- 创建 `AuthTicketMapper` 接口

**Day 3-4**: 核心业务逻辑
- 实现 `AuthTicketService`（申请、查询、撤销）
- 实现 `AuthTicketApprovalService`（双人审批）
- 实现 `AuthTicketExpirationService`（过期处理）

**Day 5**: 验证与拦截器
- 实现 `AuthTicketValidationService`
- 实现 `CrossFondsAccessInterceptor`
- 集成到现有访问控制流程

**Day 6**: API 接口
- 实现授权票据申请/审批/查询接口
- 实现跨全宗访问接口（示例）

**Day 7**: 测试与文档
- 单元测试
- 集成测试
- API 文档

### Sprint 5.2: 审计证据链验真接口（3-5 天）

**Day 1**: 验真服务
- 实现 `AuditLogVerificationService`
- 实现单条日志验真
- 实现哈希链验真

**Day 2**: 证据包导出
- 实现 `AuditEvidencePackageService`
- 实现证据包生成（ZIP 格式）
- 实现验真报告生成

**Day 3**: 抽检服务
- 实现 `AuditLogSamplingService`
- 实现随机抽检
- 实现条件抽检

**Day 4**: API 接口
- 实现验真接口
- 实现证据包导出接口
- 实现抽检验真接口

**Day 5**: 测试与文档
- 单元测试
- 集成测试
- API 文档

---

## 🔖 变更记录

| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|----------|
| v1.0 | 2025-01 | AI Agent | 初始版本，包含两个 P0 功能的完整 OpenSpec |

---

## 📚 参考文档

- [PRD v1.0](prd-v1.0.md) - Section 2.4, Section 6.2
- [PRD 实现审查报告](../reports/prd-v1.0-implementation-review.md)
- [OpenSpec Sprint 4+](openspec-sprint4-plus.md)

