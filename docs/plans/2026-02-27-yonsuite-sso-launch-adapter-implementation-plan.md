# YonSuite SSO Launch Adapter Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 提供 YonSuite 专用的 SSO 发起接口，让 YonSuite 仅传业务字段即可获得 `launchUrl` 并完成联查跳转。

**Architecture:** 在现有 `/api/erp/sso/launch` 之上增加一层 YonSuite 适配器：新接口接收简化入参，服务端生成 `timestamp/nonce` 并用服务端密钥签名，再复用 `ErpSsoLaunchService.launch(...)` 产出票据。适配层只做“协议翻译与入口防护”，核心鉴权、映射、票据逻辑不复制。

**Tech Stack:** Spring Boot, Spring MVC, Lombok, JUnit5, Mockito, MockMvc, Maven。

---

### Task 1: 定义 YonSuite 专用 API 协议（DTO）

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/YonSuiteLaunchRequest.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/YonSuiteLaunchResponse.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/controller/YonSuiteSsoControllerTest.java`

**Step 1: Write the failing test**

在 `YonSuiteSsoControllerTest` 添加最小 case，调用新路径 `POST /integration/yonsuite/sso/launch`，断言请求只包含：
- `accbookCode`
- `erpUserJobNo`
- `voucherNo`

预期当前阶段失败（404 或 bean 不存在）。

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=YonSuiteSsoControllerTest test`
Expected: FAIL，提示路由不存在或控制器未实现。

**Step 3: Write minimal implementation**

创建 DTO：
- `YonSuiteLaunchRequest`
```java
@Data
public class YonSuiteLaunchRequest {
    @NotBlank private String accbookCode;
    @NotBlank private String erpUserJobNo;
    @NotBlank private String voucherNo;
}
```
- `YonSuiteLaunchResponse`
```java
@Data
@Builder
public class YonSuiteLaunchResponse {
    private String launchTicket;
    private Long expiresInSeconds;
    private String launchUrl;
}
```

**Step 4: Run test to verify compile baseline**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -DskipTests compile`
Expected: PASS。

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/YonSuiteLaunchRequest.java \
  nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/YonSuiteLaunchResponse.java \
  nexusarchive-java/src/test/java/com/nexusarchive/controller/YonSuiteSsoControllerTest.java
git commit -m "feat: add yonsuite sso launch dto contract"
```

### Task 2: 新增 YonSuite 适配服务（服务端补 timestamp/nonce/signature）

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/sso/YonSuiteSsoLaunchFacade.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/YonSuiteSsoLaunchFacadeImpl.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/service/sso/impl/YonSuiteSsoLaunchFacadeImplTest.java`

**Step 1: Write the failing test**

在 `YonSuiteSsoLaunchFacadeImplTest` 编写用例：
1. `should_build_signed_erp_launch_request_and_delegate()`
2. `should_reject_invalid_inbound_api_key()`

断言：
- facade 调用 `ErpSsoLaunchService.launch(clientId, signature, request)`
- request 中 `timestamp` 为 `Instant.now().getEpochSecond()` 附近值
- `nonce` 非空
- API Key 不合法时抛业务异常。

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=YonSuiteSsoLaunchFacadeImplTest test`
Expected: FAIL（类不存在）。

**Step 3: Write minimal implementation**

核心逻辑（伪代码必须按此实现）：
```java
long timestamp = Instant.now().getEpochSecond();
String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
ErpLaunchRequest req = new ErpLaunchRequest();
req.setAccbookCode(in.getAccbookCode());
req.setErpUserJobNo(in.getErpUserJobNo());
req.setVoucherNo(in.getVoucherNo());
req.setTimestamp(timestamp);
req.setNonce(nonce);
String payload = String.join("|", clientId, String.valueOf(timestamp), nonce,
    req.getAccbookCode(), req.getErpUserJobNo(), req.getVoucherNo());
String sig = erpSsoSignatureService.sign(payload, clientSecret);
return erpSsoLaunchService.launch(clientId, sig, req);
```

**Step 4: Run tests to verify pass**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=YonSuiteSsoLaunchFacadeImplTest test`
Expected: PASS。

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/sso/YonSuiteSsoLaunchFacade.java \
  nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/YonSuiteSsoLaunchFacadeImpl.java \
  nexusarchive-java/src/test/java/com/nexusarchive/service/sso/impl/YonSuiteSsoLaunchFacadeImplTest.java
git commit -m "feat: add yonsuite sso launch facade"
```

### Task 3: 暴露 YonSuite 专用 Controller 接口

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/controller/YonSuiteSsoController.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/config/SecurityConfig.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/controller/YonSuiteSsoControllerTest.java`

**Step 1: Write the failing test**

在 `YonSuiteSsoControllerTest` 里新增断言：
- 正常请求返回 `code=200` 且包含 `launchTicket/expiresInSeconds/launchUrl`
- 缺字段返回 `400`
- API Key 错误返回业务错误码（如 `INVALID_CLIENT_KEY`）

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=YonSuiteSsoControllerTest test`
Expected: FAIL。

**Step 3: Write minimal implementation**

控制器示例：
```java
@RestController
@RequestMapping("/integration/yonsuite/sso")
@RequiredArgsConstructor
public class YonSuiteSsoController {
  private final YonSuiteSsoLaunchFacade facade;

  @PostMapping("/launch")
  public Result<YonSuiteLaunchResponse> launch(
      @RequestHeader("X-Yon-Api-Key") String apiKey,
      @Valid @RequestBody YonSuiteLaunchRequest request) {
    return Result.success(facade.launch(apiKey, request));
  }
}
```

并在 `SecurityConfig` 的 `permitAll` 白名单加入：
- `/integration/yonsuite/sso/launch`

**Step 4: Run tests to verify pass**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=YonSuiteSsoControllerTest,ErpSsoControllerTest test`
Expected: PASS。

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/controller/YonSuiteSsoController.java \
  nexusarchive-java/src/main/java/com/nexusarchive/config/SecurityConfig.java \
  nexusarchive-java/src/test/java/com/nexusarchive/controller/YonSuiteSsoControllerTest.java
git commit -m "feat: expose yonsuite sso launch endpoint"
```

### Task 4: 配置项与安全约束落地

**Files:**
- Modify: `nexusarchive-java/src/main/resources/application.yml`
- Modify: `nexusarchive-java/src/main/resources/application-dev.yml`（如存在）
- Modify: `nexusarchive-java/src/main/resources/application-prod.yml`（如存在）
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/service/sso/impl/YonSuiteSsoLaunchFacadeImplTest.java`

**Step 1: Write the failing test**

为 facade 增加配置缺失用例：
- clientId/clientSecret/yonsuiteApiKey 任一为空时，抛启动前可识别异常（`IllegalStateException` 或业务异常）。

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=YonSuiteSsoLaunchFacadeImplTest test`
Expected: FAIL。

**Step 3: Write minimal implementation**

新增配置键：
- `app.sso.yonsuite.client-id`
- `app.sso.yonsuite.client-secret`
- `app.sso.yonsuite.inbound-api-key`

启动时或调用时校验非空；日志中禁止打印 secret 与完整 token。

**Step 4: Run tests to verify pass**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=YonSuiteSsoLaunchFacadeImplTest,ErpSsoSignatureServiceTest test`
Expected: PASS。

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/resources/application*.yml \
  nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/YonSuiteSsoLaunchFacadeImpl.java \
  nexusarchive-java/src/test/java/com/nexusarchive/service/sso/impl/YonSuiteSsoLaunchFacadeImplTest.java
git commit -m "chore: add yonsuite sso adapter config and guardrails"
```

### Task 5: 文档交付（给 YonSuite ERP 开发可直接调用）

**Files:**
- Create: `docs/api/yonsuite-sso-launch.md`
- Modify: `docs/api/README.md`
- Modify: `docs/CHANGELOG.md`

**Step 1: Write documentation test checklist (failing criteria)**

先写“文档验收点”，未满足即视为失败：
- 有完整请求示例（YonSuite 版）
- 有响应示例
- 有错误码清单
- 有 curl 联调命令
- 明确“ERP 前端只跳 launchUrl，不携带 token 到 URL”。

**Step 2: Run manual verification for checklist**

Run: 人工逐条检查文档。
Expected: 初始不满足（文档不存在）。

**Step 3: Write minimal documentation**

文档必须包含：
- 接口：`POST /api/integration/yonsuite/sso/launch`
- Header：`X-Yon-Api-Key`
- Body：`accbookCode/erpUserJobNo/voucherNo`
- 返回：`launchTicket/expiresInSeconds/launchUrl`
- 完整时序与常见错误排查。

**Step 4: Verify documentation completeness**

Run: `cd /Users/user/nexusarchive && rg -n "integration/yonsuite/sso/launch|X-Yon-Api-Key|launchUrl" docs/api/yonsuite-sso-launch.md docs/api/README.md`
Expected: 均能命中关键字。

**Step 5: Commit**

```bash
git add docs/api/yonsuite-sso-launch.md docs/api/README.md docs/CHANGELOG.md
git commit -m "docs: add yonsuite sso launch integration guide"
```

### Task 6: 端到端验证与联调脚本

**Files:**
- Modify: `docs/api/yonsuite-sso-launch.md`
- Optional Create: `scripts/sso/yonsuite_launch_smoke.sh`

**Step 1: Write failing verification step**

在本地先执行 smoke 命令，预期在未配置数据时失败（如 `CLIENT_NOT_FOUND` 或 `USER_MAPPING_NOT_FOUND`）。

**Step 2: Run command to verify fail-first**

Run（示例）：
```bash
curl -s -X POST 'http://localhost:19090/api/integration/yonsuite/sso/launch' \
  -H 'Content-Type: application/json' \
  -H 'X-Yon-Api-Key: REPLACE_ME' \
  -d '{"accbookCode":"BR01","erpUserJobNo":"1001","voucherNo":"记-8"}'
```
Expected: 初始失败（环境未就绪时）。

**Step 3: Prepare minimal env and rerun**

补齐：
- `erp_sso_client`
- `erp_user_mapping`
- `accbook -> fonds` 映射
- `app.sso.yonsuite.*` 配置

**Step 4: Run verification suite**

Run:
- `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=YonSuiteSsoControllerTest,YonSuiteSsoLaunchFacadeImplTest,ErpSsoControllerTest test`
- 再执行上面的 curl
Expected: 测试 PASS，curl 返回 `code=200` + `launchUrl`。

**Step 5: Commit**

```bash
git add docs/api/yonsuite-sso-launch.md scripts/sso/yonsuite_launch_smoke.sh
# 若无脚本则只 add 文档
git commit -m "test: add yonsuite sso launch smoke verification"
```

## Definition of Done

- YonSuite 可调用 `POST /api/integration/yonsuite/sso/launch` 获取 `launchUrl`。
- YonSuite 不需要实现 HMAC 签名，也不需要处理 ticket 消费逻辑。
- 复用现有 `erp/sso` 主链路，无重复实现。
- 单元/控制器测试通过，联调 curl 可返回成功票据。
- 文档可直接发给 ERP 开发执行。
