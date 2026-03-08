# YunDun SDK Bootstrap Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在现有 `nexusarchive-java` 中打通云盾 SDK 的首阶段能力：安全加载配置并通过 SDK 获取 `appToken`，提供受控调试接口用于联调验证。

**Architecture:** 新增 `integration/yundun` 模块，采用“SDK Facade + Service + Controller”三层。Facade 隔离静态 SDK 调用，Service 负责配置校验与错误语义转换，Controller 只暴露最小联调入口。该阶段不改动现有 ERP SSO 主链路，仅为后续 OIDC/JWT 桥接和用户组织同步提供令牌基础。

**Tech Stack:** Spring Boot 3.1、Java 17、Maven、JUnit5、Mockito、云盾 `app-sec-sso-1.0.0.jar`

---

### Task 1: 引入 SDK 依赖与配置骨架

**Files:**
- Modify: `nexusarchive-java/pom.xml`
- Modify: `nexusarchive-java/src/main/resources/application.yml`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yundun/config/YundunSdkProperties.java`

**Step 1: 先写配置绑定测试（失败）**

```java
// YundunSdkPropertiesTest
// 断言 prefix=app.yundun.sdk 能绑定 privateKey、idpBaseUrl、enabled
```

**Step 2: 运行单测验证失败**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -Dtest=YundunSdkPropertiesTest test`
Expected: FAIL（类或字段不存在）

**Step 3: 实现最小配置类与默认值**

```java
@ConfigurationProperties(prefix = "app.yundun.sdk")
public class YundunSdkProperties {
  private boolean enabled;
  private String privateKey;
  private String idpBaseUrl;
}
```

**Step 4: 在 `application.yml` 增加配置模板**

- `app.yundun.sdk.enabled`
- `app.yundun.sdk.private-key`
- `app.yundun.sdk.idp-base-url`

**Step 5: 引入 SDK JAR 依赖**

- 在 `pom.xml` 增加 system scope（路径指向 `../java-sdk-test/lib/app-sec-sso-1.0.0.jar`）。

**Step 6: 运行测试确保通过**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -Dtest=YundunSdkPropertiesTest test`
Expected: PASS

---

### Task 2: 实现 SDK Token 获取服务（TDD）

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yundun/sdk/YundunSdkFacade.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yundun/sdk/DefaultYundunSdkFacade.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yundun/service/YundunTokenService.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yundun/service/impl/YundunTokenServiceImpl.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/yundun/service/YundunTokenServiceImplTest.java`

**Step 1: 写失败测试（成功、配置缺失、SDK失败三条）**

```java
// should_return_token_when_sdk_success
// should_throw_when_private_key_missing
// should_throw_when_sdk_returns_fail
```

**Step 2: 运行测试确认失败**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -Dtest=YundunTokenServiceImplTest test`
Expected: FAIL（实现不存在）

**Step 3: 实现 Facade 包装静态 SDK**

- `setConfigPriKey(privateKey, idpBaseUrl)`
- `applyAppToken()`

**Step 4: 实现 Service**

- 校验 `enabled/privateKey`；
- 调用 Facade 获取 `InvokeResult`；
- 提取 token（`code==0` 且 content 为字符串）；
- 失败时抛出统一业务异常（不泄漏敏感信息）。

**Step 5: 运行测试验证通过**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -Dtest=YundunTokenServiceImplTest test`
Expected: PASS

---

### Task 3: 暴露受控联调接口

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/YundunAppTokenResponse.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/controller/YundunSdkController.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/config/SecurityConfig.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/controller/YundunSdkControllerTest.java`

**Step 1: 写控制器测试（Mock Service）**

```java
// should_return_app_token
// should_return_error_when_service_throws
```

**Step 2: 运行失败测试**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -Dtest=YundunSdkControllerTest test`
Expected: FAIL

**Step 3: 实现控制器**

- 路径：`POST /integration/yundun/sdk/token`
- 响应：`token` + `provider` + `issuedAt`

**Step 4: 更新安全配置**

- 仅在需要无登录联调时 `permitAll`；否则保持鉴权（推荐保持鉴权）。

**Step 5: 运行测试通过**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -Dtest=YundunSdkControllerTest test`
Expected: PASS

---

### Task 4: 文档与回归验证

**Files:**
- Create: `docs/api/yundun-sdk-token.md`
- Modify: `docs/api/README.md`
- Optional Create: `scripts/sso/yundun_sdk_token_smoke.sh`

**Step 1: 补联调文档**

- 环境变量、接口示例、错误码、脱敏日志规范。

**Step 2: 运行目标测试集**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -Dtest=YundunSdkPropertiesTest,YundunTokenServiceImplTest,YundunSdkControllerTest,ErpSsoControllerTest,YonSuiteSsoControllerTest test`
Expected: PASS

**Step 3: 本地 smoke（可选）**

- 用 curl 调 `POST /api/integration/yundun/sdk/token` 验证返回结构。

**Step 4: 提交变更**

```bash
git add nexusarchive-java/pom.xml \
  nexusarchive-java/src/main/resources/application.yml \
  nexusarchive-java/src/main/java/com/nexusarchive/integration/yundun \
  nexusarchive-java/src/main/java/com/nexusarchive/controller/YundunSdkController.java \
  nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/YundunAppTokenResponse.java \
  nexusarchive-java/src/main/java/com/nexusarchive/config/SecurityConfig.java \
  nexusarchive-java/src/test/java/com/nexusarchive/integration/yundun \
  nexusarchive-java/src/test/java/com/nexusarchive/controller/YundunSdkControllerTest.java \
  docs/api/yundun-sdk-token.md docs/api/README.md

git commit -m "feat: bootstrap yundun sdk app-token integration"
```

---

## 风险与约束

1. SDK 依赖 `config.properties`（位于 JAR 内）+ 私钥解密，若私钥与加密配置不匹配将失败。
2. `systemPath` 依赖可移植性差，后续建议转私服制品仓库。
3. 返回 `appToken` 接口必须受控，避免外泄。
4. 当前阶段仅打通令牌获取，不覆盖 OIDC 登录回调与单点登出。
