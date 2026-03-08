# 云盾 OIDC 回调桥接接口

> 适用场景：云盾完成认证后回调业务系统，业务系统使用 `code` 换取云盾用户信息并签发 NexusArchive JWT。

## 1. 接口定义

- Method: `GET`
- URL: `/api/integration/yundun/oidc/callback`
- 认证：无需 JWT（回调入口）

请求参数：

| 参数 | 位置 | 必填 | 说明 |
| --- | --- | --- | --- |
| `code` | query | 是 | 云盾回调授权码 |
| `state` | query | 否 | 云盾透传状态值 |

成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "<nexus-jwt>",
    "user": {
      "id": "u-1",
      "username": "zhangsan"
    },
    "provider": "YUNDUN_OIDC",
    "externalUserId": "job1001"
  }
}
```

失败响应示例：

```json
{
  "code": 502,
  "message": "YUNDUN_OIDC_TOKEN_EXCHANGE_FAILED: invalid code",
  "data": null
}
```

## 2. 配置项

```yaml
app:
  yundun:
    oidc:
      enabled: ${APP_YUNDUN_OIDC_ENABLED:false}
      client-id: ${APP_YUNDUN_OIDC_CLIENT_ID:}
      access-token-url: ${APP_YUNDUN_OIDC_ACCESS_TOKEN_URL:}
      user-info-url: ${APP_YUNDUN_OIDC_USER_INFO_URL:}
      redirect-uri: ${APP_YUNDUN_OIDC_REDIRECT_URI:}
      access-token-field: ${APP_YUNDUN_OIDC_ACCESS_TOKEN_FIELD:access_token}
      user-id-field: ${APP_YUNDUN_OIDC_USER_ID_FIELD:sub}
      authorization-prefix: ${APP_YUNDUN_OIDC_AUTHORIZATION_PREFIX:}
```

说明：

1. `client-id` 必须在 `erp_sso_client` 表中存在且状态为 `ACTIVE`。
2. `user-id-field` 会映射到 `erp_user_mapping.erp_user_job_no` 进行本地用户匹配。
3. `authorization-prefix` 默认空字符串；若云盾侧要求 `Bearer ` 可配置。

## 3. 错误码

| 错误码 | 含义 |
| --- | --- |
| `YUNDUN_OIDC_DISABLED` | 云盾 OIDC 集成未启用 |
| `YUNDUN_OIDC_CONFIG_INVALID` | 云盾 OIDC 配置不完整 |
| `YUNDUN_OIDC_TOKEN_EXCHANGE_FAILED` | `code` 换 token 失败 |
| `YUNDUN_OIDC_USERINFO_FETCH_FAILED` | 获取 userInfo 失败 |
| `YUNDUN_OIDC_USERINFO_INVALID` | userInfo 内容不合法 |
| `USER_MAPPING_NOT_FOUND` | 云盾用户未映射本地用户 |
