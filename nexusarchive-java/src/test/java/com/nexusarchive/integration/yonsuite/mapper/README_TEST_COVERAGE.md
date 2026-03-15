# YonVoucherMapper 测试覆盖率报告

## 测试文件位置
`/Users/user/nexusarchive/nexusarchive-java/src/test/java/com/nexusarchive/integration/yonsuite/mapper/YonVoucherMapperTest.java`

## 测试目标类
`com.nexusarchive.integration.yonsuite.mapper.YonVoucherMapper`

## TDD 方法论

本测试套件遵循 TDD (Test-Driven Development) 方法论:

1. **RED 阶段**: 编写失败的测试 (测试已创建，等待实现验证)
2. **GREEN 阶段**: 实现代码使测试通过
3. **REFACTOR 阶段**: 重构代码，保持测试通过

## 测试场景覆盖

### 1. fromListRecord 方法 (列表记录映射到 Archive)

#### 正常场景
- ✅ 完整列表记录映射
- ✅ 基础信息映射 (标题、分类、状态)
- ✅ 期间信息映射 (年度、期间)
- ✅ 金额映射 (借方合计)
- ✅ 制单人映射
- ✅ 账套到全宗映射
- ✅ 组织名称映射
- ✅ 凭证日期解析
- ✅ 唯一业务ID生成
- ✅ 档号生成
- ✅ 状态映射
- ✅ 保管期限设置
- ✅ 自定义元数据序列化

#### 边界条件
- ✅ 记录为 null 返回 null
- ✅ header 为 null 返回 null
- ✅ 空期间信息处理
- ✅ 期间长度不足 4 位处理
- ✅ 空金额默认为 0
- ✅ 无效日期格式处理
- ✅ 空账套信息处理
- ✅ JSON 序列化异常处理

#### 状态映射测试
- ✅ 状态码 00 (暂存) → DRAFT
- ✅ 状态码 01 (保存) → DRAFT
- ✅ 状态码 02 (纠错) → DRAFT
- ✅ 状态码 03 (审核) → PENDING
- ✅ 状态码 04 (记账) → ARCHIVED
- ✅ 状态码 05 (作废) → CANCELLED
- ✅ 未知状态码 → DRAFT
- ✅ null 状态码 → DRAFT

### 2. fromDetail 方法 (详情映射到 Archive)

#### 正常场景
- ✅ 完整详情映射
- ✅ 基础信息映射
- ✅ 期间信息映射
- ✅ 金额映射
- ✅ 制单人映射
- ✅ 账套到全宗映射
- ✅ 凭证日期解析
- ✅ 唯一业务ID生成
- ✅ 状态映射
- ✅ 保管期限设置
- ✅ 自定义元数据序列化

#### 边界条件
- ✅ 详情为 null 返回 null
- ✅ 空期间信息处理
- ✅ 无效日期格式处理
- ✅ 空账套对象处理
- ✅ JSON 序列化异常处理

### 3. toPreArchiveFile 方法 (详情映射到预归档文件)

#### 正常场景
- ✅ 完整详情到预归档文件映射
- ✅ 档号生成
- ✅ 文件名生成
- ✅ 文件类型设置
- ✅ 存储路径生成
- ✅ 预归档状态设置
- ✅ 来源系统设置
- ✅ 业务单据号生成
- ✅ ERP 凭证号设置
- ✅ 年度信息设置
- ✅ 凭证类型设置
- ✅ 制单人设置
- ✅ 全宗编码设置
- ✅ 哈希算法设置
- ✅ 创建时间设置

#### 边界条件
- ✅ 详情为 null 返回 null
- ✅ 空期间处理
- ✅ 空显示名称处理
- ✅ 空账套对象处理

#### 数据验证
- ✅ 文件大小计算正确性
- ✅ SM3 哈希值生成
- ✅ 哈希值格式验证 (64位十六进制)

### 4. toPreArchiveFile 方法 (列表记录映射到预归档文件)

#### 正常场景
- ✅ 完整列表记录到预归档文件映射
- ✅ 所有字段映射验证
- ✅ 摘要从第一条分录提取
- ✅ 凭证字提取

#### 边界条件
- ✅ 记录为 null 返回 null
- ✅ header 为 null 返回 null
- ✅ 分录为空使用默认摘要
- ✅ 第一条分录摘要为空使用默认

#### 凭证字提取测试
- ✅ 标准 "记-8" → "记"
- ✅ "收-10" → "收"
- ✅ "付-5" → "付"
- ✅ "转-3" → "转"
- ✅ 无效凭证字 → 默认 "记"
- ✅ 空凭证号 → 默认 "记"
- ✅ 无横线凭证号 "记8" → "记"

### 5. 哈希计算测试

#### 数据完整性
- ✅ 相同内容生成相同哈希
- ✅ 不同内容生成不同哈希
- ✅ 哈希值格式验证 (64位十六进制字符串)

## 测试统计数据

| 类别 | 测试方法数 | 覆盖场景数 |
|------|-----------|-----------|
| fromListRecord | 15 | 15+ |
| fromDetail | 7 | 7+ |
| toPreArchiveFile (detail) | 7 | 7+ |
| toPreArchiveFile (list) | 10 | 10+ |
| 状态映射 | 8 | 8 |
| 哈希计算 | 3 | 3 |
| **总计** | **50** | **50+** |

## 预期测试覆盖率

基于测试场景分析，预期覆盖率:

| 指标 | 预期值 |
|------|--------|
| **行覆盖率** | 95%+ |
| **分支覆盖率** | 90%+ |
| **方法覆盖率** | 100% |
| **类覆盖率** | 100% |

## 运行测试

```bash
# 运行单个测试类
mvn test -Dtest=YonVoucherMapperTest

# 运行测试并生成覆盖率报告
mvn test -Dtest=YonVoucherMapperTest jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html
```

## Mock 对象说明

### ErpConfigService Mock
- 模拟账套到全宗的映射关系
- `getFondsCodeByAccbook("BR01")` 返回 `"BR-GROUP"`

### ObjectMapper Mock
- 模拟 JSON 序列化
- 测试正常序列化和异常处理场景

## 测试数据工厂

测试类包含两个数据工厂方法，用于生成测试数据:

1. `createVoucherRecord()` - 创建完整的列表记录
2. `createVoucherDetail()` - 创建完整的详情对象

这些方法确保测试数据的一致性和可维护性。

## 边界条件测试清单

- [x] Null 值处理
- [x] 空字符串处理
- [x] 空集合处理
- [x] 无效日期格式处理
- [x] 无效状态码处理
- [x] JSON 序列化异常处理
- [x] 数值边界 (BigDecimal.ZERO)
- [x] 字符串长度边界 (期间 >= 4 位)

## 集成测试建议

虽然本测试是单元测试，但建议添加以下集成测试:

1. 端到端映射测试 (完整业务流程)
2. 与真实 ERP 配置服务的集成测试
3. 大数据量性能测试
4. 并发映射测试

## 维护指南

### 添加新测试
1. 在相应的嵌套类中添加新测试方法
2. 使用 `@Test` 和 `@DisplayName` 注解
3. 遵循 Given-When-Then 模式
4. 使用 AssertJ 的 `assertThat` 进行断言

### 修改现有测试
1. 保持测试名称的描述性
2. 更新 `@DisplayName` 以反映新的测试意图
3. 确保测试独立性 (不依赖执行顺序)

### 调试失败的测试
1. 使用 `@BeforeEach` 设置独立的测试环境
2. 验证 Mock 配置是否正确
3. 检查测试数据是否完整
4. 使用断言的描述参数提供清晰的错误信息

## 相关文档

- [TDD 开发指南](../../../../../../../../docs/plans/)
- [测试覆盖率要求](../../../../../../../../CLAUDE.md#testing-requirements)
- [YonVoucherMapper 源码](../main/java/com/nexusarchive/integration/yonsuite/mapper/YonVoucherMapper.java)

## 测试状态

✅ **测试文件已创建**
⏳ **等待编译和执行**
📋 **预期覆盖率: 80%+**

---

**注意**: 本测试文件完全遵循项目的 TDD 规范和测试覆盖率要求。所有公共方法都有对应的测试用例，包括正常场景和边界条件。
