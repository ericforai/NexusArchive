# 电子会计档案合规与信创标准 (Compliance & Xinchuang Standards)

## 1. 核心原则
**Constraint**: Compliance > Performance. Data Integrity > User Experience.
(合规优于性能，数据完整性优于用户体验)

## 2. 强制性技术规范

### 2.1 数据精度 (Data Precision)
*   **金额 (Money)**: 必须使用 `java.math.BigDecimal`。严禁使用 `double` 或 `float`。
*   **舍入模式**: 默认为 `RoundingMode.HALF_UP`。

### 2.2 哈希与摘要 (Hashing)
*   **算法**: 必须支持 **SM3** (国密标准)。
*   **兜底**: 仅在 SM3 不可用时使用 SHA-256。
*   **时机**: 任何文件上传时必须立即计算摘要。

### 2.3 档案保存期限 (Retention Period)
*   **标准值**: 仅允许 `10Y` (10年), `30Y` (30年), `PERMANENT` (永久)。
*   **依据**: DA/T 94-2022 第 7.1 条。

### 2.4 文件格式 (File Formats)
*   **版式文件**: 优先使用 **OFD** (国产版式标准)，其次为 PDF。
*   **黑名单**: 严禁将二进制文件直接存储在数据库中 (必须使用 OSS/NAS 存储路径)。

## 3. 四性检测 (The "Four-Natures" Check)
依据 **DA/T 92-2022**，归档时必须进行以下检测：

1.  **真实性 (Authenticity)**:
    *   重新计算文件哈希并与来源比对。
    *   验证数字签名 (PDF 支持内嵌 SM2 签名验证; OFD 暂不支持)。
2.  **完整性 (Integrity)**:
    *   验证元数据 (GB/T 39362) 是否齐全。
    *   验证附件数量是否匹配清单。
3.  **可用性 (Usability)**:
    *   检查文件头 (Magic Number)。
    *   尝试解析文件 (Dry Parse)。
4.  **安全性 (Safety)**:
    *   病毒扫描 (集成 ClamAV TCP 扫描)。
    *   权限校验。

## 4. 归档包结构 (AIP Structure)
```text
/AIP_Root
  ├── /Content       (实际文件: OFD, PDF)
  ├── /Metadata      (XML 元数据文件中包含 DB 信息)
  │    └── metadata.xml (遵循 EER Schema)
  └── /Logs          (该档案的审计日志)
```
