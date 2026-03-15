# SM2 签名服务测试执行指南

## 测试文件位置

```
nexusarchive-java/src/test/java/com/nexusarchive/service/signature/Sm2SignatureServiceTest.java
```

## 快速开始

### 1. 编译测试

```bash
cd nexusarchive-java
mvn test-compile
```

### 2. 运行单个测试类

```bash
mvn test -Dtest=Sm2SignatureServiceTest
```

### 3. 运行特定测试方法

```bash
# 运行签名测试
mvn test -Dtest=Sm2SignatureServiceTest#sign_success

# 运行验签测试
mvn test -Dtest=Sm2SignatureServiceTest#verify_success

# 运行并发测试
mvn test -Dtest=Sm2SignatureServiceTest#sign_concurrent
```

### 4. 运行所有签名相关测试

```bash
mvn test -Dtest=*Signature*Test
```

## 测试覆盖的场景

### ✅ 已实现测试

| 分类 | 测试方法 | 数量 |
|------|---------|------|
| 签名测试 | sign_* | 8 |
| 验签测试 | verify_* | 8 |
| PDF 签名验证 | verifyPdfSignature_* | 3 |
| OFD 签名验证 | verifyOfdSignature_* | 3 |
| 服务可用性 | isAvailable_* | 6 |
| 服务类型 | getServiceType_* | 2 |
| 边界条件 | unicodeData, specialCharacters, singleByte | 3 |
| 并发测试 | concurrent | 2 |
| 性能测试 | performance | 2 |
| 配置测试 | configuration_* | 2 |
| **总计** | | **41** |

### 📋 测试覆盖方法

- ✅ `sign(byte[] data, String certAlias)` - SM2 签名生成
- ✅ `verify(byte[] data, byte[] signature, String certAlias)` - SM2 签名验证
- ✅ `verifyPdfSignature(InputStream pdfStream)` - PDF 签名验证
- ✅ `verifyOfdSignature(InputStream ofdStream)` - OFD 签名验证
- ✅ `getServiceType()` - 获取服务类型
- ✅ `isAvailable()` - 服务可用性检查

## 测试结果预期

### 当前状态（无密钥库）

由于没有实际的密钥库文件，大部分测试会返回失败结果，但测试本身应该通过：

```bash
# 预期测试通过数量: ~35/41
# 预期测试失败数量: 0 (测试逻辑应该正确)
# 预期测试跳过数量: ~6 (PDF/OFD 相关可能跳过)
```

### 理想状态（有密钥库）

需要准备以下测试资源：

1. **PKCS12 密钥库** (`test-keystore.p12`)
   - 包含 SM2 密钥对
   - 包含有效的 X509 证书
   - 密码: `test123456`

2. **PDF 测试文件**
   - 包含有效 SM2 签名的 PDF 文件
   - 未签名的 PDF 文件

3. **OFD 测试文件**
   - 包含有效 SM2 签名的 OFD 文件
   - 未签名的 OFD 文件

## 故障排查

### 编译错误

如果遇到编译错误：

```bash
# 清理并重新编译
mvn clean compile test-compile

# 检查依赖
mvn dependency:tree
```

### 测试失败

如果测试失败：

```bash
# 查看详细错误信息
mvn test -Dtest=Sm2SignatureServiceTest -X

# 运行单个测试方法
mvn test -Dtest=Sm2SignatureServiceTest#sign_success
```

### 依赖问题

确保以下依赖可用：

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## 覆盖率报告

### 生成覆盖率报告

```bash
# 运行测试并生成覆盖率报告
mvn test jacoco:report -Dtest=Sm2SignatureServiceTest

# 查看报告
open nexusarchive-java/target/site/jacoco/index.html
```

### 覆盖率目标

- **方法覆盖率**: ≥ 80%
- **分支覆盖率**: ≥ 75%
- **行覆盖率**: ≥ 75%

## 持续集成

### CI/CD 配置

在 CI 管道中运行测试：

```yaml
test:
  script:
    - cd nexusarchive-java
    - mvn test -Dtest=Sm2SignatureServiceTest
    - mvn jacoco:report
  coverage: '/Total.*?([0-9]{1,3})%/'
```

## 最佳实践

### 1. 测试命名

使用描述性的测试名称：

```java
@Test
@DisplayName("SM2 签名 - 成功场景")
void sign_success() { }
```

### 2. 断言使用

使用 AssertJ 进行断言：

```java
assertThat(result).isNotNull();
assertThat(result.isSuccess()).isFalse();
assertThat(result.getErrorMessage()).contains("无法加载证书私钥");
```

### 3. 测试隔离

每个测试方法应该独立：

```java
@BeforeEach
void setUp() {
    sm2SignatureService = new Sm2SignatureService();
    ReflectionTestUtils.setField(sm2SignatureService, "keystorePath", TEST_KEYSTORE_PATH);
}

@AfterEach
void tearDown() {
    sm2SignatureService = null;
}
```

### 4. 边界条件

测试边界条件：

```java
@Test
@DisplayName("SM2 签名 - null 数据")
void sign_nullData() {
    SignResult result = sm2SignatureService.sign(null, certAlias);
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
}
```

## 扩展测试

### 添加新测试

1. 在 `Sm2SignatureServiceTest.java` 中添加新方法
2. 使用 `@Test` 和 `@DisplayName` 注解
3. 编写清晰的测试逻辑
4. 运行测试验证

### 集成测试

考虑添加集成测试：

```java
@SpringBootTest
class Sm2SignatureServiceIntegrationTest {
    @Autowired
    private Sm2SignatureService sm2SignatureService;

    @Test
    void endToEnd_signAndVerify() {
        // 完整的签名和验签流程
    }
}
```

## 相关文档

- **测试总结**: `Sm2SignatureServiceTest-Summary.md`
- **源代码**: `/main/java/.../service/signature/Sm2SignatureService.java`
- **接口定义**: `/main/java/.../service/signature/SignatureAdapter.java`
- **项目测试规范**: `/.claude/rules/common/testing.md`

## 支持

如有问题，请参考：

1. 项目测试规范: `/.claude/rules/common/testing.md`
2. TDD 工作流程: 使用 `tdd-guide` agent
3. 构建问题解决: 使用 `build-error-resolver` agent
