一旦我所属的文件夹有所变化，请更新我。

# 模块治理机制

统一的模块治理服务，监控和管理代码模块的健康状况。

## 组件列表

### ModuleGovernanceService
模块治理服务，负责监控和管理代码模块。

**Features:**
- 模块信息统计
- 依赖关系分析
- 健康状况评估
- 度量指标计算

### ModuleInfo
模块信息实体。

### ModuleDependency
模块依赖关系实体。

### ModuleMetrics
模块度量指标实体。

## 使用示例

```java
@Autowired
private ModuleGovernanceService governanceService;

// 获取所有模块
List<ModuleInfo> modules = governanceService.getAllModules();

// 获取依赖关系
List<ModuleDependency> dependencies = governanceService.getDependencies();

// 获取度量指标
ModuleMetrics metrics = governanceService.getMetrics();
```

## 健康状态定义

- **HEALTHY**: 健康 - 复杂度<300, 文件数<30
- **ATTENTION**: 关注 - 复杂度300-500, 文件数30-50
- **WARNING**: 警告 - 复杂度>500, 文件数>50

## 收益

- 可视化模块健康状况
- 及时发现架构问题
- 指导重构决策
- 防止架构腐化

## TODO

- [ ] 添加循环依赖检测
- [ ] 集成 ArchUnit 进行架构验证
- [ ] 添加模块边界测试
- [ ] 实现模块拆分建议
