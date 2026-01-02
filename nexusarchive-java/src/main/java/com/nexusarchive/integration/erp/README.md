# ERP 集成模块

## 概述

本模块负责与各类 ERP 系统的集成，提供统一的适配器接口和元数据管理。

## 架构 (v2.4)

### 设计模式

- **适配器模式**: 统一不同 ERP 系统的接口差异
- **工厂模式**: ErpAdapterFactory 自动管理所有适配器实例
- **注解驱动**: @ErpAdapterAnnotation 实现声明式元数据定义
- **注册中心模式**: ErpMetadataRegistry 运行时元数据管理

### 模块层次

```
┌─────────────────────────────────────┐
│      Controller Layer               │
│  ErpConfigController, ErpScenario...  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Service Layer                  │
│  ErpSyncService, ErpChannelService  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Integration Layer                 │
│  ┌──────────────────────────────┐   │
│  │   ErpAdapterFactory          │   │
│  │   (获取适配器)               │   │
│  └────────────┬─────────────────┘   │
│               │                     │
│  ┌────────────▼─────────────────┐   │
│  │   ErpMetadataRegistry        │   │
│  │   (元数据注册与查询)         │   │
│  └────────────┬─────────────────┘   │
│               │                     │
│  ┌────────────▼─────────────────┐   │
│  │   ErpAdapter 实现            │   │
│  │   - YonSuiteErpAdapter       │   │
│  │   - KingdeeAdapter           │   │
│  │   - WeaverAdapter            │   │
│  │   - WeaverE10Adapter         │   │
│  │   - GenericErpAdapter        │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

## 适配器列表

| 标识 | 名称 | ERP类型 | 场景 | Webhook | 优先级 |
|------|------|---------|------|---------|--------|
| yonsuite | 用友YonSuite | YONSUITE | VOUCHER_SYNC, ATTACHMENT_SYNC, WEBHOOK | ✅ | 10 |
| kingdee | 金蝶云星空 | KINGDEE | VOUCHER_SYNC, ATTACHMENT_SYNC | ❌ | 20 |
| weaver | 浪潮GS | WEAVER | VOUCHER_SYNC | ❌ | 30 |
| weaver_e10 | 泛微E10 | WEAVER_E10 | VOUCHER_SYNC | ❌ | 40 |
| generic | 通用ERP | GENERIC | VOUCHER_SYNC | ❌ | 100 |

## 使用示例

### 1. 获取适配器

```java
@Autowired
private ErpAdapterFactory factory;

// 通过类型标识获取
ErpAdapter adapter = factory.getAdapter("yonsuite");

// 查询元数据
ErpMetadata metadata = factory.getMetadata("yonsuite");

// 列出所有可用适配器
List<ErpAdapterInfo> adapters = factory.listAvailableAdapters();
```

### 2. 调用适配器功能

```java
// 测试连接
ConnectionTestResult result = adapter.testConnection(config);

// 同步凭证
List<VoucherDTO> vouchers = adapter.syncVouchers(config, startDate, endDate);

// 查询凭证详情
VoucherDTO voucher = adapter.getVoucherDetail(config, voucherNo);

// 获取附件
List<AttachmentDTO> attachments = adapter.getAttachments(config, voucherNo);
```

### 3. 查询适配器能力

```java
@Autowired
private ErpMetadataRegistry registry;

// 获取元数据
ErpMetadata metadata = registry.getByIdentifier("yonsuite");

// 检查支持的场景
boolean supportsVoucherSync = metadata.getSupportedScenarios().contains("VOUCHER_SYNC");

// 检查是否支持 Webhook
boolean supportsWebhook = metadata.isSupportsWebhook();

// 按类型分组查询
List<ErpMetadata> yonsuiteAdapters = registry.getByErpType("YONSUITE");
```

## 开发新适配器

详见: [ERP 适配器开发指南](../../../../docs/architecture/erp-adapter-development-guide.md)

快速步骤:

1. 实现 `ErpAdapter` 接口
2. 添加 `@ErpAdapterAnnotation` 注解
3. 添加 `@Service` 注解
4. 编写单元测试
5. 运行 ArchUnit 验证

## 自指能力

本模块支持运行时自查询:

- ✅ 列出所有可用适配器
- ✅ 查询适配器元数据
- ✅ 检查适配器能力
- ✅ 按类型分组查询
- ✅ 优先级排序

## 架构测试

ArchUnit 规则强制执行:

1. 所有适配器必须有 `@ErpAdapterAnnotation` 注解
2. 适配器标识必须唯一
3. 模块边界清晰，无越界依赖
4. 元数据注册中心隔离

运行测试:

```bash
mvn test -Dtest=ArchitectureTest
```

## 相关文档

- [模块清单](../../../../docs/architecture/module-manifest.md)
- [适配器开发指南](../../../../docs/architecture/erp-adapter-development-guide.md)
- [架构防御指南](../../../../docs/architecture/architecture-defense-guide.md)
