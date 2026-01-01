# SIP 接收模块

统一的 SIP (Submission Information Package) 接收处理模块，将 IngestServiceImpl 拆分为职责单一的组件。

## 组件列表

### IngestFacade
门面服务，协调整个 SIP 接收流程。

**职责:**
- 协调验证器执行
- 协调文件处理
- 协调事件发布
- 协调状态跟踪

### IngestValidator
验证器接口，定义 SIP 请求验证规则。

**实现类:**
- `IntegrityValidator` - 完整性验证（附件数量、分录金额平衡）

### IngestFileHandler
文件处理器，负责 SIP 附件文件的临时存储。

**职责:**
- 文件类型验证
- Base64 解码
- 临时文件落地

### IngestEventPublisher
事件发布器，封装 Spring 事件发布机制。

**职责:**
- 发布 VoucherReceivedEvent 事件

### IngestStatusTracker
状态跟踪器，负责记录和更新 SIP 请求状态。

**职责:**
- 初始化请求状态
- 更新请求状态
- 查询请求状态

## 使用示例

```java
@Autowired
private IngestFacade ingestFacade;

// 接收 SIP 请求
IngestResponse response = ingestFacade.ingestSip(sipDto);

// 查询请求状态
IngestRequestStatus status = ingestFacade.getStatus(requestId);
```

## 自定义验证器

如需添加新的验证规则，实现 `IngestValidator` 接口：

```java
@Component
public class CustomValidator implements IngestValidator {
    @Override
    public void validate(AccountingSipDto sipDto) {
        // 自定义验证逻辑
    }

    @Override
    public String getName() {
        return "自定义验证器";
    }

    @Override
    public int getPriority() {
        return 10; // 数字越小优先级越高
    }
}
```

## 收益

- 每个组件职责单一，易于测试和维护
- 新增验证规则无需修改主服务
- 文件处理逻辑可复用
- 状态跟踪统一管理
- 事件发布解耦

## 重构前后对比

| 指标 | 重构前 | 重构后 |
|-----|-------|--------|
| 类行数 | 685 行 | 各模块 < 150 行 |
| 职责数量 | 6+ | 每个模块 1-2 个 |
| 可测试性 | 需要大量 Mock | 可独立测试 |
| 扩展性 | 修改主类 | 添加新组件 |
