# ERP 插件模块

统一的 ERP 集成插件架构，将 ErpSyncService 拆分为可插拔的 ERP 适配器。

## 组件列表

### ErpPlugin
ERP 插件接口，定义 ERP 集成的标准契约。

**方法:**
- `getPluginId()` - 插件标识符
- `getPluginName()` - 插件名称
- `sync()` - 执行同步
- `validateConfig()` - 验证配置

### ErpPluginContext
插件上下文，封装执行所需的参数。

### ErpPluginResult
插件结果，封装执行返回的数据。

### ErpPluginManager
插件管理器，管理所有插件的注册和调用。

### AbstractErpPlugin
插件抽象基类，提供通用实现。

### 实现类
- `YonSuiteErpPlugin` - 用友 YonSuite 插件
- `KingdeeErpPlugin` - 金蝶 K/3 Cloud 插件

## 使用示例

```java
@Autowired
private ErpPluginManager pluginManager;

// 执行同步
ErpPluginContext context = ErpPluginContext.builder()
    .config(erpConfig)
    .scenario(scenario)
    .startDate(startDate)
    .endDate(endDate)
    .operatorId(userId)
    .build();

ErpPluginResult result = pluginManager.sync("yonsuite", context);
```

## 添加自定义 ERP 插件

```java
@Component
public class CustomErpPlugin extends AbstractErpPlugin {
    @Override
    public String getPluginId() {
        return "custom";
    }

    @Override
    public String getPluginName() {
        return "自定义 ERP";
    }

    @Override
    public String getSupportedErpType() {
        return "CUSTOM";
    }

    @Override
    protected List<VoucherDTO> doSync(ErpAdapter adapter, 
                                       ErpConfig dtoConfig,
                                       ErpPluginContext context) {
        // 自定义同步逻辑
        return vouchers;
    }
}
```

## 收益

- 每个插件独立可维护
- 新增 ERP 无需修改核心服务
- 配置验证可自定义
- 插件可动态注册

## 重构前后对比

| 指标 | 重构前 | 重构后 |
|-----|-------|--------|
| ERP 集成方式 | 硬编码在服务中 | 插件化架构 |
| 添加新 ERP | 修改 ErpSyncService | 创建新插件 |
| 可测试性 | 需要完整服务 | 可独立单元测试 |
| 配置验证 | 通用逻辑 | 每个 ERP 自定义 |
