# YunDun OIDC Callback JWT Bridge Plan

**目标**：完成“云盾 OIDC 回调 `code` -> 本系统 JWT 登录态”的第二阶段闭环，并移除 `systemPath` 依赖。

## 范围

1. 新增云盾 OIDC 配置项（开关、clientId、token/userInfo 端点、用户标识字段）。
2. 新增桥接服务：code 换 token、拉取 userInfo、按 `erp_user_mapping` 映射本地用户、签发 JWT。
3. 新增回调接口：`GET /integration/yundun/oidc/callback`。
4. 安全放行该回调路径（permitAll）。
5. 新增单测（服务层 + 控制器）。
6. `app-sec-sso` 从 `systemPath` 改为私服普通依赖。

## 关键设计

1. **本地登录态统一由 AuthService 签发**  
   复用 `AuthService.issueTokenByUserId`，避免额外 JWT 分支。
2. **映射复用现有 ERP SSO 表**  
   用 `erp_sso_client` 读取 `client_secret`；用 `erp_user_mapping(client_id, erp_user_job_no)` 做身份桥接。
3. **错误语义统一**  
   新增 `YUNDUN_OIDC_*` 错误码，控制器统一返回 `Result.error(code, "ERROR: message")`。
4. **可移植构建**  
   `pom.xml` 启用 `nexus-private` 仓库并改普通依赖，去除本地文件路径耦合。

## 验证

1. `mvn -Dtest=YundunOidcBridgeServiceImplTest,YundunOidcControllerTest test`
2. `mvn -Dtest=YundunTokenServiceImplTest,YundunSdkControllerTest test`

## 风险

1. 云盾 `userInfo` 字段差异：通过 `app.yundun.oidc.user-id-field` 可配置化规避。
2. 私服制品尚未上传时，构建会失败：需先把 `app-sec-sso-1.0.0.jar` 发布到私服对应坐标。
