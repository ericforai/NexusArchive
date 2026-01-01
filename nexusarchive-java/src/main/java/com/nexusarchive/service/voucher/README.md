# 原始凭证服务模块

统一的原始凭证处理模块，将 OriginalVoucherService 拆分为职责单一的组件。

## 组件列表

### OriginalVoucherFacade
门面服务，协调整个原始凭证处理流程。

### VoucherQueryService
查询服务，负责所有查询操作。

**职责:**
- 分页查询凭证
- 详情查询
- 文件查询
- 版本历史查询
- 关联查询
- 类型查询

### VoucherCrudService
CRUD 服务，负责创建、更新、删除操作。

**职责:**
- 创建凭证
- 更新凭证
- 创建新版本
- 删除凭证
- 提交归档
- 确认归档

### VoucherFileManager
文件管理器，负责凭证文件的操作。

**职责:**
- 添加文件
- 下载文件
- 文件哈希计算
- PDF 发票解析

### VoucherRelationService
关联服务，管理原始凭证与记账凭证的关联关系。

**职责:**
- 创建关联
- 删除关联
- 查询关联

## 使用示例

```java
@Autowired
private OriginalVoucherFacade facade;

// 查询凭证
Page<OriginalVoucher> vouchers = facade.getVouchers(1, 10, null, null, null, null, null, null);

// 创建凭证
OriginalVoucher voucher = facade.create(voucherDto, userId);

// 添加文件
OriginalVoucherFile file = facade.addFile(voucherId, multipartFile, "PRIMARY", userId);

// 提交归档
facade.submitForArchive(voucherId, userId);
```

## 收益

- 每个组件职责单一，易于测试和维护
- 查询与 CRUD 分离，提高性能
- 文件管理独立，便于扩展
- 关联管理解耦，降低复杂度
- 版本控制逻辑清晰

## 重构前后对比

| 指标 | 重构前 | 重构后 |
|-----|-------|--------|
| 类行数 | 658 行 | 各模块 < 200 行 |
| 职责数量 | 9 | 每个模块 1-3 个 |
| 可测试性 | 需要大量 Mock | 可独立单元测试 |
| 扩展性 | 修改主类 | 添加新模块 |
