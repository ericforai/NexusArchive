# Sprint 2 (Revised) 技术规格说明书

> **版本**: v1.0
> **周期**: week 5-6 (预计 2 周)
> **目标**: 检索与利用增强 (Search & Retrieval Enhancement)
> **对齐**: [PRD v1.0 模块二](file:///Users/user/nexusarchive/docs/product/prd-v1.0.md#模块二检索与利用-p0p1) + 专家组审查建议

---

## 🎯 Sprint 目标

| 目标 | 验收标准 |
| --- | --- |
| 高级检索 | 按金额/日期/对方/凭证号组合查询 |
| 数据脱敏 | 敏感字段按规则遮蔽 |
| 流式预览 | 500页大文件秒开 |
| 动态水印 | 用户名+时间戳全屏覆盖 |

---

## 📦 交付物清单

### Week 1: 高级检索与脱敏

| 交付物 | 优先级 | PRD 来源 |
| --- | --- | --- |
| 结构化索引迁移 | P0 | PRD 3.2 |
| `ArchiveSearchService` | P0 | PRD 2.1 |
| `DataMaskingService` | P1 | PRD 2.1 |
| 脱敏规则配置 | P1 | PRD 2.1 |

### Week 2: 流式预览与水印

| 交付物 | 优先级 | PRD 来源 |
| --- | --- | --- |
| `StreamingPreviewService` | P0 | PRD 2.2 |
| `DynamicWatermarkService` 增强 | P0 | PRD 2.2 |
| 前端 PDF 阅读器集成 | P1 | PRD 2.2 |
| 高敏模式服务端渲染 | P2 | PRD 2.2 |

---

## 🏗 技术规格

### 1. 高级检索 (ArchiveSearchService)

**PRD 来源**: PRD 2.1 - 高级检索与脱敏

#### 1.1 结构化索引

```sql
-- Flyway: V70__add_search_indexes.sql
CREATE INDEX IF NOT EXISTS idx_archive_amount 
    ON archive_object(fonds_no, archive_year, amount);
CREATE INDEX IF NOT EXISTS idx_archive_doc_date 
    ON archive_object(fonds_no, archive_year, doc_date);
CREATE INDEX IF NOT EXISTS idx_archive_counterparty 
    ON archive_object(fonds_no, archive_year, counterparty);
CREATE INDEX IF NOT EXISTS idx_archive_voucher_no 
    ON archive_object(fonds_no, archive_year, voucher_no);
CREATE INDEX IF NOT EXISTS idx_archive_invoice_no 
    ON archive_object(fonds_no, archive_year, invoice_no);
```

#### 1.2 查询接口

```java
public interface ArchiveSearchService {
    /** 高级检索 */
    Page<ArchiveObject> search(ArchiveSearchRequest request, Pageable pageable);
}

public record ArchiveSearchRequest(
    String fondsNo,
    Integer archiveYear,
    BigDecimal amountFrom,
    BigDecimal amountTo,
    LocalDate dateFrom,
    LocalDate dateTo,
    String counterparty,
    String voucherNo,
    String invoiceNo,
    String keyword
) {}
```

---

### 2. 数据脱敏 (DataMaskingService)

**PRD 来源**: PRD 2.1 - 脱敏规则

#### 2.1 脱敏规则配置

```yaml
nexus:
  masking:
    rules:
      - field: bank_account
        pattern: MIDDLE_8  # 中间8位替换
        mask: "********"
      - field: id_card
        pattern: KEEP_3_4  # 保留前3后4
        mask: "***********"
      - field: phone
        pattern: MIDDLE_4  # 中间4位
        mask: "****"
```

#### 2.2 接口设计

```java
public interface DataMaskingService {
    /** 脱敏单个字段 */
    String mask(String fieldName, String value);
    
    /** 脱敏整个对象 (使用反射) */
    <T> T maskObject(T object, UserRole role);
    
    /** 检查是否需要脱敏 */
    boolean requireMasking(String fieldName, UserRole role);
}

public enum MaskPattern {
    MIDDLE_4,      // 中间4位
    MIDDLE_8,      // 中间8位
    KEEP_3_4,      // 保留前3后4
    KEEP_FIRST_LAST, // 保留首尾
    FULL           // 全部遮蔽
}
```

---

### 3. 流式预览 (StreamingPreviewService)

**PRD 来源**: PRD 2.2 - 流式预览与动态水印

#### 3.1 分页流式读取

```java
public interface StreamingPreviewService {
    /** 获取页数 */
    int getPageCount(String fileId);
    
    /** 获取单页 (带水印) */
    byte[] getPage(String fileId, int pageNumber, WatermarkConfig watermark);
    
    /** 获取页面范围 (Range 请求) */
    InputStream getPageRange(String fileId, int fromPage, int toPage);
}
```

#### 3.2 实现要点

- 使用 PDFBox `PDFRenderer` 逐页渲染
- 服务端缓存已渲染页面 (Redis/本地缓存)
- 支持 HTTP Range 请求

---

### 4. 动态水印增强 (DynamicWatermarkService)

**PRD 来源**: PRD 2.2 - 动态水印

#### 4.1 水印内容

```
用户名 + 时间戳 + TraceID
副文本: TraceID + FondsNo (用于追责)
```

#### 4.2 增强功能

```java
public interface DynamicWatermarkService {
    /** 应用水印到单页 */
    byte[] applyWatermark(byte[] pageImage, WatermarkConfig config);
    
    /** 生成水印文本 */
    String generateWatermarkText(UserContext user, String traceId);
}

public record WatermarkConfig(
    String text,
    String subText,
    float opacity,
    int angle,
    String fontName,
    int fontSize,
    Color color
) {}
```

#### 4.3 高敏模式

当 `security_level = SECRET` 时:
- 强制启用服务端渲染
- 前端只接收带水印的图片流
- 禁止下载原文件

---

## 📅 里程碑

### Week 1 (Day 1-5)
- [ ] Day 1: 结构化索引迁移 (amount/date/counterparty)
- [ ] Day 2: `ArchiveSearchService` 实现
- [ ] Day 3: 脱敏规则配置 + `DataMaskingService`
- [ ] Day 4: 脱敏 AOP 切面 (自动脱敏响应)
- [ ] Day 5: 检索 + 脱敏 API 集成测试

### Week 2 (Day 6-10)
- [ ] Day 6: `StreamingPreviewService` 基础
- [ ] Day 7: 分页渲染 + 缓存
- [ ] Day 8: `DynamicWatermarkService` 增强
- [ ] Day 9: 高敏模式 + 前端集成
- [ ] Day 10: 文档整理 + Sprint 2 验收

---

## ⚠️ 风险与缓解

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 大文件渲染慢 | 用户体验差 | 预渲染 + 缓存 |
| 脱敏规则复杂 | 维护困难 | 规则外置配置化 |
| OFD 预览 | 前端不支持 | 后端转 PDF 降级 |

---

## 📚 参考资料

- [PDFBox PDFRenderer](https://pdfbox.apache.org/2.0/cookbook/rendering.html)
- [Spring AOP 切面](https://docs.spring.io/spring-framework/reference/core/aop.html)
