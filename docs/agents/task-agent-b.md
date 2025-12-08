# Agent B: 合规开发工程师任务书

> **角色**: 合规开发工程师
> **技术栈**: Java 17, Spring Boot 3.x, 国密算法 (SM2/SM3), BouncyCastle
> **负责阶段**: 第二阶段 - 合规补齐（核心部分）
> **前置依赖**: ⚠️ 需等待 Agent A 完成第一阶段基础安全加固

---

## 📋 项目背景

NexusArchive 是一个电子会计档案管理系统，必须严格遵守以下法规：

- **DA/T 94-2022**: 电子会计档案元数据规范
- **DA/T 92-2022**: 电子档案"四性检测"规范
- **《会计档案管理办法》(财政部79号令)**: 核心合规红线

### 关键约束
- **合规性 > 性能**：法规要求必须100%满足
- **信创适配**：必须使用国密算法（SM2签名、SM3摘要、SM4加密）
- **审计留痕**：所有关键操作必须记录，日志不可篡改

---

## 🔐 必读规则

执行任务前，请阅读以下规则文件：

1. **[.agent/rules/general.md](file:///Users/user/nexusarchive/.agent/rules/general.md)** - 核心编码规范（尤其是审计日志部分）
2. **[.agent/rules/expert-group.md](file:///Users/user/nexusarchive/.agent/rules/expert-group.md)** - 专家审查机制

---

## ✅ 任务清单

### 2.1 电子签章集成

| 序号 | 任务 | 产出文件 | 说明 | 验收标准 |
|------|------|----------|------|----------|
| 2.1.1 | 签章服务接口定义 | `SignatureAdapter.java` | 抽象签章适配器接口 | 接口设计合理可扩展 |
| 2.1.2 | SM2签名实现 | `Sm2SignatureService.java` | 基于BouncyCastle实现SM2签名 | 签名可验证 |
| 2.1.3 | 签章验证服务 | `SignatureVerifyService.java` | 验证PDF/OFD签章 | 能验证有效/无效签章 |
| 2.1.4 | 签章结果存储 | `arc_signature_log` 表 | 记录签章信息到数据库 | 数据正确持久化 |
| 2.1.5 | 前端签章展示 | 组件修改 | 显示签章状态标识 | 前端显示签章图标 |

**数据库表设计：**
```sql
-- V15__add_signature_log.sql
CREATE TABLE arc_signature_log (
    id VARCHAR(32) PRIMARY KEY,
    archive_id VARCHAR(32) NOT NULL,
    file_id VARCHAR(32),
    signer_name VARCHAR(100),
    signer_cert_sn VARCHAR(100),  -- 证书序列号
    signer_org VARCHAR(200),      -- 签章单位
    sign_time TIMESTAMP,
    sign_algorithm VARCHAR(20) DEFAULT 'SM2',
    signature_value TEXT,
    verify_result VARCHAR(20),    -- VALID, INVALID, UNKNOWN
    verify_time TIMESTAMP,
    verify_message TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_signature_archive FOREIGN KEY (archive_id) 
        REFERENCES arc_archive(id)
);

CREATE INDEX idx_signature_archive ON arc_signature_log(archive_id);
```

**代码示例：**
```java
// SignatureAdapter.java
public interface SignatureAdapter {
    
    SignResult sign(byte[] data, String certAlias);
    
    VerifyResult verify(byte[] data, byte[] signature, String certAlias);
    
    VerifyResult verifyPdfSignature(InputStream pdfStream);
    
    VerifyResult verifyOfdSignature(InputStream ofdStream);
}

// SignResult.java
@Data
public class SignResult {
    private boolean success;
    private byte[] signature;
    private String algorithm;
    private String signerName;
    private String certSerialNumber;
    private LocalDateTime signTime;
    private String errorMessage;
}

// Sm2SignatureService.java
@Service
@RequiredArgsConstructor
public class Sm2SignatureService implements SignatureAdapter {
    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    @Override
    public SignResult sign(byte[] data, String certAlias) {
        try {
            // 1. 获取私钥
            PrivateKey privateKey = loadPrivateKey(certAlias);
            
            // 2. SM2签名
            Signature signature = Signature.getInstance("SM3withSM2", "BC");
            signature.initSign(privateKey);
            signature.update(data);
            byte[] sig = signature.sign();
            
            return SignResult.builder()
                .success(true)
                .signature(sig)
                .algorithm("SM2")
                .signTime(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            return SignResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }
    
    @Override
    public VerifyResult verify(byte[] data, byte[] sig, String certAlias) {
        try {
            PublicKey publicKey = loadPublicKey(certAlias);
            Signature signature = Signature.getInstance("SM3withSM2", "BC");
            signature.initVerify(publicKey);
            signature.update(data);
            boolean valid = signature.verify(sig);
            
            return VerifyResult.builder()
                .valid(valid)
                .verifyTime(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            return VerifyResult.builder()
                .valid(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }
}
```

---

### 2.2 审计日志完善

| 序号 | 任务 | 文件 | 操作 | 验收标准 |
|------|------|------|------|----------|
| 2.2.1 | MAC地址获取优化 | `AuditLogService.java` | 从请求头 X-Client-Mac 获取 | 日志包含 MAC |
| 2.2.2 | 操作前数据快照 | `ArchivalAuditAspect.java` | 修改前查询并保存 JSON | before_value 有值 |
| 2.2.3 | 操作后数据快照 | `ArchivalAuditAspect.java` | 修改后记录 JSON | after_value 有值 |
| 2.2.4 | 审计日志防篡改 | 增加哈希链 | 每条日志含前一条哈希 | 日志链条可验证 |
| 2.2.5 | 审计日志导出 | `AuditLogController.java` | 支持导出Excel/PDF | 导出文件可用 |

**数据库迁移：**
```sql
-- V16__enhance_audit_log.sql
ALTER TABLE sys_audit_log ADD COLUMN prev_log_hash VARCHAR(64);
ALTER TABLE sys_audit_log ADD COLUMN log_hash VARCHAR(64);
ALTER TABLE sys_audit_log ADD COLUMN device_fingerprint VARCHAR(200);

CREATE INDEX idx_audit_log_hash ON sys_audit_log(log_hash);
```

**代码示例：**
```java
// 增强的 AuditLogService.java
@Service
@RequiredArgsConstructor
public class AuditLogService {
    
    private final AuditLogMapper auditLogMapper;
    private final SM3Utils sm3Utils;
    
    @Transactional
    public void saveAuditLog(AuditLog log) {
        // 获取前一条日志的哈希
        String prevHash = auditLogMapper.getLatestLogHash();
        log.setPrevLogHash(prevHash);
        
        // 计算当前日志的哈希
        String content = log.getOperatorId() + log.getOperationType() 
            + log.getObjectDigest() + log.getCreatedTime() + prevHash;
        log.setLogHash(sm3Utils.hash(content));
        
        auditLogMapper.insert(log);
    }
    
    public boolean verifyLogChain(LocalDate startDate, LocalDate endDate) {
        List<AuditLog> logs = auditLogMapper.findByDateRange(startDate, endDate);
        
        for (int i = 1; i < logs.size(); i++) {
            AuditLog current = logs.get(i);
            AuditLog prev = logs.get(i - 1);
            
            if (!prev.getLogHash().equals(current.getPrevLogHash())) {
                return false; // 日志链被篡改
            }
        }
        return true;
    }
}
```

---

### 2.3 四性检测增强

| 序号 | 任务 | 当前状态 | 需要增强 | 验收标准 |
|------|------|----------|----------|----------|
| 2.3.1 | 真实性检测 | 基础实现 | 增加签章验证集成 | 签章文件验证通过 |
| 2.3.2 | 完整性检测 | SM3哈希 | 增加元数据完整性检查 | 检测缺失元数据 |
| 2.3.3 | 可用性检测 | 格式检查 | 增加文件可读性验证 | 检测损坏文件 |
| 2.3.4 | 安全性检测 | Mock实现 | 集成真实杀毒引擎接口 | 病毒扫描可用 |
| 2.3.5 | 检测报告生成 | 基础实现 | 符合DA/T 92格式 | 报告格式合规 |

**代码示例：**
```java
// 增强的真实性检测
@Component
public class AuthenticityChecker {
    
    private final SignatureAdapter signatureAdapter;
    private final SM3Utils sm3Utils;
    
    public CheckResult check(Archive archive, FileContent file) {
        List<String> issues = new ArrayList<>();
        
        // 1. 哈希校验
        String computed = sm3Utils.hash(file.getContent());
        if (!computed.equals(file.getFixityValue())) {
            issues.add("文件哈希值不匹配，可能被篡改");
        }
        
        // 2. 签章验证（新增）
        if (file.hasSealedSignature()) {
            VerifyResult result = signatureAdapter.verifyPdfSignature(
                new ByteArrayInputStream(file.getContent()));
            if (!result.isValid()) {
                issues.add("电子签章验证失败: " + result.getErrorMessage());
            }
        }
        
        return CheckResult.builder()
            .passed(issues.isEmpty())
            .issues(issues)
            .checkTime(LocalDateTime.now())
            .build();
    }
}
```

---

### 2.4 敏感字段加密扩展

| 序号 | 任务 | 说明 | 验收标准 |
|------|------|------|----------|
| 2.4.1 | 扩展加密字段 | `summary`, `creator` 等使用 SM4 加密 | 数据库存密文 |
| 2.4.2 | 密钥管理 | 从环境变量读取密钥 | 无硬编码密钥 |
| 2.4.3 | 数据迁移脚本 | 旧数据批量加密 | 迁移脚本可执行 |

**代码示例：**
```java
// Archive.java 扩展加密字段
@TableField(typeHandler = EncryptTypeHandler.class)
private String title;

@TableField(typeHandler = EncryptTypeHandler.class)
private String summary;

@TableField(typeHandler = EncryptTypeHandler.class)
private String creatorName;
```

---

## 🧪 验证步骤

### 1. 编译验证
```bash
cd nexusarchive-java
mvn clean compile -DskipTests
```

### 2. 签章验证测试
```bash
# 调用签章接口
curl -X POST http://localhost:8080/api/signature/sign \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.pdf" \
  -F "certAlias=default"
```

### 3. 审计日志链验证
```bash
curl http://localhost:8080/api/admin/audit/verify-chain?startDate=2025-01-01
```

---

## 📝 完成标志

任务完成后，请在 `docs/优化计划.md` 中勾选第二阶段 2.1-2.4 的相关项目。

---

*Agent B 任务书 - 由 Claude 于 2025-12-07 生成*
