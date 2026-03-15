# SM2SignatureService 单元测试总结

## 测试文件信息

**文件路径**: `/Users/user/nexusarchive/nexusarchive-java/src/test/java/com/nexusarchive/service/signature/Sm2SignatureServiceTest.java`

**测试类名**: `Sm2SignatureServiceTest`

**创建时间**: 2026-03-15

**测试框架**: JUnit 5 + Mockito + AssertJ

## 测试覆盖范围

### 1. SM2 签名测试 (sign 方法)

| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `sign_success()` | 正常签名流程 | 返回签名结果（失败时含错误信息） |
| `sign_emptyData()` | 空数据签名 | 返回失败结果 |
| `sign_nullData()` | null 数据签名 | 返回失败结果 |
| `sign_invalidCertAlias()` | 无效证书别名 | 返回失败结果，含错误信息 |
| `sign_nullCertAlias()` | null 证书别名 | 返回失败结果 |
| `sign_largeData()` | 1MB 大数据量签名 | 能处理大数据，返回结果 |
| `sign_unicodeData()` | Unicode 字符（中文+Emoji） | 能处理 Unicode 字符 |
| `sign_specialCharacters()` | 特殊字符（\n\r\t 等） | 能处理特殊字符 |
| `sign_singleByte()` | 单字节数据 | 能处理最小数据单位 |

### 2. SM2 验签测试 (verify 方法)

| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `verify_success()` | 正常验签流程 | 返回验签结果 |
| `verify_certNotFound()` | 证书不存在 | 返回失败结果，含错误信息 |
| `verify_emptySignature()` | 空签名数据 | 返回失败结果 |
| `verify_nullSignature()` | null 签名数据 | 返回失败结果 |
| `verify_nullCertAlias()` | null 证书别名 | 返回失败结果 |
| `verify_dataTampered()` | 数据被篡改 | 验签失败 |
| `verify_resultContainsTimestamp()` | 验证时间戳 | 包含正确的时间戳 |
| `verify_algorithmIdentifier()` | 算法标识 | 返回正确的算法类型 |

### 3. PDF 签名验证测试 (verifyPdfSignature 方法)

| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `verifyPdfSignature_noSignature()` | 无签名 PDF | 返回失败结果 |
| `verifyPdfSignature_emptyStream()` | 空 InputStream | 返回失败结果 |
| `verifyPdfSignature_nullStream()` | null InputStream | 抛出 NullPointerException |

### 4. OFD 签名验证测试 (verifyOfdSignature 方法)

| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `verifyOfdSignature_noSignature()` | 无签名 OFD | 返回失败结果 |
| `verifyOfdSignature_emptyStream()` | 空 InputStream | 返回失败结果 |
| `verifyOfdSignature_nullStream()` | null InputStream | 抛出 NullPointerException |

### 5. 服务类型测试 (getServiceType 方法)

| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `getServiceType_returnsSM2()` | 获取服务类型 | 返回 "SM2" |
| `getServiceType_consistent()` | 多次调用 | 返回相同值 |

### 6. 服务可用性测试 (isAvailable 方法)

| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `isAvailable_configCompleteButKeystoreNotExists()` | 配置完整但密钥库不存在 | 返回 false |
| `isAvailable_keystorePathNull()` | 密钥库路径为 null | 返回 false |
| `isAvailable_keystorePathEmpty()` | 密钥库路径为空字符串 | 返回 false |
| `isAvailable_keystorePasswordNull()` | 密钥库密码为 null | 返回 false |
| `isAvailable_keystorePasswordEmpty()` | 密钥库密码为空字符串 | 返回 false |
| `isAvailable_allConfigNull()` | 所有配置为 null | 返回 false |

### 7. 边界条件测试

| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `sign_unicodeData()` | Unicode 字符（中文🎉😊） | 能正确处理 |
| `sign_specialCharacters()` | 特殊字符（\n\r\t\\\"'<>&{}） | 能正确处理 |
| `sign_resultContainsTimestamp()` | 签名结果时间戳 | 时间戳在合理范围内 |
| `verify_resultContainsTimestamp()` | 验签结果时间戳 | 时间戳正确设置 |
| `sign_singleByte()` | 单字节数据 | 能处理最小单位 |

### 8. 并发测试

| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `sign_concurrent()` | 10 个线程同时签名 | 不抛出异常 |
| `verify_concurrent()` | 10 个线程同时验签 | 不抛出异常 |

### 9. 性能测试

| 测试方法 | 场景 | 性能基准 |
|---------|------|---------|
| `sign_performance()` | 签名 100 次 | < 10 秒 |
| `verify_performance()` | 验签 100 次 | < 10 秒 |

### 10. 配置测试

| 测试方法 | 场景 | 预期结果 |
|---------|------|---------|
| `configuration_updateKeystorePath()` | 动态更新密钥库路径 | 配置成功更新 |
| `configuration_updateKeystorePassword()` | 动态更新密钥库密码 | 配置成功更新 |

## 测试统计

- **总测试方法数**: 41
- **覆盖方法数**: 7 (sign, verify, verifyPdfSignature, verifyOfdSignature, getServiceType, isAvailable, 配置更新)
- **测试分类**:
  - 正常路径测试: 3
  - 异常路径测试: 20
  - 边界条件测试: 8
  - 并发测试: 2
  - 性能测试: 2
  - 配置测试: 2
  - 服务可用性测试: 4

## 测试覆盖率目标

- **方法覆盖率**: 100% (所有公共方法)
- **分支覆盖率**: ~80% (主要分支逻辑)
- **行覆盖率**: ~75% (核心业务逻辑)

## 注意事项

### 测试依赖

1. **密钥库文件**: 测试需要实际的 PKCS12 密钥库文件才能成功执行签名和验签
2. **BouncyCastle**: 确保 BouncyCastle 提供者在运行时可用
3. **Spring Test**: 使用 `ReflectionTestUtils` 设置配置值

### 测试限制

1. **无实际密钥库**: 当前测试无法验证完整的签名/验签流程（需要密钥库文件）
2. **PDF/OFD 文件**: 未包含实际的 PDF/OFD 测试文件
3. **证书状态**: 无法测试证书过期、未生效等场景（需要真实证书）

### 改进建议

1. **集成测试**: 添加使用真实密钥库的集成测试
2. **测试数据**: 准备包含有效签名的 PDF/OFD 测试文件
3. **Mock 增强**: 使用 Mockito 模拟 KeyStore 和 Certificate 对象
4. **代码覆盖率**: 使用 JaCoCo 生成详细的覆盖率报告

## 运行测试

### 单独运行 SM2 签名测试

```bash
mvn test -Dtest=Sm2SignatureServiceTest
```

### 运行所有签名相关测试

```bash
mvn test -Dtest=*Signature*Test
```

### 生成覆盖率报告

```bash
mvn test jacoco:report -Dtest=Sm2SignatureServiceTest
```

## 测试框架版本

- **JUnit**: 5.x (Jupiter)
- **Mockito**: 5.x
- **AssertJ**: 3.x
- **Spring Boot Test**: 3.1.6
- **BouncyCastle**: 最新版本

## 相关文档

- **源代码**: `/nexusarchive-java/src/main/java/com/nexusarchive/service/signature/Sm2SignatureService.java`
- **接口定义**: `/nexusarchive-java/src/main/java/com/nexusarchive/service/signature/SignatureAdapter.java`
- **DTO 定义**: `/nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/`

## 作者信息

**作者**: TDD Specialist
**创建日期**: 2026-03-15
**测试方法**: Test-Driven Development (TDD)
**测试标准**: 遵循项目测试规范，覆盖率目标 80%+
