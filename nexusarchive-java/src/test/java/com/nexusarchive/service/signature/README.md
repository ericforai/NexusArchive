一旦我所属的文件夹有所变化，请更新我。
本目录存放 signature 相关测试内容。
用于验证 SM2 签名/验签、PDF 签名解析、归一化结果映射与降级行为。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `Sm2SignatureServiceTest.java` | Java 类 | SM2 签名/验签服务完整单元测试 |
| `PdfSignatureVerificationServiceTest.java` | Java 类 | PDF 签名验证服务测试 |

## 测试覆盖范围

### Sm2SignatureServiceTest

**签名测试**:
- 正常签名流程
- 空数据、null 数据签名
- 无效证书别名处理
- 大数据量签名 (1MB)
- Unicode 和特殊字符支持

**验签测试**:
- 正常验签流程
- 证书不存在场景
- 空签名、null 签名处理
- 数据篡改检测

**文件签名验证**:
- PDF 签名验证（无签名场景）
- OFD 签名验证（无签名场景）
- null InputStream 处理

**服务可用性**:
- 配置完整性检查
- BouncyCastle 提供者可用性
- 密钥库加载验证

**边界条件**:
- 单字节数据处理
- 并发签名/验签
- 性能测试（100次操作）

**配置测试**:
- 动态更新密钥库配置
- 配置参数验证
