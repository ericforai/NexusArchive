# YonSuite 一跳式 SSO 联查对接文档

> 适用场景：YonSuite 用户在 ERP 中点击“联查电子会计档案”，直接带参数访问 NexusArchive 页面并自动登录后进入联查页。

## 1. 总体流程（一跳 + 安全落地）

1. YonSuite 前端构造带签名参数的 URL，直接跳转 `GET /system/sso/launch?...`。
2. NexusArchive 的 `SsoLaunchPage` 读取 URL 参数并调用后端 `POST /api/integration/yonsuite/sso/consume`。
3. 后端校验签名、时间戳、nonce、防重放、账套映射、用户映射。
4. 校验通过后返回 `token + user + voucherNo`。
5. 前端写入登录态并跳转 `/system/utilization/relationship?voucherNo=xxx&autoSearch=1`。

> 说明：URL 参数里放的是一次性 SSO 凭据（签名参数），不是业务 JWT Token。

## 2. YonSuite 跳转 URL 规范

- Method: `GET`
- URL: `/system/sso/launch`
- Query 参数：
  - `clientId`：SSO 客户端标识
  - `requestId`：本次请求唯一流水号（建议 UUID）
  - `accbookCode`：账套编码
  - `erpUserJobNo`：ERP 工号
  - `voucherNo`：凭证号
  - `timestamp`：Unix 秒级时间戳
  - `nonce`：随机串（每次唯一）
  - `signature`：签名值（HMAC-SHA256 + Base64）

示例：

```text
https://www.digivoucher.cn/system/sso/launch?clientId=ERP_DIGIVOUCHER_TEST&requestId=2f6e6018-7c16-4bf3-8e5c-1b0d7f2d2e91&accbookCode=BR01&erpUserJobNo=1001&voucherNo=%E8%AE%B0-8&timestamp=1739230000&nonce=a6a4f0e5d2&signature=R8L2...=
```

## 3. 后端消费接口（前端内部调用）

### 3.1 消费 URL 参数并换登录态

- Method: `POST`
- URL: `/api/integration/yonsuite/sso/consume`
- Body:

```json
{
  "clientId": "ERP_DIGIVOUCHER_TEST",
  "requestId": "2f6e6018-7c16-4bf3-8e5c-1b0d7f2d2e91",
  "accbookCode": "BR01",
  "erpUserJobNo": "1001",
  "voucherNo": "记-8",
  "timestamp": 1739230000,
  "nonce": "a6a4f0e5d2",
  "signature": "R8L2...="
}
```

成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "<jwt>",
    "user": {
      "id": "u1",
      "username": "zhangsan",
      "roles": ["archive_user"],
      "permissions": ["archive:view"]
    },
    "voucherNo": "记-8"
  }
}
```

## 4. 签名规则

- 算法：`HMAC-SHA256`
- 编码：`Base64`
- 原文拼接（固定顺序，分隔符 `|`）：

`clientId|requestId|timestamp|nonce|accbookCode|erpUserJobNo|voucherNo`

示例原文：

`ERP_DIGIVOUCHER_TEST|2f6e6018-7c16-4bf3-8e5c-1b0d7f2d2e91|1739230000|a6a4f0e5d2|BR01|1001|记-8`

## 5. 服务端校验规则

1. 签名校验必须通过。
2. 时间戳窗口：默认 `±300s`。
3. nonce 防重放：同一 `clientId + nonce` 仅允许一次。
4. `accbookCode -> fondsCode` 必须严格唯一。
5. `erpUserJobNo -> nexusUserId` 映射必须存在。

## 6. 常见错误码

| 错误码 | 含义 | 建议处理 |
| --- | --- | --- |
| `INVALID_SIGNATURE` | 签名无效 | 检查拼接顺序、编码、密钥 |
| `TIMESTAMP_EXPIRED` | 时间戳过期/漂移过大 | 校准 YonSuite 服务器时间 |
| `NONCE_REPLAYED` | 请求重放 | 每次生成新 nonce 和 requestId |
| `CLIENT_NOT_FOUND` | clientId 不存在或禁用 | 检查客户端配置 |
| `USER_MAPPING_NOT_FOUND` | ERP 工号未映射 | 补齐用户映射 |
| `ACCBOOK_MAPPING_NOT_FOUND` | 账套映射缺失 | 补齐账套-全宗映射 |
| `ACCBOOK_MAPPING_DUPLICATE` | 账套映射不唯一 | 清理重复映射 |

## 7. Java 签名示例（YonSuite 可直接参考）

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class YonSsoSignDemo {
    public static String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] out = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(out);
    }
}
```

## 8. 联调检查清单

1. 已配置 `clientId/clientSecret` 且状态 `ACTIVE`。
2. 已配置 `erpUserJobNo -> nexusUserId` 映射。
3. 已配置 `accbookCode -> fondsCode` 映射且严格唯一。
4. YonSuite 跳转 URL 参数完整且签名正确。
5. 浏览器最终落在联查页并自动发起凭证号查询。

## 9. 安全边界（必须遵守）

1. 禁止在 URL 中传递 JWT token。
2. 禁止在前端暴露 `clientSecret`。
3. 所有 SSO 参数必须服务端复核，不能仅前端信任。
