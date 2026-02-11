# ERP 发起联查单点登录（账套解析 + 用户映射）Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现 ERP 点击“发起联查”后，NexusArchive 基于账套编码与工号完成安全单点登录，并自动打开穿透联查页面且按凭证号自动查询。

**Architecture:** 采用“ERP 后端签名调用 + NexusArchive 一次性短期 launchTicket”双阶段模型。第一阶段由 ERP 服务端调用 `launch` 接口换取 ticket；第二阶段浏览器携带 ticket 访问前端落地页，前端再调用 `consume` 接口换取 JWT 与用户信息并写入本地登录态，最后跳转联查页自动查询。账套到全宗走现有 `accbookMapping` 解析，但切换为“严格唯一”；用户身份通过新增映射表维护，未映射即拒绝。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + Redis（nonce 防重）+ React + Zustand + React Router + Vitest。

---

### Task 1: 建立 SSO 基础数据模型（客户端、用户映射、一次性票据）

**Files:**
- Create: `nexusarchive-java/src/main/resources/db/migration/V108__create_erp_sso_tables.sql`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/ErpSsoClient.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/ErpUserMapping.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/ErpSsoLaunchTicket.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ErpSsoClientMapper.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ErpUserMappingMapper.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ErpSsoLaunchTicketMapper.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/service/sso/ErpSsoSchemaSmokeTest.java`

**Step 1: Write the failing test**

```java
@Test
void should_find_user_mapping_by_client_and_job_no() {
    ErpUserMapping mapping = erpUserMappingMapper.findActive("ERP_A", "JOB1001");
    assertNotNull(mapping);
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpSsoSchemaSmokeTest test`
Expected: FAIL（表或 Mapper 不存在）

**Step 3: Write minimal implementation**

```sql
CREATE TABLE erp_sso_client (... client_id VARCHAR(64) UNIQUE, client_secret VARCHAR(256), status VARCHAR(16)...);
CREATE TABLE erp_user_mapping (... client_id VARCHAR(64), erp_user_job_no VARCHAR(64), nexus_user_id VARCHAR(64), status VARCHAR(16)..., UNIQUE(client_id, erp_user_job_no));
CREATE TABLE erp_sso_launch_ticket (... ticket_id VARCHAR(64) PRIMARY KEY, used TINYINT, expires_at TIMESTAMP, voucher_no VARCHAR(128)...);
```

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpSsoSchemaSmokeTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/resources/db/migration/V108__create_erp_sso_tables.sql nexusarchive-java/src/main/java/com/nexusarchive/entity/ErpSsoClient.java nexusarchive-java/src/main/java/com/nexusarchive/entity/ErpUserMapping.java nexusarchive-java/src/main/java/com/nexusarchive/entity/ErpSsoLaunchTicket.java nexusarchive-java/src/main/java/com/nexusarchive/mapper/ErpSsoClientMapper.java nexusarchive-java/src/main/java/com/nexusarchive/mapper/ErpUserMappingMapper.java nexusarchive-java/src/main/java/com/nexusarchive/mapper/ErpSsoLaunchTicketMapper.java nexusarchive-java/src/test/java/com/nexusarchive/service/sso/ErpSsoSchemaSmokeTest.java
git commit -m "feat(sso): add erp sso base tables and mappers"
```

### Task 2: 实现账套严格唯一解析（ledgerCode -> fondsCode）

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/ErpConfigService.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ErpConfigServiceImpl.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/sso/AccbookResolutionResult.java`
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/service/ErpConfigServiceStrictAccbookTest.java`

**Step 1: Write the failing test**

```java
@Test
void should_fail_when_accbook_mapping_is_duplicated() {
    assertThrows(IllegalStateException.class, () -> service.resolveFondsCodeStrict("BR01"));
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpConfigServiceStrictAccbookTest test`
Expected: FAIL（方法不存在）

**Step 3: Write minimal implementation**

```java
public String resolveFondsCodeStrict(String accbookCode) {
    // 0 条 -> throw MAPPING_NOT_FOUND
    // >1 条 -> throw MAPPING_DUPLICATE
    // 1 条 -> return fondsCode
}
```

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpConfigServiceStrictAccbookTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/ErpConfigService.java nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ErpConfigServiceImpl.java nexusarchive-java/src/main/java/com/nexusarchive/service/sso/AccbookResolutionResult.java nexusarchive-java/src/test/java/com/nexusarchive/service/ErpConfigServiceStrictAccbookTest.java
git commit -m "feat(sso): add strict accbook to fonds resolution"
```

### Task 3: 实现签名鉴权与防重放（HMAC + timestamp + nonce）

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/ErpLaunchRequest.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/sso/ErpSsoSignatureService.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/ErpSsoSignatureServiceImpl.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/sso/NonceReplayGuard.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/RedisNonceReplayGuard.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/service/sso/ErpSsoSignatureServiceTest.java`

**Step 1: Write the failing test**

```java
@Test
void should_reject_replayed_nonce() {
    assertTrue(guard.tryAcquire("ERP_A", "n1", 60));
    assertFalse(guard.tryAcquire("ERP_A", "n1", 60));
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpSsoSignatureServiceTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

```java
boolean verify(String payload, String clientSecret, String signature);
void validateTimestamp(long ts, long now, long allowedSkewSeconds);
```

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpSsoSignatureServiceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/ErpLaunchRequest.java nexusarchive-java/src/main/java/com/nexusarchive/service/sso/ErpSsoSignatureService.java nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/ErpSsoSignatureServiceImpl.java nexusarchive-java/src/main/java/com/nexusarchive/service/sso/NonceReplayGuard.java nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/RedisNonceReplayGuard.java nexusarchive-java/src/test/java/com/nexusarchive/service/sso/ErpSsoSignatureServiceTest.java
git commit -m "feat(sso): add hmac signature and nonce replay guard"
```

### Task 4: 实现 ERP 发起联查接口（签发一次性 launchTicket）

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpSsoController.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/sso/ErpSsoLaunchService.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/ErpSsoLaunchServiceImpl.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/ErpLaunchResponse.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/config/SecurityConfig.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/controller/ErpSsoControllerLaunchTest.java`

**Step 1: Write the failing test**

```java
@Test
void launch_should_return_ticket_when_request_is_valid() throws Exception {
    mockMvc.perform(post("/erp/sso/launch") ...)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.launchTicket").exists());
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpSsoControllerLaunchTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

```java
@PostMapping("/erp/sso/launch")
public Result<ErpLaunchResponse> launch(@RequestBody ErpLaunchRequest req,
    @RequestHeader("X-Client-Id") String clientId,
    @RequestHeader("X-Signature") String signature) { ... }
```

返回字段至少包含：`launchTicket`, `expiresInSeconds(=60)`, `launchUrl`。

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpSsoControllerLaunchTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpSsoController.java nexusarchive-java/src/main/java/com/nexusarchive/service/sso/ErpSsoLaunchService.java nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/ErpSsoLaunchServiceImpl.java nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/ErpLaunchResponse.java nexusarchive-java/src/main/java/com/nexusarchive/config/SecurityConfig.java nexusarchive-java/src/test/java/com/nexusarchive/controller/ErpSsoControllerLaunchTest.java
git commit -m "feat(sso): add launch api for erp deep link"
```

### Task 5: 实现 ticket 消费接口（一次性换取 JWT + 用户信息）

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/AuthService.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/ConsumeTicketResponse.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpSsoController.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/ErpSsoLaunchServiceImpl.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/controller/ErpSsoControllerConsumeTest.java`

**Step 1: Write the failing test**

```java
@Test
void consume_should_fail_when_ticket_used_twice() throws Exception {
    // first consume -> 200
    // second consume -> 4xx with code TICKET_ALREADY_USED
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpSsoControllerConsumeTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

```java
public LoginResponse issueTokenByUserId(String userId) { ... } // AuthService
@PostMapping("/erp/sso/consume")
public Result<ConsumeTicketResponse> consume(@RequestParam String ticket) { ... }
```

消费后必须原子更新：`used=1`, `used_at=now`。

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpSsoControllerConsumeTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/AuthService.java nexusarchive-java/src/main/java/com/nexusarchive/dto/sso/ConsumeTicketResponse.java nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpSsoController.java nexusarchive-java/src/main/java/com/nexusarchive/service/sso/impl/ErpSsoLaunchServiceImpl.java nexusarchive-java/src/test/java/com/nexusarchive/controller/ErpSsoControllerConsumeTest.java
git commit -m "feat(sso): add consume api and one-time ticket exchange"
```

### Task 6: 前端新增 SSO 落地页并写入登录态

**Files:**
- Create: `src/pages/Auth/SsoLaunchPage.tsx`
- Modify: `src/routes/index.tsx`
- Create: `src/api/sso.ts`
- Modify: `src/store/useAuthStore.ts`
- Test: `src/pages/Auth/__tests__/SsoLaunchPage.test.tsx`

**Step 1: Write the failing test**

```tsx
it('consumes ticket and redirects to relationship page with voucherNo', async () => {
  // mock /erp/sso/consume success
  // assert useAuthStore.login called
  // assert navigate('/system/utilization/relationship?...')
});
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive && npm run test -- SsoLaunchPage.test.tsx`
Expected: FAIL

**Step 3: Write minimal implementation**

```tsx
// /system/sso/launch?ticket=...
// 1) call consume
// 2) login(token, user)
// 3) redirect to /system/utilization/relationship?voucherNo=...&autoSearch=1
```

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/nexusarchive && npm run test -- SsoLaunchPage.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add src/pages/Auth/SsoLaunchPage.tsx src/routes/index.tsx src/api/sso.ts src/store/useAuthStore.ts src/pages/Auth/__tests__/SsoLaunchPage.test.tsx
git commit -m "feat(frontend): add sso launch landing page"
```

### Task 7: 联查页支持 URL 自动填充并自动查询

**Files:**
- Modify: `src/pages/utilization/RelationshipQueryView.tsx`
- Test: `src/pages/utilization/__tests__/RelationshipQueryView.sso.test.tsx`

**Step 1: Write the failing test**

```tsx
it('auto searches when voucherNo and autoSearch=1 are provided', async () => {
  // render with route query
  // assert initializeGraph called with voucherNo
});
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive && npm run test -- RelationshipQueryView.sso.test.tsx`
Expected: FAIL

**Step 3: Write minimal implementation**

```tsx
useEffect(() => {
  const voucherNo = searchParams.get('voucherNo');
  const auto = searchParams.get('autoSearch') === '1';
  if (voucherNo) setSearchQuery(voucherNo);
  if (voucherNo && auto) void initializeGraph(voucherNo);
}, []);
```

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/nexusarchive && npm run test -- RelationshipQueryView.sso.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add src/pages/utilization/RelationshipQueryView.tsx src/pages/utilization/__tests__/RelationshipQueryView.sso.test.tsx
git commit -m "feat(utilization): support url-driven auto relationship query"
```

### Task 8: 联调验证、错误码与文档收口

**Files:**
- Modify: `docs/CHANGELOG.md`
- Modify: `docs/plans/README.md`
- Create: `docs/api/erp-sso-launch.md`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/config/OpenApiConfig.java`（如需补充分组说明）
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/controller/ErpSsoErrorCodeContractTest.java`

**Step 1: Write the failing test**

```java
@Test
void should_return_user_mapping_not_found_code() { ... }
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpSsoErrorCodeContractTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

统一错误码：
- `INVALID_SIGNATURE`
- `TIMESTAMP_EXPIRED`
- `NONCE_REPLAYED`
- `ACCBOOK_MAPPING_NOT_FOUND`
- `ACCBOOK_MAPPING_DUPLICATE`
- `USER_MAPPING_NOT_FOUND`
- `TICKET_NOT_FOUND`
- `TICKET_EXPIRED`
- `TICKET_ALREADY_USED`

**Step 4: Run test to verify it passes**

Run: `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest=ErpSsoErrorCodeContractTest test`
Expected: PASS

**Step 5: Full verification**

Run:
- `cd /Users/user/nexusarchive/nexusarchive-java && mvn -q -Dtest="ErpSso*Test,ErpConfigServiceStrictAccbookTest" test`
- `cd /Users/user/nexusarchive && npm run test -- SsoLaunchPage.test.tsx RelationshipQueryView.sso.test.tsx`

Expected: 全部 PASS

**Step 6: Commit**

```bash
git add docs/CHANGELOG.md docs/plans/README.md docs/api/erp-sso-launch.md nexusarchive-java/src/main/java/com/nexusarchive/config/OpenApiConfig.java nexusarchive-java/src/test/java/com/nexusarchive/controller/ErpSsoErrorCodeContractTest.java
git commit -m "docs(sso): add erp sso launch api contract and error codes"
```

## 联调清单（上线前）

1. 准备 `erp_sso_client`：为 ERP 分配 `clientId/clientSecret`，状态置 `ACTIVE`。
2. 准备 `erp_user_mapping`：按工号导入映射，验证唯一约束。
3. 校验 `accbookMapping`：确认账套全局唯一，出现重复必须先清理。
4. ERP 后端按签名规范调用 `/erp/sso/launch`，浏览器跳转 `launchUrl`。
5. 观察审计日志：至少记录 clientId、erpUserJobNo、nexusUserId、accbookCode、fondsCode、voucherNo、ticketId、结果码。

## 非目标（本期不做）

1. 用户映射管理前端页面（仅支持后端导入/脚本维护）。
2. 多候选账套回传 ERP 选择（当前策略是严格唯一，歧义即报错）。
3. 跨系统统一身份中心（OIDC/SAML）替代方案。
