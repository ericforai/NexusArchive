# 电子会计档案系统 - SM2 签名增强 OpenSpec

> **版本**: v1.0  
> **日期**: 2025-01  
> **对齐基准**: [开发路线图 v1.0](../planning/development_roadmap_v1.0.md)、[PRD v1.0](prd-v1.0.md)  
> **优先级**: **P2（可选功能 - 中长期规划）**  
> **当前状态**: 待开发

---

## 📊 功能概览

| 功能模块 | 路线图章节 | PRD 章节 | 优先级 | 预计工作量 | 依赖关系 |
|---------|-----------|---------|--------|-----------|---------|
| SM2 签名增强 | 阶段二（增强） | Section 6.2 | **P2** | 2 周 | 审计日志服务 |

---

## 🎯 业务目标

**用户故事**: 作为审计管理员，我需要确保审计日志的不可抵赖性，系统应对关键审计日志进行 SM2 数字签名，防止日志被篡改，并能验证签名的有效性。

**业务价值**:
- 增强审计日志的不可抵赖性
- 提供更强的证据效力（符合电子签名法要求）
- 增强系统安全性和合规性
- 支持电子证据的法律效力

**合规要求**:
- **GB/T 39784-2021**: 审计日志必须防篡改、可追溯
- **电子签名法**: 使用数字签名增强法律效力
- **DA/T 94-2022**: 电子档案应保证真实性和完整性

**当前状态**:
- 审计日志已实现 SM3 哈希链（`prev_hash` / `curr_hash`）
- `Sm2SignatureService` 已存在，但未集成到审计日志
- P0/P1 采用哈希链，P2 可引入 SM2 签名增强

---

## 📋 功能范围

### 1. SM2 签名方案设计

#### 1.1 签名策略

**签名范围**:
- **关键操作日志**: 归档、销毁、权限变更、配置修改等
- **可选签名**: 普通查询、浏览等操作（按需配置）

**签名方式**:
- **批量签名**: 对一批日志记录进行批量签名（性能优化）
- **单条签名**: 对关键日志立即签名
- **定时签名**: 定时任务对未签名日志进行签名

#### 1.2 密钥管理

**签名密钥**:
- 使用系统级 SM2 密钥对
- 私钥存储在密钥库（KeyStore）中，加密保护
- 公钥可以公开，用于验证签名

**密钥轮换**:
- 支持密钥轮换（定期更新密钥对）
- 保留旧公钥用于验证历史签名
- 新签名使用新密钥

### 2. 签名服务扩展

#### 2.1 服务接口设计

```java
/**
 * 审计日志签名服务
 */
public interface AuditLogSignatureService {
    /**
     * 对审计日志进行 SM2 签名
     * 
     * @param auditLogId 审计日志ID
     * @return 签名结果（包含签名值、签名算法、证书信息等）
     */
    SignatureResult signAuditLog(String auditLogId);
    
    /**
     * 批量签名审计日志
     * 
     * @param auditLogIds 审计日志ID列表
     * @return 签名结果列表
     */
    List<SignatureResult> batchSignAuditLogs(List<String> auditLogIds);
    
    /**
     * 验证审计日志签名
     * 
     * @param auditLogId 审计日志ID
     * @return 验证结果
     */
    VerifyResult verifyAuditLogSignature(String auditLogId);
    
    /**
     * 验证审计日志链的签名完整性
     * 
     * @param startLogId 起始日志ID
     * @param endLogId 结束日志ID
     * @return 验证结果
     */
    ChainVerifyResult verifyAuditLogChain(String startLogId, String endLogId);
}
```

#### 2.2 签名数据结构

```java
public class SignatureResult {
    private String auditLogId;
    private String signatureValue;      // Base64 编码的签名值
    private String signatureAlgorithm;  // SM2withSM3
    private String certificateId;       // 证书ID（用于密钥管理）
    private LocalDateTime signTime;     // 签名时间
    private String signerId;            // 签名人ID（系统签名则为 SYSTEM）
}

public class VerifyResult {
    private boolean valid;              // 签名是否有效
    private String reason;              // 验证失败原因
    private String certificateInfo;     // 证书信息
    private LocalDateTime verifyTime;   // 验证时间
}
```

### 3. 数据库变更

#### 3.1 审计日志表扩展

**新增字段**:
```sql
-- 为 sys_audit_log 表添加签名相关字段
ALTER TABLE sys_audit_log 
ADD COLUMN IF NOT EXISTS signature_value TEXT COMMENT 'SM2 签名值（Base64 编码）',
ADD COLUMN IF NOT EXISTS signature_algorithm VARCHAR(20) COMMENT '签名算法: SM2withSM3',
ADD COLUMN IF NOT EXISTS certificate_id VARCHAR(64) COMMENT '签名证书ID',
ADD COLUMN IF NOT EXISTS signed_at TIMESTAMP COMMENT '签名时间',
ADD COLUMN IF NOT EXISTS signature_version INT DEFAULT 1 COMMENT '签名版本（用于密钥轮换）';

-- 索引
CREATE INDEX IF NOT EXISTS idx_audit_log_signed ON sys_audit_log(signed_at) WHERE signature_value IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_audit_log_certificate ON sys_audit_log(certificate_id);
```

#### 3.2 签名证书表（可选）

```sql
-- 签名证书表（用于密钥管理）
CREATE TABLE IF NOT EXISTS sys_signature_certificate (
    id VARCHAR(64) PRIMARY KEY,
    certificate_name VARCHAR(100) NOT NULL COMMENT '证书名称',
    public_key TEXT NOT NULL COMMENT '公钥（PEM 格式）',
    certificate_serial VARCHAR(64) COMMENT '证书序列号',
    valid_from TIMESTAMP NOT NULL COMMENT '有效期开始',
    valid_to TIMESTAMP NOT NULL COMMENT '有效期结束',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, REVOKED, EXPIRED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_cert_status (status, valid_to)
);

COMMENT ON TABLE sys_signature_certificate IS 'SM2 签名证书表（用于审计日志签名）';
```

### 4. 集成到审计日志服务

#### 4.1 审计日志服务扩展

```java
@Service
@RequiredArgsConstructor
public class AuditLogService {
    
    private final SysAuditLogMapper auditLogMapper;
    private final SM3Utils sm3Utils;
    private final AuditLogSignatureService signatureService; // 新增
    
    /**
     * 记录审计日志（带签名）
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void saveAuditLog(String action, String resourceType, String resourceId, 
                            String status, String details, String operatorId) {
        // 1. 创建审计日志记录（现有逻辑）
        SysAuditLog log = new SysAuditLog();
        // ... 设置字段
        
        // 2. 计算哈希链（现有逻辑）
        String prevHash = getLastAuditLogHash();
        String currHash = calculateHash(log, prevHash);
        log.setPrevHash(prevHash);
        log.setCurrHash(currHash);
        
        // 3. 保存日志
        auditLogMapper.insert(log);
        
        // 4. 对关键操作进行签名（新增）
        if (shouldSign(action, resourceType)) {
            try {
                signatureService.signAuditLog(log.getId());
            } catch (Exception e) {
                log.error("审计日志签名失败: logId={}, error={}", log.getId(), e.getMessage());
                // 签名失败不影响日志记录，但需要告警
            }
        }
    }
    
    /**
     * 判断是否需要签名
     */
    private boolean shouldSign(String action, String resourceType) {
        // 关键操作需要签名
        Set<String> criticalActions = Set.of(
            "ARCHIVE", "DESTRUCTION", "PERMISSION_CHANGE", 
            "CONFIG_CHANGE", "DATA_EXPORT"
        );
        return criticalActions.contains(action);
    }
}
```

#### 4.2 签名实现

```java
@Service
@RequiredArgsConstructor
public class AuditLogSignatureServiceImpl implements AuditLogSignatureService {
    
    private final SysAuditLogMapper auditLogMapper;
    private final Sm2SignatureService sm2SignatureService;
    private final SignatureCertificateService certificateService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SignatureResult signAuditLog(String auditLogId) {
        SysAuditLog log = auditLogMapper.selectById(auditLogId);
        if (log == null) {
            throw new BusinessException("审计日志不存在");
        }
        
        // 1. 构建待签名数据（包含日志关键字段）
        byte[] dataToSign = buildSignData(log);
        
        // 2. 使用 SM2 签名
        com.nexusarchive.dto.signature.SignResult signResult = 
            sm2SignatureService.sign(dataToSign);
        
        // 3. 获取当前有效证书
        SignatureCertificate cert = certificateService.getActiveCertificate();
        
        // 4. 更新审计日志的签名字段
        log.setSignatureValue(signResult.getSignature());
        log.setSignatureAlgorithm("SM2withSM3");
        log.setCertificateId(cert.getId());
        log.setSignedAt(LocalDateTime.now());
        log.setSignatureVersion(cert.getVersion());
        auditLogMapper.updateById(log);
        
        // 5. 构建返回结果
        return SignatureResult.builder()
            .auditLogId(auditLogId)
            .signatureValue(signResult.getSignature())
            .signatureAlgorithm("SM2withSM3")
            .certificateId(cert.getId())
            .signTime(LocalDateTime.now())
            .signerId("SYSTEM")
            .build();
    }
    
    /**
     * 构建待签名数据
     */
    private byte[] buildSignData(SysAuditLog log) {
        // 包含关键字段：ID、操作类型、资源ID、状态、哈希值、时间戳
        String data = String.format("%s|%s|%s|%s|%s|%s|%s",
            log.getId(),
            log.getAction(),
            log.getResourceType(),
            log.getResourceId(),
            log.getStatus(),
            log.getCurrHash(),
            log.getCreatedTime().toString()
        );
        return data.getBytes(StandardCharsets.UTF_8);
    }
    
    @Override
    public VerifyResult verifyAuditLogSignature(String auditLogId) {
        SysAuditLog log = auditLogMapper.selectById(auditLogId);
        if (log == null) {
            return VerifyResult.failure("审计日志不存在");
        }
        
        if (log.getSignatureValue() == null) {
            return VerifyResult.failure("审计日志未签名");
        }
        
        // 1. 构建原始数据
        byte[] dataToVerify = buildSignData(log);
        
        // 2. 获取证书公钥
        SignatureCertificate cert = certificateService.getCertificate(log.getCertificateId());
        if (cert == null || cert.getStatus() != "ACTIVE") {
            return VerifyResult.failure("签名证书无效或已撤销");
        }
        
        // 3. 验证签名
        com.nexusarchive.dto.signature.VerifyResult verifyResult = 
            sm2SignatureService.verify(dataToVerify, log.getSignatureValue(), cert.getPublicKey());
        
        return VerifyResult.builder()
            .valid(verifyResult.isValid())
            .reason(verifyResult.isValid() ? null : verifyResult.getReason())
            .certificateInfo(cert.getCertificateName())
            .verifyTime(LocalDateTime.now())
            .build();
    }
}
```

### 5. 定时签名任务

#### 5.1 批量签名定时任务

**实现要求**:
- 定时任务（每小时执行）
- 扫描未签名的关键操作日志
- 批量签名（提升性能）
- 记录签名结果

```java
@Service
public class AuditLogBatchSignatureService {
    
    @Scheduled(cron = "0 0 * * * ?") // 每小时执行
    @DistributedLock(key = "audit:log:batch:sign", timeout = 3600)
    public void batchSignUnSignedLogs() {
        // 1. 查询未签名的关键操作日志（最近 24 小时）
        List<SysAuditLog> unsignedLogs = findUnsignedCriticalLogs(24);
        
        // 2. 批量签名
        for (List<SysAuditLog> batch : Lists.partition(unsignedLogs, 100)) {
            List<String> logIds = batch.stream()
                .map(SysAuditLog::getId)
                .collect(Collectors.toList());
            signatureService.batchSignAuditLogs(logIds);
        }
        
        log.info("批量签名完成: 共 {} 条日志", unsignedLogs.size());
    }
}
```

---

## 🧪 测试要求

### 6.1 单元测试

**测试用例**:
- 签名生成和验证
- 批量签名
- 签名验证失败场景
- 证书失效场景

### 6.2 集成测试

**测试场景**:
- 审计日志记录和签名流程
- 签名验证流程
- 日志链签名验证
- 密钥轮换场景

---

## 📝 开发检查清单

- [ ] 设计 SM2 签名方案（签名范围、策略）
- [ ] 扩展 `Sm2SignatureService`（如需要）
- [ ] 创建 `AuditLogSignatureService` 接口和实现
- [ ] 创建签名证书表（数据库迁移脚本）
- [ ] 扩展 `sys_audit_log` 表添加签名字段
- [ ] 集成签名服务到 `AuditLogService`
- [ ] 实现批量签名定时任务
- [ ] 实现签名验证功能
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 更新相关文档

---

## 🔗 相关文档

- 开发路线图：`docs/planning/development_roadmap_v1.0.md`
- PRD v1.0：`docs/product/prd-v1.0.md` (Section 6.2)
- 缺口分析报告：`docs/reports/roadmap-gap-analysis-2025-01.md`
- 审计日志服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/AuditLogService.java`
- SM2 签名服务：`nexusarchive-java/src/main/java/com/nexusarchive/service/Sm2SignatureService.java`

---

**文档状态**: ✅ 已完成  
**下一步**: 设计详细签名方案，开始开发实现



