# 云盾 SDK AppToken 接口

> 适用场景：验证云盾（TAM）`app-sec-sso` SDK 是否可在当前系统内成功取到 `appToken`。

## 1. 接口定义

- Method: `POST`
- URL: `/api/integration/yundun/sdk/token`
- 认证：`Bearer <jwt>`（需要系统登录态）

成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "<appToken>",
    "provider": "YUNDUN_SDK",
    "issuedAt": 1772614617
  }
}
```

失败响应示例：

```json
{
  "code": 502,
  "message": "YUNDUN_SDK_TOKEN_FETCH_FAILED: 获取云盾 appToken 失败: Apply token fail",
  "data": null
}
```

## 2. 配置项

在 `application.yml` 中对应：

```yaml
app:
  yundun:
    sdk:
      enabled: ${APP_YUNDUN_SDK_ENABLED:false}
      private-key: ${APP_YUNDUN_SDK_PRIVATE_KEY:}
      idp-base-url: ${APP_YUNDUN_SDK_IDP_BASE_URL:}
```

说明：

1. `enabled=false` 时接口直接返回 `YUNDUN_SDK_DISABLED`。
2. `private-key` 必填（由运维注入），未配置返回 `YUNDUN_SDK_CONFIG_INVALID`。
3. `idp-base-url` 可选；配置后会覆盖 SDK 内 `config.properties` 里的域名前缀。

## 3. 错误码

| 错误码 | 含义 |
| --- | --- |
| `YUNDUN_SDK_DISABLED` | 云盾 SDK 集成未启用 |
| `YUNDUN_SDK_CONFIG_INVALID` | 云盾 SDK 配置不完整 |
| `YUNDUN_SDK_TOKEN_FETCH_FAILED` | 调用 SDK 获取 token 失败 |

## 4. 联调建议

1. 先在测试环境开启 `APP_YUNDUN_SDK_ENABLED=true`。
2. 注入私钥后调用接口，确认返回 `provider=YUNDUN_SDK`。
3. 不要在日志或截图中明文暴露 `token`。
