# YonSuite 两接口式 SSO 联查对接文档

> 适用场景：YonSuite 只能接受“先取单点 token，再取跳转 URL”两步接口调用模式。

## 1. 总体流程

1. YonSuite 后端调用接口 1，按 `appId + loginId` 获取一次性 `ssoToken`。
2. YonSuite 后端调用接口 2，携带 `requestId + ssoToken + 凭证信息` 获取 `urlPath`。
3. YonSuite 前端直接打开 `urlPath`（例如 `/system/sso/launch?ticket=...`）。
4. NexusArchive 前端落地页消费 ticket，写入登录态并跳转联查页。

## 2. 接口 1：获取单点登录 token

- Method: `POST`
- URL: `/api/integration/yonsuite/sso/token`

### 2.1 请求体

```json
{
  "appId": "ERP_DIGIVOUCHER_TEST",
  "loginId": "1001"
}
```

### 2.2 成功响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "requestId": "2f6e6018-7c16-4bf3-8e5c-1b0d7f2d2e91",
    "ssoToken": "5a9f...",
    "expiresInSeconds": 60
  }
}
```

### 2.3 说明

- `requestId` 与 `ssoToken` 成对使用。
- `ssoToken` 为一次性短时令牌（默认 60 秒）。

## 3. 接口 2：获取单点登录跳转 URL

- Method: `POST`
- URL: `/api/integration/yonsuite/sso/url`

### 3.1 请求体

```json
{
  "requestId": "2f6e6018-7c16-4bf3-8e5c-1b0d7f2d2e91",
  "ssoToken": "5a9f...",
  "accbookCode": "BR01",
  "voucherNo": "记-8"
}
```

### 3.2 成功响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "urlPath": "/system/sso/launch?ticket=3a6f7d..."
  }
}
```

### 3.3 说明

- 该接口内部会完成 ERP SSO 发起签名与 ticket 生成。
- YonSuite 只需打开 `urlPath`，不需要自己拼签名串。

## 4. 错误码

| 错误码 | 含义 | 建议处理 |
| --- | --- | --- |
| `CLIENT_NOT_FOUND` | appId 对应客户端不存在或未启用 | 检查 appId 配置与状态 |
| `SSO_TOKEN_INVALID` | ssoToken 无效 | 重新调用接口 1 获取新 token |
| `SSO_TOKEN_EXPIRED` | ssoToken 已过期 | 重新调用接口 1 获取新 token |
| `SSO_TOKEN_ALREADY_USED` | ssoToken 已使用 | 重新调用接口 1 获取新 token |
| `USER_MAPPING_NOT_FOUND` | loginId 未映射 Nexus 用户 | 补齐用户映射 |
| `ACCBOOK_MAPPING_NOT_FOUND` | 账套映射缺失 | 补齐账套映射 |
| `ACCBOOK_MAPPING_DUPLICATE` | 账套映射不唯一 | 清理重复映射 |

## 5. YonSuite 对接示例（伪代码）

```java
// 接口一：获取单点 token
TokenResp t = post("/api/integration/yonsuite/sso/token", {
  "appId": "ERP_DIGIVOUCHER_TEST",
  "loginId": "1001"
});

// 接口二：获取跳转 URL
UrlResp u = post("/api/integration/yonsuite/sso/url", {
  "requestId": t.data.requestId,
  "ssoToken": t.data.ssoToken,
  "accbookCode": "BR01",
  "voucherNo": "记-8"
});

// 前端跳转
redirect(u.data.urlPath);
```

## 6. 联调前置

1. `erp_sso_client` 已配置 `client_id = appId` 且状态 `ACTIVE`。
2. `erp_user_mapping` 已配置 `clientId + loginId -> nexusUserId`。
3. `accbookCode -> fondsCode` 映射已配置且严格唯一。

## 7. 安全边界

1. `ssoToken` 一次性、短时有效。
2. 不在 URL 中传递业务 JWT token。
3. 所有登录态由 NexusArchive 后端签发。
