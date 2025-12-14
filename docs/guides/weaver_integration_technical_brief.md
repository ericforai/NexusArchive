# 泛微 OA (Ecology 9) 集成技术方案详解

## 1. 当前状态说明：骨架已成，只待注入灵魂

针对您的疑问，回答非常明确：**目前还不能同步真实数据。**

我们刚才完成的是 **"集成通道的开通"** 和 **"业务流程的框架搭建"**。
这就好比我们已经把档案系统的"水管"接到了泛微OA的"墙根下"，并且装好了"水龙头"（界面上的按钮）。
但是：
1.  **墙还需要打通**：我们使用的是演示用的 URL 和 AppKey，连不上您真实的服务器。
2.  **水管还需要接头**：`WeaverAdapter` 中的代码目前是 Mock（模拟）逻辑，还没有写入真实的 HTTP 调用代码。

---

## 2. 泛微 OA 集成技术深度解析

作为档案与集成专家，我不仅知道泛微的 API，还非常熟悉其特有的 **E-Bridge / RESTful** 架构。

如果您的系统是 **Ecology 9 (E9)**，真实对接流程如下：

### 2.1 复杂的认证机制 (Handshake)
泛微的 API 安全性较高，不是简单的填一个 Key 就能用，而是采用 **RSA 非对称加密 + Token 换取** 的机制。

**标准握手四步曲**：
1.  **申请 Token**：
    *   调用 `POST /api/ec/dev/auth/applytoken`
    *   参数：`appid` (需在 OA 后台注册)
    *   返回：临时 `token`
2.  **计算密钥**：
    *   OA 服务端会返回一个公钥 (SPK)。
    *   我们需要结合本地的 `Secret`，使用 **RSA 算法** 进行加密。
3.  **注册认证 (Register)**：
    *   调用 `POST /api/ec/dev/auth/regist`
    *   返回：Session Key (用于后续通讯)
4.  **业务调用**：
    *   后续所有请求头必须携带 `token` 和 `userid`。

### 2.2 数据获取流程 (Schema)
对于"报销单据同步"，我们通常不直接读数据库（不安全），而是调用标准工作流接口：
-   **获取列表**：`POST /api/workflow/paService/getWorkflowRequestList`
    -   筛选条件：`workflowId` (流程类型ID), `lastHandleDate` (归档日期范围)。
-   **获取详情**：`GET /api/workflow/paService/getWorkflowRequest`
    -   返回：包含主表字段 (MainTable) 和明细表数据 (DetailTable) 的完整 JSON。

---

## 3. 下一步行动计划 (To-Do List)

要让系统真正跑通，我们需要配合完成以下三步：

### Step A: 您需要提供的信息 (IT 侧)
请联系您的 OA 管理员提供：
1.  **OA 访问地址** (如 `http://oa.your-company.com:8080`)
2.  **APPID** (在 Ecology 后台->集成中心->注册并生成)
3.  **流程 ID** (例如"费用报销流程"的 ID 是 `45`)

### Step B: 填入配置
在我们的「集成中心」->「系统连接」中，点击「泛微OA」，将上述信息填入。

### Step C: 填充代码 (Dev 侧)
我将把 `WeaverAdapter.java` 中的 Mock 代码替换为真实逻辑。

**真实代码预览 (即将实现):**

```java
// 1. 获取 Token
String token = weaverClient.getToken(config.getAppUrl(), config.getAppId());

// 2. 只有拿到 Token 才能查数据
String flowData = HttpRequest.post(config.getAppUrl() + "/api/workflow/paService/getWorkflowRequestList")
    .header("token", token)
    .body(queryJson)
    .execute()
    .body();

// 3. 解析为凭证
for (WorkflowRequest req : parse(flowData)) {
    VoucherDTO voucher = new VoucherDTO();
    voucher.setSummary("报销-" + req.getRequestName());
    voucher.setAmount(req.getMainTable().getField("total_amount"));
    // ...
}
```

---

## 结论
即使我们现在还没填写真实 Key，但系统的**架构扩展性**已经验证完毕。
一旦您拿到真实的 APPID，我只需要 10 分钟填充代码，就能立刻实现从 OA 到档案系统的自动化归档。
