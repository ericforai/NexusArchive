# Sprint 1 技术规格说明书 (Development Spec)

> **版本**: v1.0
> **周期**: week 3-4 (预计 2 周)
> **目标**: 合规攻关 (Compliance Gate) - 解决技术上最难啃的骨头
> **对齐**: [PRD v1.0 模块三](file:///Users/user/nexusarchive/docs/product/prd-v1.0.md) + [Roadmap 阶段二](file:///Users/user/nexusarchive/docs/planning/development_roadmap_v1.0.md)

---

## 🎯 Sprint 目标

| 目标 | 验收标准 |
| --- | --- |
| 四性检测引擎完善 | 坏文件样本库 100% 拦截 |
| 审计哈希链 | 链式校验工具可检测篡改 |
| 服务端水印 | 预览 PDF 带用户+时间戳水印 |

---

## 📦 交付物清单

### Week 1: 四性检测引擎完善

| 交付物 | 优先级 | 负责人 |
| --- | --- | --- |
| `IntegrityChecker.java` | P0 | - |
| `DigitalSignatureVerifier.java` | P0 | - |
| `VirusScanService.java` | P0 | - |
| 坏文件样本库 (`test-samples/`) | P0 | - |

### Week 2: 审计与水印

| 交付物 | 优先级 | 负责人 |
| --- | --- | --- |
| `AuditLogService.java` | P0 | - |
| `AuditChainVerifier.java` | P1 | - |
| `WatermarkService.java` | P0 | - |

---

## 🏗 技术规格

### 1. 完整性检测 (IntegrityChecker)

**PRD 来源**: PRD 3.1 - 必须校验 XML 元数据与版式文件(OFD/PDF)内容的一致性

#### 1.1 功能需求
- 解析电子发票 XML 提取关键字段: 发票号、金额、日期、销方/购方
- 解析 OFD/PDF 版式文件提取相同字段
- 比对一致性，记录差异

#### 1.2 接口设计

```java
package com.nexusarchive.core.compliance;

public interface IntegrityChecker {
    /**
     * 校验 XML 元数据与版式文件一致性
     * @param xmlPath XML 文件路径
     * @param formatPath OFD/PDF 文件路径
     * @return 校验结果
     */
    IntegrityCheckResult verify(Path xmlPath, Path formatPath);
}

public record IntegrityCheckResult(
    boolean passed,
    List<IntegrityDiff> diffs
) {}

public record IntegrityDiff(
    String fieldName,      // 如 "amount", "invoice_no"
    String xmlValue,       // XML 中的值
    String formatValue,    // OFD/PDF 中的值
    String message
) {}
```

#### 1.3 实现要点
- XML 解析: 标准 JAXB 或 DOM 解析
- OFD 解析: 需引入 OFD 解析库 (如 `ofdrw-reader`)
- PDF 解析: PDFBox 文本提取 + 正则匹配
- 字段匹配容差: 金额允许 0.01 误差，日期格式统一后比对

#### 1.4 依赖

```xml
<dependency>
    <groupId>org.ofdrw</groupId>
    <artifactId>ofdrw-reader</artifactId>
    <version>2.2.6</version>
</dependency>
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.1</version>
</dependency>
```

---

### 2. 数字签名校验 (DigitalSignatureVerifier)

**PRD 来源**: PRD 3.1 - 文件哈希 + 数字签名校验（支持 SM2/SM3）

#### 2.1 功能需求
- 验证 OFD/PDF 文件的数字签名完整性
- 支持国密 SM2 签名算法
- 验证签名证书链有效性

#### 2.2 接口设计

```java
public interface DigitalSignatureVerifier {
    /**
     * 验证文件数字签名
     * @param filePath 签名文件路径
     * @return 签名验证结果
     */
    SignatureVerifyResult verify(Path filePath);
}

public record SignatureVerifyResult(
    boolean valid,
    String algorithm,          // SM2, RSA, etc.
    String signerName,
    LocalDateTime signTime,
    String certSerialNo,
    String errorMessage
) {}
```

#### 2.3 实现要点
- OFD 签名: `ofdrw-sign` 模块
- PDF 签名: PDFBox + BouncyCastle
- SM2 支持: BouncyCastle `bcprov-jdk18on` (已集成)

#### 2.4 依赖

```xml
<dependency>
    <groupId>org.ofdrw</groupId>
    <artifactId>ofdrw-sign</artifactId>
    <version>2.2.6</version>
</dependency>
```

---

### 3. 病毒扫描 (VirusScanService)

**PRD 来源**: PRD 3.1 - 文件入库/预览前病毒扫描

#### 3.1 功能需求
- 接入 ClamAV 开源杀毒引擎
- 支持流式扫描 (避免大文件 OOM)
- 记录扫描结果到数据库

#### 3.2 接口设计

```java
public interface VirusScanService {
    /**
     * 扫描文件是否包含病毒
     * @param filePath 文件路径
     * @return 扫描结果
     */
    VirusScanResult scan(Path filePath);
    
    /**
     * 扫描输入流
     */
    VirusScanResult scan(InputStream inputStream, String fileName);
}

public record VirusScanResult(
    boolean clean,
    String virusName,      // 若检测到病毒
    String scanEngine,     // "ClamAV"
    long scanDurationMs
) {}
```

#### 3.3 实现要点
- ClamAV 部署: Docker 容器 `clamav/clamav:latest`
- 客户端: `clamav-java` 库通过 TCP 连接 clamd
- 超时设置: 大文件扫描需设置合理超时 (如 60s)

#### 3.4 依赖

```xml
<dependency>
    <groupId>fi.solita.clamav</groupId>
    <artifactId>clamav-client</artifactId>
    <version>1.0.1</version>
</dependency>
```

#### 3.5 Docker 配置

```yaml
# docker-compose.dev.yml 新增
clamav:
  image: clamav/clamav:latest
  container_name: nexus-clamav
  ports:
    - "3310:3310"
  volumes:
    - clamav-data:/var/lib/clamav
```

---

### 4. 审计哈希链 (AuditLogService)

**Roadmap 来源**: 阶段二 - 实现 `curr_hash = SM3(prev_hash + data)`

#### 4.1 功能需求
- 每条审计日志计算当前哈希并关联上一条哈希
- 支持链式验证，检测中间篡改
- 使用 SM3 算法 (已集成)

#### 4.2 数据模型

```sql
-- 审计日志表 (已存在，需补充字段)
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS prev_hash VARCHAR(64);
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS curr_hash VARCHAR(64);
ALTER TABLE sys_audit_log ADD COLUMN IF NOT EXISTS chain_seq BIGINT;
```

#### 4.3 接口设计

```java
@Service
public class AuditLogService {
    private final Sm3HashService hashService;
    
    /**
     * 写入审计日志 (自动计算哈希链)
     */
    public void log(AuditLogEntry entry) {
        String prevHash = getLatestHash();
        String dataPayload = serializeEntry(entry);
        String currHash = hashService.hashSm3(prevHash + dataPayload);
        entry.setPrevHash(prevHash);
        entry.setCurrHash(currHash);
        entry.setChainSeq(getNextSeq());
        auditLogMapper.insert(entry);
    }
    
    /**
     * 验证审计链完整性
     */
    public ChainVerifyResult verifyChain(LocalDateTime from, LocalDateTime to);
}

public record ChainVerifyResult(
    boolean valid,
    long totalRecords,
    long verifiedRecords,
    List<ChainBreak> breaks
) {}

public record ChainBreak(
    long seq,
    String expectedHash,
    String actualHash
) {}
```

---

### 5. 服务端水印 (WatermarkService)

**PRD 来源**: PRD 2.2 - 服务端流式加水印
**Roadmap 来源**: 阶段二 - 集成 PDFBox/iText

#### 5.1 功能需求
- PDF 预览时实时添加水印
- 水印内容: 用户名 + 时间戳 + TraceID
- 支持流式处理 (不完整加载整个文件)

#### 5.2 接口设计

```java
public interface WatermarkService {
    /**
     * 为 PDF 添加水印
     * @param source 原始 PDF 输入流
     * @param watermarkText 水印文本
     * @return 带水印的 PDF 输出流
     */
    InputStream addWatermark(InputStream source, WatermarkConfig config);
}

public record WatermarkConfig(
    String primaryText,    // 用户名
    String secondaryText,  // TraceID + FondsNo
    float opacity,         // 0.1 - 0.3
    float rotation,        // 45 度
    String fontName,       // "SimSun" 或 "STSong"
    float fontSize         // 48
) {}
```

#### 5.3 实现要点
- 使用 PDFBox 3.x 的 `PDPageContentStream`
- 水印绘制在每页中心，45 度倾斜
- 中文字体需嵌入 (避免乱码)
- 流式处理: 按页加载/输出

#### 5.4 依赖

```xml
<!-- PDFBox 已在 IntegrityChecker 引入 -->
```

---

## 🧪 测试策略

### 坏文件样本库 (test-samples/)

| 样本类型 | 文件名 | 预期行为 |
| --- | --- | --- |
| 改后缀 exe | `fake.pdf` | Magic Number 检测失败 |
| 签名失效 | `invalid-sign.ofd` | 签名校验失败 |
| 金额不一致 | `amount-mismatch.zip` (xml+pdf) | 完整性检测失败 |
| 病毒样本 | `eicar.txt` | 病毒扫描报警 |
| 正常文件 | `valid-invoice.ofd` | 全部通过 |

### 审计链测试

| 场景 | 预期 |
| --- | --- |
| 正常写入 10 条日志 | 链式校验通过 |
| 手动修改中间记录 | 链式校验报警 |
| 删除中间记录 | 链式校验报警 |

---

## 📅 里程碑

### Week 1 (Day 1-5)
- [ ] Day 1: `IntegrityChecker` 接口 + XML 解析
- [ ] Day 2: OFD 解析 + 字段比对
- [ ] Day 3: `DigitalSignatureVerifier` SM2 验签
- [ ] Day 4: `VirusScanService` + ClamAV Docker
- [ ] Day 5: 坏文件样本库 + 集成测试

### Week 2 (Day 6-10)
- [ ] Day 6: `AuditLogService` 哈希链实现
- [ ] Day 7: `AuditChainVerifier` 链式校验
- [ ] Day 8: `WatermarkService` PDF 水印
- [ ] Day 9: 集成测试 + 性能测试
- [ ] Day 10: 文档 + 代码审查

---

## ⚠️ 风险与缓解

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| OFD 解析库不稳定 | 完整性检测失败 | 提前验证 `ofdrw-reader` 兼容性 |
| ClamAV 病毒库更新慢 | 漏检新病毒 | 定期更新 + 备选商业方案 |
| 大文件水印性能 | 预览卡顿 | 流式处理 + 缓存机制 |
| SM2 签名格式多样 | 验签失败 | 收集真实样本覆盖测试 |

---

## 📚 参考资料

- [OFD 读写库 ofdrw](https://github.com/Trisia/ofdrw)
- [BouncyCastle SM2/SM3](https://www.bouncycastle.org/)
- [ClamAV 官方文档](https://docs.clamav.net/)
- [PDFBox 3.x 文档](https://pdfbox.apache.org/)
