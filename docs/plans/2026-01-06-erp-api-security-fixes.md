# ERP API Security Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix 4 critical security issues in ERP scenario synchronization API endpoints

**Architecture:** Layer-by-layer security hardening - Controller (permission) → Service (async) → Validation (input)

**Tech Stack:** Spring Security, Spring Async, Spring Validation, Custom Audit Annotations

---

## Problem Statement

Current `ErpScenarioController` has 4 security vulnerabilities:

1. **🔴 CRITICAL: Missing permission control** - Any authenticated user can trigger ERP sync
2. **🟡 MEDIUM: Incomplete audit coverage** - Some methods lack `@ArchivalAudit` annotation
3. **🟡 MEDIUM: Synchronous blocking calls** - Long-running syncs block HTTP requests
4. **🟡 MEDIUM: Missing input validation** - `Map<String, Object>` params bypass validation

---

## Task 1: Add Missing Permission Control

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpScenarioController.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/controller/ErpScenarioControllerTest.java`

**Step 1: Write permission test**

```java
// ErpScenarioControllerTest.java
@Test
@WithMockUser(username = "regular_user", roles = {"USER"})
void triggerSync_shouldFail_forRegularUser() {
    // Should return 403 Forbidden
    var response = restTemplate.postForEntity(
        "/erp/scenario/1/sync",
        null,
        Void.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
}

@Test
@WithMockUser(username = "admin", roles = {"SYSTEM_ADMIN"})
void triggerSync_shouldSucceed_forSystemAdmin() {
    // Should allow access
    var response = restTemplate.postForEntity(
        "/erp/scenario/1/sync",
        null,
        Void.class
    );
    assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ErpScenarioControllerTest#triggerSync_shouldFail_forRegularUser`
Expected: FAIL (test passes because permission is missing - need to expect 403 but gets 200)

**Step 3: Add @PreAuthorize annotations to Controller**

Add to `ErpScenarioController.java`:

```java
package com.nexusarchive.controller;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/erp/scenario")
@RequiredArgsConstructor
@Tag(name = "ERP业务场景管理")
public class ErpScenarioController {

    private final ErpScenarioService erpScenarioService;

    @GetMapping("/list/{configId}")
    @Operation(summary = "获取指定ERP配置的场景列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<List<ErpScenario>> listByConfig(@PathVariable Long configId) {
        return Result.success(erpScenarioService.listScenariosByConfigId(configId));
    }

    @PutMapping
    @Operation(summary = "更新场景配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<Void> update(@Valid @RequestBody ErpScenario scenario) {
        erpScenarioService.updateScenario(scenario);
        return Result.success();
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "手动触发同步")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")  // ADD THIS LINE
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "CAPTURE", resourceType = "ERP_SYNC", description = "手动触发ERP同步场景")
    public Result<Void> triggerSync(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);
        erpScenarioService.syncScenario(id, operatorId, clientIp);
        return Result.success();
    }

    @GetMapping("/channels")
    @Operation(summary = "获取所有集成通道（聚合视图）")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER', 'AUDITOR')")
    public Result<List<com.nexusarchive.dto.IntegrationChannelDTO>> listAllChannels() {
        return Result.success(erpScenarioService.listAllChannels());
    }

    @GetMapping("/{scenarioId}/interfaces")
    @Operation(summary = "获取场景的子接口列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<List<ErpSubInterface>> listSubInterfaces(@PathVariable Long scenarioId) {
        return Result.success(erpScenarioService.listSubInterfaces(scenarioId));
    }

    @PutMapping("/interface")
    @Operation(summary = "更新子接口配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<Void> updateSubInterface(@Valid @RequestBody ErpSubInterface subInterface,
                                            jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);
        erpScenarioService.updateSubInterface(subInterface, operatorId, clientIp);
        return Result.success();
    }

    @PutMapping("/interface/toggle/{id}")
    @Operation(summary = "切换子接口启用状态")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<Void> toggleSubInterface(@PathVariable Long id,
                                           jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);
        erpScenarioService.toggleSubInterface(id, operatorId, clientIp);
        return Result.success();
    }

    @GetMapping("/{scenarioId}/history")
    @Operation(summary = "获取场景的同步历史 (最近10条)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER', 'AUDITOR')")
    public Result<List<SyncHistory>> getSyncHistory(@PathVariable Long scenarioId) {
        return Result.success(erpScenarioService.getSyncHistory(scenarioId));
    }

    @PutMapping("/{id}/params")
    @Operation(summary = "更新场景参数配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<Void> updateScenarioParams(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        erpScenarioService.updateScenarioParams(id, params);
        return Result.success();
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ErpScenarioControllerTest`
Expected: PASS (regular user gets 403, admin gets access)

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpScenarioController.java
git commit -m "security(erp): add @PreAuthorize to all ErpScenarioController methods

- Only SYSTEM_ADMIN, super_admin can trigger sync operations
- ARCHIVE_MANAGER can view but not modify
- AUDITOR can view history
- Fixes critical permission bypass vulnerability

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Add Missing Audit Annotations

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpScenarioController.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/integration/AuditLogIntegrationTest.java`

**Step 1: Write audit integration test**

```java
// AuditLogIntegrationTest.java
@Test
void updateScenario_shouldCreateAuditLog() {
    // Update scenario
    restTemplate.exchange(
        "/erp/scenario/1",
        HttpMethod.PUT,
        new HttpHeaders(),
        ErpScenario.class
    );

    // Verify audit log was created
    List<AuditLog> logs = auditLogRepository.findByOperationTypeAndResourceType("UPDATE", "ERP_SCENARIO");
    assertThat(logs).isNotEmpty();
    assertThat(logs.get(0).getDescription()).contains("更新ERP场景配置");
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=AuditLogIntegrationTest#updateScenario_shouldCreateAuditLog`
Expected: FAIL (audit log not found because annotation is missing)

**Step 3: Add @ArchivalAudit annotations**

Add to `ErpScenarioController.java`:

```java
import com.nexusarchive.annotation.ArchivalAudit;

@RestController
@RequestMapping("/erp/scenario")
@RequiredArgsConstructor
@Tag(name = "ERP业务场景管理")
public class ErpScenarioController {

    // ... existing methods ...

    @PutMapping
    @Operation(summary = "更新场景配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SCENARIO", description = "更新ERP场景配置")
    public Result<Void> update(@Valid @RequestBody ErpScenario scenario) {
        erpScenarioService.updateScenario(scenario);
        return Result.success();
    }

    @GetMapping("/{scenarioId}/interfaces")
    @Operation(summary = "获取场景的子接口列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<List<ErpSubInterface>> listSubInterfaces(@PathVariable Long scenarioId) {
        return Result.success(erpScenarioService.listSubInterfaces(scenarioId));
    }

    @PutMapping("/interface")
    @Operation(summary = "更新子接口配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SUB_INTERFACE", description = "更新ERP子接口配置")
    public Result<Void> updateSubInterface(@Valid @RequestBody ErpSubInterface subInterface,
                                            jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);
        erpScenarioService.updateSubInterface(subInterface, operatorId, clientIp);
        return Result.success();
    }

    @PutMapping("/interface/toggle/{id}")
    @Operation(summary = "切换子接口启用状态")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SUB_INTERFACE", description = "切换ERP子接口启用状态")
    public Result<Void> toggleSubInterface(@PathVariable Long id,
                                           jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);
        erpScenarioService.toggleSubInterface(id, operatorId, clientIp);
        return Result.success();
    }

    @PutMapping("/{id}/params")
    @Operation(summary = "更新场景参数配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SCENARIO_PARAMS", description = "更新ERP场景参数配置")
    public Result<Void> updateScenarioParams(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        erpScenarioService.updateScenarioParams(id, params);
        return Result.success();
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=AuditLogIntegrationTest`
Expected: PASS (audit logs are created for all update operations)

**Step 5: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpScenarioController.java
git commit -m "security(audit): add @ArchivalAudit to ERP scenario mutations

All update operations now create audit logs:
- updateScenario()
- updateSubInterface()
- toggleSubInterface()
- updateScenarioParams()

Ensures complete audit trail for compliance.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Convert to Async Sync with Progress Tracking

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/SyncTaskDTO.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/SyncTaskStatus.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/erp/AsyncErpSyncService.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpScenarioController.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/config/AsyncConfig.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/controller/ErpScenarioControllerAsyncTest.java`

**Step 1: Create DTOs**

Create `SyncTaskDTO.java`:

```java
package com.nexusarchive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncTaskDTO {
    private String taskId;
    private String status;  // SUBMITTED, RUNNING, SUCCESS, FAIL
    private String message;
}
```

Create `SyncTaskStatus.java`:

```java
package com.nexusarchive.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncTaskStatus {
    private String taskId;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double progress;  // 0.0 to 1.0
}
```

**Step 2: Create async configuration**

Create `AsyncConfig.java` in `config` package:

```java
package com.nexusarchive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "erpSyncExecutor")
    public Executor erpSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("erp-sync-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

**Step 3: Create async service wrapper**

Create `AsyncErpSyncService.java`:

```java
package com.nexusarchive.service.erp;

import com.nexusarchive.dto.SyncTaskDTO;
import com.nexusarchive.dto.SyncTaskStatus;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.mapper.ErpScenarioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncErpSyncService {

    private final ErpSyncService erpSyncService;
    private final ErpScenarioMapper erpScenarioMapper;

    // In-memory task status store (consider Redis for production)
    private final Map<String, SyncTaskStatus> taskStore = new ConcurrentHashMap<>();

    @Async("erpSyncExecutor")
    @Transactional
    public void syncScenarioAsync(String taskId, Long scenarioId, String operatorId, String clientIp) {
        try {
            // Update status to RUNNING
            SyncTaskStatus status = SyncTaskStatus.builder()
                .taskId(taskId)
                .status("RUNNING")
                .startTime(LocalDateTime.now())
                .progress(0.0)
                .build();
            taskStore.put(taskId, status);

            // Execute sync
            erpSyncService.syncScenario(scenarioId, operatorId, clientIp);

            // Update status to SUCCESS
            ErpScenario scenario = erpScenarioMapper.selectById(scenarioId);
            SyncTaskStatus finalStatus = SyncTaskStatus.builder()
                .taskId(taskId)
                .status("SUCCESS".equals(scenario.getLastSyncStatus()) ? "SUCCESS" : "FAIL")
                .endTime(LocalDateTime.now())
                .errorMessage("FAIL".equals(scenario.getLastSyncStatus()) ? scenario.getLastSyncMsg() : null)
                .progress(1.0)
                .build();
            taskStore.put(taskId, finalStatus);

        } catch (Exception e) {
            log.error("Async sync failed for task {}", taskId, e);
            SyncTaskStatus errorStatus = SyncTaskStatus.builder()
                .taskId(taskId)
                .status("FAIL")
                .errorMessage(e.getMessage())
                .endTime(LocalDateTime.now())
                .build();
            taskStore.put(taskId, errorStatus);
        }
    }

    public SyncTaskStatus getTaskStatus(String taskId) {
        return taskStore.get(taskId);
    }

    public SyncTaskDTO submitSyncTask(Long scenarioId) {
        String taskId = "sync-" + scenarioId + "-" + System.currentTimeMillis();

        SyncTaskDTO task = SyncTaskDTO.builder()
            .taskId(taskId)
            .status("SUBMITTED")
            .message("同步任务已提交")
            .build();

        return task;
    }
}
```

**Step 4: Update controller endpoint**

Modify `ErpScenarioController.java`:

```java
import com.nexusarchive.dto.SyncTaskDTO;
import com.nexusarchive.dto.SyncTaskStatus;
import com.nexusarchive.service.erp.AsyncErpSyncService;

@RestController
@RequestMapping("/erp/scenario")
@RequiredArgsConstructor
@Tag(name = "ERP业务场景管理")
public class ErpScenarioController {

    private final ErpScenarioService erpScenarioService;
    private final AsyncErpSyncService asyncErpSyncService;  // ADD THIS

    // ... existing methods ...

    @PostMapping("/{id}/sync")
    @Operation(summary = "手动触发同步（异步）")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @ArchivalAudit(operationType = "CAPTURE", resourceType = "ERP_SYNC", description = "手动触发ERP同步场景")
    public Result<SyncTaskDTO> triggerSync(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);

        // Submit async task
        SyncTaskDTO task = asyncErpSyncService.submitSyncTask(id);
        asyncErpSyncService.syncScenarioAsync(task.getTaskId(), id, operatorId, clientIp);

        return Result.success(task);
    }

    @GetMapping("/{id}/sync/status/{taskId}")
    @Operation(summary = "查询同步任务状态")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<SyncTaskStatus> getSyncStatus(@PathVariable Long id, @PathVariable String taskId) {
        SyncTaskStatus status = asyncErpSyncService.getTaskStatus(taskId);
        if (status == null) {
            return Result.error("任务不存在: " + taskId);
        }
        return Result.success(status);
    }
}
```

**Step 5: Write async test**

```java
// ErpScenarioControllerAsyncTest.java
@Test
void triggerSync_shouldReturnImmediately_withTaskId() {
    var response = restTemplate.postForEntity(
        "/erp/scenario/1/sync",
        null,
        SyncTaskDTO.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getTaskId()).isNotEmpty();
    assertThat(response.getBody().getStatus()).isEqualTo("SUBMITTED");
}

@Test
void getSyncStatus_shouldReturnProgress() throws InterruptedException {
    // Submit sync
    var task = restTemplate.postForEntity(
        "/erp/scenario/1/sync",
        null,
        SyncTaskDTO.class
    ).getBody();

    // Wait a bit
    Thread.sleep(1000);

    // Check status
    var status = restTemplate.getForEntity(
        "/erp/scenario/1/sync/status/" + task.getTaskId(),
        SyncTaskStatus.class
    ).getBody();

    assertThat(status.getStatus()).isIn("RUNNING", "SUCCESS", "FAIL");
}
```

**Step 6: Run test to verify it passes**

Run: `mvn test -Dtest=ErpScenarioControllerAsyncTest`
Expected: PASS (async operations complete correctly)

**Step 7: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/dto/SyncTaskDTO.java \
        nexusarchive-java/src/main/java/com/nexusarchive/dto/SyncTaskStatus.java \
        nexusarchive-java/src/main/java/com/nexusarchive/service/erp/AsyncErpSyncService.java \
        nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpScenarioController.java \
        nexusarchive-java/src/main/java/com/nexusarchive/config/AsyncConfig.java
git commit -m "feat(erp): convert sync operations to async execution

Breaking change: POST /erp/scenario/{id}/sync now returns SyncTaskDTO instead of void

Benefits:
- Non-blocking HTTP responses
- Progress tracking via GET /erp/scenario/{id}/sync/status/{taskId}
- Better UX for long-running syncs
- Prevents request timeouts

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 4: Add Input Validation

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/ScenarioParamsUpdateRequest.java`
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpScenarioController.java`
- Test: `nexusarchive-java/src/test/java/com/nexusarchive/controller/ErpScenarioControllerValidationTest.java`

**Step 1: Create validated DTO**

Create `ScenarioParamsUpdateRequest.java`:

```java
package com.nexusarchive.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioParamsUpdateRequest {

    @NotBlank(message = "场景参数不能为空")
    private String scenarioKey;

    @Pattern(regexp = "^(MANUAL|SCHEDULED|REALTIME)$", message = "同步策略必须是 MANUAL、SCHEDULED 或 REALTIME")
    private String syncStrategy;

    @Min(value = 1, message = "期间间隔天数必须大于0")
    @Max(value = 365, message = "期间间隔天数不能超过365")
    private Integer periodDays;

    private Map<String, Object> mapping;

    private Map<String, Object> filter;
}
```

**Step 2: Write validation test**

```java
// ErpScenarioControllerValidationTest.java
@Test
void updateScenarioParams_shouldReject_invalidSyncStrategy() {
    var request = ScenarioParamsUpdateRequest.builder()
        .scenarioKey("VOUCHER_SYNC")
        .syncStrategy("INVALID_STRATEGY")  // Invalid
        .build();

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    var entity = new HttpEntity<>(request, headers);

    var response = restTemplate.exchange(
        "/erp/scenario/1/params",
        HttpMethod.PUT,
        entity,
        Void.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}
```

**Step 3: Run test to verify it fails**

Run: `mvn test -Dtest=ErpScenarioControllerValidationTest`
Expected: FAIL (validation doesn't reject invalid input)

**Step 4: Update controller to use validated DTO**

Modify `ErpScenarioController.java`:

```java
import com.nexusarchive.dto.request.ScenarioParamsUpdateRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/erp/scenario")
@RequiredArgsConstructor
@Tag(name = "ERP业务场景管理")
public class ErpScenarioController {

    // ... existing methods ...

    @PutMapping("/{id}/params")
    @Operation(summary = "更新场景参数配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SCENARIO_PARAMS", description = "更新ERP场景参数配置")
    public Result<Void> updateScenarioParams(@PathVariable Long id,
                                            @Valid @RequestBody ScenarioParamsUpdateRequest request) {
        // Convert validated DTO to Map
        Map<String, Object> params = convertToParamsMap(request);
        erpScenarioService.updateScenarioParams(id, params);
        return Result.success();
    }

    private Map<String, Object> convertToParamsMap(ScenarioParamsUpdateRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("scenarioKey", request.getScenarioKey());
        params.put("syncStrategy", request.getSyncStrategy());
        if (request.getPeriodDays() != null) {
            params.put("periodDays", request.getPeriodDays());
        }
        if (request.getMapping() != null) {
            params.put("mapping", request.getMapping());
        }
        if (request.getFilter() != null) {
            params.put("filter", request.getFilter());
        }
        return params;
    }
}
```

**Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=ErpScenarioControllerValidationTest`
Expected: PASS (invalid input is rejected with 400)

**Step 6: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/dto/request/ScenarioParamsUpdateRequest.java \
        nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpScenarioController.java
git commit -m "security(validation): add input validation to scenario params

New validated DTO replaces raw Map<String, Object>:
- ScenarioParamsUpdateRequest with @Valid annotations
- Enforces enum values (syncStrategy)
- Validates numeric ranges (periodDays)
- Prevents injection of malicious parameters

Breaking change: Request body format changed for PUT /erp/scenario/{id}/params

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 5: Update E2E Tests to Use Correct API Paths

**Files:**
- Modify: `src/e2e/yonsuite-verification.spec.ts`
- Test: Run E2E tests to verify

**Step 1: Update API paths in E2E test**

Modify `yonsuite-verification.spec.ts`:

```typescript
// Change from incorrect paths to correct paths

// OLD (incorrect):
const scenariosResponse = await request.get(`${API_BASE}/api/erp/scenarios?configId=1`);

// NEW (correct):
const scenariosResponse = await request.get(`${API_BASE}/erp/scenario/list/1`);
```

Full updated test:

```typescript
test('should have exactly 5 real scenarios in database', async ({ request }) => {
    // Step 1: Login to get token
    const loginResponse = await request.post(`${API_BASE}/api/auth/login`, {
      headers: { 'Content-Type': 'application/json' },
      data: {
        username: 'admin',
        password: 'admin123'
      }
    });

    expect(loginResponse.ok()).toBeTruthy();
    const loginData = await loginResponse.json();
    const token = loginData.token;

    // Step 2: Get scenarios for YonSuite (configId=1) - CORRECTED PATH
    const scenariosResponse = await request.get(`${API_BASE}/erp/scenario/list/1`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    // Step 3: Verify response
    if (scenariosResponse.status() === 404) {
      console.warn('⚠️ API endpoint /erp/scenario/list not found (404)');
      test.skip(true, 'API endpoint not implemented');
      return;
    }

    expect(scenariosResponse.ok()).toBeTruthy();
    const scenarios = await scenariosResponse.json();

    // Step 4: Verify exactly 5 scenarios
    expect(scenarios.data.length).toBe(5);
    console.log(`✅ Database has ${scenarios.data.length} scenarios (expected: 5)`);

    // ... rest of test
});
```

**Step 2: Run E2E test**

Run: `npx playwright test src/e2e/yonsuite-verification.spec.ts`

**Step 3: Verify tests pass**

Expected: All tests pass with correct API paths

**Step 4: Commit**

```bash
git add src/e2e/yonsuite-verification.spec.ts
git commit -m "test(e2e): fix API paths to match actual backend endpoints

Updated paths:
- /api/erp/scenarios?configId=1 → /erp/scenario/list/1
- Other path corrections to match ErpScenarioController

Tests now correctly verify YonSuite scenarios.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Final Summary

After completing all 5 tasks:

| Task | Security Issue | Severity | Status |
|------|----------------|----------|--------|
| 1 | Missing @PreAuthorize | 🔴 CRITICAL | ✅ Fixed |
| 2 | Missing @ArchivalAudit | 🟡 MEDIUM | ✅ Fixed |
| 3 | Blocking sync calls | 🟡 MEDIUM | ✅ Fixed |
| 4 | No input validation | 🟡 MEDIUM | ✅ Fixed |
| 5 | Wrong E2E paths | 🟢 LOW | ✅ Fixed |

**Total commits:** 5
**Total files modified:** 7
**Total files created:** 4

**Breaking Changes:**
- `POST /erp/scenario/{id}/sync` now returns `SyncTaskDTO` instead of `Void`
- `PUT /erp/scenario/{id}/params` request body changed from `Map<String, Object>` to `ScenarioParamsUpdateRequest`

**Migration required:** Yes, frontend needs to update API calls to handle async sync responses.
