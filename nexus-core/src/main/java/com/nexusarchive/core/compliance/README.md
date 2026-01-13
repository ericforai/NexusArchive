一旦我所属的文件夹有所变化，请更新我。

# compliance 目录

> 一旦我所属的文件夹有所变化，请更新我。

## 目录说明

四性检测与合规引擎核心实现。
当前验签以 PDF 为准，OFD 验签暂停。

## 文件清单

### Sprint 0 (基础)

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `HashAlgorithm.java` | 枚举 | 支持的哈希算法类型 (SM3/SHA256) |
| `Sm3HashService.java` | 服务 | 纯哈希算法封装，支持 SM3/SHA256 |
| `FileHashService.java` | 服务 | 流式大文件哈希，避免 OOM |
| `MagicNumberValidator.java` | 组件 | 文件头 Magic Number 检测 (PDF/OFD/XML/JPEG/PNG) |
| `FourNatureCheckService.java` | 服务 | 四性检测引擎主入口（联动完整性/签名/病毒） |
| `FourNatureCheckRequest.java` | DTO | 四性检测请求参数（含 XML 与策略） |
| `FourNatureCheckResult.java` | DTO | 四性检测结果（含签名/完整性/病毒详情） |
| `ArchiveSubmitService.java` | 服务 | 归档提交整合示例 (隔离 + 四性) |
| `ArchiveSubmitRequest.java` | DTO | 归档提交请求参数（含四性策略） |
| `ArchiveSubmitResult.java` | DTO | 归档提交结果 |

### Sprint 1 (完整性/签名/病毒/审计/水印)

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `IntegrityChecker.java` | 接口 | 完整性检测接口 |
| `DefaultIntegrityChecker.java` | 服务 | XML 与 OFD/PDF 元数据一致性校验 |
| `IntegrityCheckResult.java` | DTO | 完整性检测结果 |
| `IntegrityDiff.java` | DTO | 字段差异项 |
| `DigitalSignatureVerifier.java` | 接口 | 数字签名验证接口 |
| `DefaultDigitalSignatureVerifier.java` | 服务 | PDF CMS/PKCS7 签名验证（时间戳/证书链/信任根） |
| `SignatureVerifyResult.java` | DTO | 签名验证结果（含时间戳有效性） |
| `VirusScanService.java` | 接口 | 病毒扫描接口 |
| `ClamAvVirusScanService.java` | 服务 | ClamAV 病毒扫描实现 |
| `MockVirusScanService.java` | 服务 | Mock 病毒扫描实现 (EICAR) |
| `VirusScanResult.java` | DTO | 扫描结果 |
| `VirusScanConfiguration.java` | 配置 | 病毒扫描服务装配配置 |
| `AuditLogService.java` | 服务 | 审计日志哈希链服务 |
| `AuditLogEntry.java` | 实体 | 审计日志条目 |
| `ChainVerifyResult.java` | DTO | 链式校验结果 |
| `ChainBreak.java` | DTO | 链式断裂点 |
| `WatermarkService.java` | 接口 | 水印服务接口 |
| `PdfWatermarkService.java` | 服务 | PDF 水印实现 (PDFBox, 流式输出) |
| `WatermarkConfig.java` | DTO | 水印配置 |

## 配置项

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `signature.truststore.path` | 空 | 信任根 KeyStore 路径（PKCS12） |
| `signature.truststore.password` | 空 | 信任根 KeyStore 密码 |
| `compliance.strict-mode` | `true` | 严格合规模式（TSA/信任根失败即失败） |
