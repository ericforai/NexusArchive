# 全宗隔离 P0 问题修复计划

**创建日期**: 2025-01-07
**优先级**: P0 (数据安全)
**影响范围**: 数据统计、案卷管理、借阅管理

---

## 问题概述

三个核心功能模块存在全宗隔离缺失问题，可能导致跨全宗数据泄露。

---

## 问题 1: StatsController.borrowing 统计无全宗过滤

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/controller/StatsController.java:55-78`

### 现状代码
```java
@GetMapping("/borrowing")
public Result<BorrowingStats> getBorrowingStats() {
    long pending = borrowingMapper.selectCount(
            new LambdaQueryWrapper<Borrowing>().eq(Borrowing::getStatus, "PENDING")
    );
    // 直接使用 Mapper，没有全宗过滤！
}
```

### 修复方案
```java
@GetMapping("/borrowing")
public Result<BorrowingStats> getBorrowingStats() {
    DataScopeContext scope = dataScopeService.resolve();

    long pending = borrowingMapper.selectCount(
            new LambdaQueryWrapper<Borrowing>()
                    .eq(Borrowing::getStatus, "PENDING")
                    .apply(wrapper -> borrowingScopePolicy.apply(wrapper, scope))
    );
    // ... 其他统计同样处理
}
```

### 依赖注入
- 添加 `@Autowired private DataScopeService dataScopeService;`
- 添加 `@Autowired private BorrowingScopePolicy borrowingScopePolicy;`

---

## 问题 2: VolumeQuery 案卷查询无全宗过滤

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/volume/VolumeQuery.java`

### 现状代码
```java
public Page<Volume> getVolumeList(VolumeMapper volumeMapper, int page, int limit, String status) {
    Page<Volume> pageObj = new Page<>(page, limit);
    LambdaQueryWrapper<Volume> wrapper = new LambdaQueryWrapper<>();
    if (status != null && !status.isEmpty()) {
        wrapper.eq(Volume::getStatus, status);
    }
    // ❌ 缺少全宗过滤
    return volumeMapper.selectPage(pageObj, wrapper);
}
```

### 修复方案
```java
public Page<Volume> getVolumeList(VolumeMapper volumeMapper, int page, int limit, String status) {
    Page<Volume> pageObj = new Page<>(page, limit);
    LambdaQueryWrapper<Volume> wrapper = new LambdaQueryWrapper<>();

    // ✅ 添加全宗过滤
    String currentFondsNo = com.nexusarchive.security.FondsContext.getCurrentFondsNo();
    if (currentFondsNo != null && !currentFondsNo.isEmpty()) {
        wrapper.eq(Volume::getFondsNo, currentFondsNo);
    } else {
        // 没有全宗上下文时不返回任何数据
        wrapper.eq(Volume::getId, "NEVER_MATCH");
    }

    if (status != null && !status.isEmpty()) {
        wrapper.eq(Volume::getStatus, status);
    }
    wrapper.orderByDesc(Volume::getCreatedTime);
    return volumeMapper.selectPage(pageObj, wrapper);
}
```

### 同时修复的方法
- `getVolumeById()` - 添加 fondsNo 验证
- `getVolumeFiles()` - 间接通过 Archive 的 fondsNo 验证

---

## 问题 3: BorrowingScopePolicy 未使用 FondsContext

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/modules/borrowing/infra/BorrowingScopePolicyImpl.java`

### 现状代码
```java
@Override
public void apply(QueryWrapper<Borrowing> wrapper, DataScopeContext context) {
    if (context == null || context.isAll()) {
        return;
    }
    // ❌ 缺少 FondsContext.getCurrentFondsNo() 优先检查
    // ...
}
```

### 修复方案
```java
@Override
public void apply(QueryWrapper<Borrowing> wrapper, DataScopeContext context) {
    // 优先级1: 当前选中的全宗（FondsContext）
    String currentFondsNo = com.nexusarchive.security.FondsContext.getCurrentFondsNo();
    if (currentFondsNo != null && !currentFondsNo.isEmpty()) {
        wrapper.eq("fonds_no", currentFondsNo);
        return;
    }

    // 优先级2: 系统管理员
    if (context == null || context.isAll()) {
        return;
    }

    // 优先级3: self scope
    if (context.isSelf()) {
        if (context.userId() != null) {
            wrapper.eq("user_id", context.userId());
        } else {
            wrapper.eq("1", "0");
        }
        return;
    }

    // 优先级4: allowedFonds 列表
    Set<String> allowedFonds = context.allowedFonds();
    if (!allowedFonds.isEmpty()) {
        wrapper.in("fonds_no", allowedFonds);
        return;
    }

    // 无权限
    wrapper.eq("1", "0");
}
```

---

## 执行顺序

| 步骤 | 任务 | 依赖 |
|------|------|------|
| 1 | 修复 BorrowingScopePolicyImpl | 无 |
| 2 | 修复 StatsController.borrowing | 步骤1 |
| 3 | 修复 VolumeQuery | 无 |
| 4 | 运行测试验证 | 步骤1,2,3 |

---

## 验证计划

### 单元测试
```bash
# 运行相关测试
mvn test -Dtest=BorrowingApplicationServiceTest
mvn test -Dtest=StatsServiceTest
mvn test -Dtest=VolumeServiceTest
```

### 手动验证
1. 创建两个不同全宗的测试数据
2. 切换全宗，验证只能看到当前全宗的数据
3. 验证统计数据只包含当前全宗
4. 验证案卷列表只包含当前全宗

---

## 风险评估

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| 破坏现有功能 | 中 | 完整的回归测试 |
| 性能影响 | 低 | 添加索引建议 |
| 兼容性问题 | 低 | 保留 DataScopeContext 逻辑 |

---

## 后续改进 (P1)

1. **IngestRequestStatus** 添加 fondsNo 字段
2. **NavController** 使用 FondsContext 替代请求参数
3. 添加 ArchUnit 架构测试规则，强制全宗过滤
