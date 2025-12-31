一旦我所属的文件夹有所变化，请更新我。
本目录存放四性检测与哈希相关测试。
用于合规能力单元验证。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `FourNatureCheckServiceTests.java` | 测试 | 四性检测服务覆盖测试（签名/完整性/病毒） |
| `FourNatureIntegrationTests.java` | 测试 | 四性检测样例集成校验 |
| `AuditLogServiceTests.java` | 测试 | 审计哈希链校验测试 |
| `MagicNumberValidatorTests.java` | 测试 | Magic Number 文件头校验测试 |
| `MockVirusScanServiceTests.java` | 测试 | Mock 病毒扫描测试 |
| `DefaultDigitalSignatureVerifierTests.java` | 测试 | PDF CMS 验签覆盖测试（TSA/信任根） |
| `PdfWatermarkServiceTests.java` | 测试 | PDF 水印流式渲染测试 |
| `Sm3BenchmarkTests.java` | 测试 | SM3 vs SHA256 性能基准 |
| `Sm3HashServiceTests.java` | 测试 | SM3 哈希服务测试 |
