# 防止类型不一致问题的系统性方案

## 方案对比

| 方案 | 效果 | 成本 | 优先级 |
|-----|------|------|-------|
| 1. 契约测试 (Pact) | ⭐⭐⭐⭐⭐ | 中 | P0 |
| 2. 类型代码单一源 (OpenAPI) | ⭐⭐⭐⭐⭐ | 低 | P0 |
| 3. 数据一致性单元测试 | ⭐⭐⭐⭐ | 低 | P1 |
| 4. Git Hook 预检查 | ⭐⭐⭐ | 低 | P1 |
| 5. CI 集成检查 | ⭐⭐⭐⭐ | 中 | P2 |

---

## 方案一：契约测试 (推荐实施)

在后端添加一个专门的测试，验证前端使用的所有类型代码都能正确查询到数据：

```java
// OriginalVoucherServiceTest.java
@Test
@DisplayName("契约测试：前端类型代码应能查询到数据（包括旧代码别名）")
void shouldQueryVouchersWithFrontendTypeCodes() {
    // 准备：创建使用旧类型代码的数据
    OriginalVoucher bankSlip = createVoucher("BANK_SLIP");
    OriginalVoucher vatInvoice = createVoucher("VAT_INVOICE");

    // 执行：使用前端期望的新代码查询
    Page<OriginalVoucher> result = service.getVouchers(
        1, 10, null, null, "BANK_RECEIPT", null, null, null, "ENTRY,PARSED,PARSE_FAILED"
    );

    // 断言：应该通过别名映射查到旧代码的数据
    assertThat(result.getRecords()).hasSize(1);
    assertThat(result.getRecords().get(0).getVoucherType()).isEqualTo("BANK_SLIP");
}
```

**优点**：代码改动小，立即生效，覆盖核心场景

---

## 方案二：类型代码单一源 (根本解决)

### 2.1 创建共享的类型定义文件

```yaml
# src/main/resources/api/voucher-types.yaml
voucherTypes:
  - code: "BANK_RECEIPT"
    name: "银行回单"
    aliases: ["BANK_SLIP"]  # 历史别名
    category: "BANK"

  - code: "INV_VAT_E"
    name: "增值税电子发票"
    aliases: ["VAT_INVOICE"]
    category: "INVOICE"
```

### 2.2 后端启动时加载并验证

```java
@Component
public class VoucherTypeAliasLoader implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // 从 YAML 加载类型定义
        List<VoucherTypeDefinition> types = loadFromYaml();

        // 验证：数据库中的所有类型代码要么在 code 列表，要么在 aliases 列表
        List<String> dbTypes = voucherMapper.getAllTypes();
        List<String> definedTypes = types.stream()
            .flatMap(t -> Stream.concat(Stream.of(t.getCode()), t.getAliases().stream()))
            .toList();

        List<String> undefinedTypes = dbTypes.stream()
            .filter(t -> !definedTypes.contains(t))
            .toList();

        if (!undefinedTypes.isEmpty()) {
            throw new IllegalStateException(
                "数据库中存在未定义的类型代码: " + undefinedTypes +
                "。请在 voucher-types.yaml 中添加定义或添加别名。"
            );
        }

        // 构建别名映射表供运行时使用
        this.typeAliasMap = buildAliasMap(types);
    }
}
```

### 2.3 前端从 OpenAPI 生成类型

```bash
# npm script
"generate:types": "openapi-typescript http://localhost:19090/api/v3/api-docs -o src/api/types.ts"
```

**优点**：单一事实来源，前后端自动同步

---

## 方案三：数据一致性检查 (简单有效)

在 `SchemaValidator` 中添加外键引用完整性检查：

```java
@Component
public class SchemaValidator implements ApplicationRunner {

    private void validateVoucherFondsReference() {
        // 检查：arc_original_voucher.fonds_code 必须存在于 sys_entity.id
        String sql = """
            SELECT DISTINCT v.fonds_code
            FROM arc_original_voucher v
            WHERE v.deleted = 0
            AND NOT EXISTS (
                SELECT 1 FROM sys_entity e WHERE e.id = v.fonds_code AND e.deleted = 0
            )
            """;

        List<String> orphanFonds = jdbcTemplate.queryForList(sql, String.class);
        if (!orphanFonds.isEmpty()) {
            log.error("发现孤儿全宗代码: {}，这些单据将无法被查询到", orphanFonds);
        }
    }

    private void validateVoucherTypeReference() {
        // 检查：arc_original_voucher.voucher_type 必须存在于 sys_original_voucher_type.type_code
        String sql = """
            SELECT DISTINCT v.voucher_type
            FROM arc_original_voucher v
            WHERE v.deleted = 0
            AND NOT EXISTS (
                SELECT 1 FROM sys_original_voucher_type t WHERE t.type_code = v.voucher_type AND t.enabled = true
            )
            """;

        List<String> unknownTypes = jdbcTemplate.queryForList(sql, String.class);
        if (!unknownTypes.isEmpty()) {
            log.warn("发现未知类型代码: {}，可能需要添加类型别名映射", unknownTypes);
        }
    }
}
```

**优点**：启动时自动检查，问题早发现

---

## 方案四：Git Hook 预检查

```bash
# .git/hooks/pre-commit
#!/bin/bash

echo "检查类型代码变更..."

# 检查是否修改了类型相关的文件
if git diff --cached --name-only | grep -E "(voucher|type|fonds)"; then
    echo "检测到类型/全宗相关代码变更，请确认："
    echo "1. 前端类型定义是否同步更新？"
    echo "2. 后端类型别名映射是否更新？"
    echo "3. 是否需要数据迁移脚本？"
    echo "4. 是否需要清除缓存？"

    read -p "是否继续提交？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi
```

---

## 方案五：类型别名自动生成

让后端根据数据库配置自动生成别名映射，而不是硬编码：

```java
@Service
public class OriginalVoucherService {

    private Map<String, List<String>> typeAliasMap;

    @PostConstruct
    public void initTypeAliases() {
        // 从 sys_original_voucher_type 表读取别名配置
        // 如果表结构支持，添加 parent_type_code 字段指向主类型
        this.typeAliasMap = buildAliasMapFromDatabase();
    }

    private List<String> getTypeAliases(String typeCode) {
        // 自动查找：type_code 本身 + 所有指向 type_code 的别名
        return typeAliasMap.getOrDefault(typeCode, List.of(typeCode));
    }
}
```

数据库扩展：

```sql
-- 添加别名支持字段
ALTER TABLE sys_original_voucher_type ADD COLUMN parent_type_code VARCHAR(50);
COMMENT ON COLUMN sys_original_voucher_type.parent_type_code IS '指向主类型代码，用于类型别名映射';

-- 配置别名
UPDATE sys_original_voucher_type SET parent_type_code = 'BANK_RECEIPT' WHERE type_code = 'BANK_SLIP';
UPDATE sys_original_voucher_type SET parent_type_code = 'INV_VAT_E' WHERE type_code = 'VAT_INVOICE';
```

---

## 推荐实施路线

### 阶段一：立即实施（1小时内）
1. 添加数据一致性启动检查（方案三）
2. 添加契约测试用例（方案一）

### 阶段二：短期优化（1天内）
3. 创建类型定义 YAML 文件
4. 添加别名自动加载机制

### 阶段三：长期建设（1周内）
5. 前端从 OpenAPI 生成类型
6. 配置 CI 集成检查

---

## 快速启动代码

```java
// 立即可用：添加到 OriginalVoucherServiceTest.java
@Test
@DisplayName("类型别名映射：BANK_RECEIPT 应能查到 BANK_SLIP 数据")
void testBankReceiptAliasMapping() {
    // GIVEN: 数据库中有 BANK_SLIP 类型的单据
    String sql = "SELECT COUNT(*) FROM arc_original_voucher WHERE voucher_type = 'BANK_SLIP'";
    Integer existingCount = jdbcTemplate.queryForObject(sql, Integer.class);
    assumeTrue(existingCount != null && existingCount > 0, "需要 BANK_SLIP 类型的测试数据");

    // WHEN: 用 BANK_RECEIPT 查询
    LambdaQueryWrapper<OriginalVoucher> wrapper = new LambdaQueryWrapper<>();
    List<String> types = getTypeAliases("BANK_RECEIPT");  // 应返回 ["BANK_RECEIPT", "BANK_SLIP"]
    wrapper.in(OriginalVoucher::getVoucherType, types);

    List<OriginalVoucher> result = voucherMapper.selectList(wrapper);

    // THEN: 应该查到数据
    assertThat(result).isNotEmpty();
}
```
