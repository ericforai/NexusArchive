一旦我所属的文件夹有所变化，请更新我。

# 匹配策略模块

统一的凭证匹配策略模块，将 VoucherMatchingEngine 的匹配逻辑拆分为独立的策略。

## 组件列表

### MatchStrategy
匹配策略接口，定义评分策略的契约。

**实现类:**
- `AmountMatchStrategy` - 金额匹配策略 (权重 40)
- `DateMatchStrategy` - 日期匹配策略 (权重 20)
- `CounterpartyMatchStrategy` - 客商匹配策略 (权重 25)
- `VoucherNumberMatchStrategy` - 凭证号匹配策略 (权重 15)

### MatchContext
匹配上下文，传递匹配过程中的上下文信息。

### MatchingEngineFacade
匹配引擎门面，协调各个策略计算综合得分。

## 使用示例

```java
@Autowired
private MatchingEngineFacade facade;

// 创建上下文
MatchContext context = MatchContext.builder()
    .scene("PAYMENT")
    .evidenceRole("INVOICE")
    .toleranceConfig(Map.of("amount", 0.01, "date", 3))
    .build();

// 计算单个候选得分
int score = facade.calculateScore(voucher, candidate, context);

// 批量计算
facade.scoreCandidates(voucher, candidates, context);
```

## 添加自定义策略

```java
@Component
public class CustomMatchStrategy implements MatchStrategy {
    @Override
    public int calculateScore(VoucherData voucher, ScoredCandidate candidate, MatchContext context) {
        // 自定义评分逻辑
        return score;
    }

    @Override
    public String getName() {
        return "自定义策略";
    }

    @Override
    public int getWeight() {
        return 30; // 自定义权重
    }

    @Override
    public boolean isApplicable(MatchContext context) {
        // 决定是否适用当前场景
        return "CUSTOM".equals(context.getScene());
    }
}
```

## 收益

- 每个策略独立可测试
- 新增策略无需修改引擎
- 权重可配置
- 支持策略组合

## 重构前后对比

| 指标 | 重构前 | 重构后 |
|-----|-------|--------|
| 策略数量 | 硬编码在引擎中 | 4 个独立策略 |
| 可测试性 | 需要完整引擎 | 可独立单元测试 |
| 扩展性 | 修改引擎代码 | 添加新策略 |
