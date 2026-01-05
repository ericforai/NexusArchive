# 后端架构重构实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 逐步降低后端代码熵值，消除反模式，建立清晰的模块边界

**架构方法:** 以 `modules/borrowing/` 为模板，采用DDD分层架构，分阶段从简单到复杂重构

**技术栈:** Spring Boot 3.1.6, Java 17, MyBatis-Plus, ArchUnit

---

## 工作空间信息

- **Worktree:** `.worktrees/architecture-refactor`
- **分支:** `feature/architecture-refactor`
- **基准测试:** 20 个架构测试全部通过

---

## 阶段概览

| 阶段 | 目标 | 复杂度 | 预计任务数 |
|------|------|--------|-----------|
| Phase 0 | 基础设施准备 | 简单 | 3 |
| Phase 1 | 简单拆分（300-400行文件） | 简单 | 4 |
| Phase 2 | 中等拆分（400-500行文件） | 中等 | 3 |
| Phase 3 | 复杂拆分（500-700行文件） | 复杂 | 3 |
| Phase 4 | 层间依赖修复 | 复杂 | 4 |
| Phase 5 | 模块化迁移 | 非常复杂 | 5 |

---

## Phase 0: 基础设施准备

### Task 0.1: 创建模块模板目录结构

**目标:** 为后续模块迁移建立标准的DDD目录结构模板

**文件:**
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/modules/_template/README.md`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/modules/_template/api/.gitkeep`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/modules/_template/app/.gitkeep`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/modules/_template/domain/.gitkeep`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/modules/_template/infra/.gitkeep`

**Step 1: 创建模块 README 模板**

```markdown
# {模块名称}模块

本目录存放 {模块名称} 的DDD分层实现。

## 目录结构

- `api/`: 对外接口（Controller + DTO）
- `app/`: 应用层（Facade / 用例编排）
- `domain/`: 领域模型（Entity、Status 等）
- `infra/`: 基础设施（Mapper、策略实现）

## 对外契约

- 仅允许依赖 `com.nexusarchive.modules.{module_name}.app..`
- 仅允许依赖 `com.nexusarchive.modules.{module_name}.api.dto..`
- 禁止外部访问 `domain` / `infra`
```

**Step 2: 创建目录结构**

```bash
mkdir -p nexusarchive-java/src/main/java/com/nexusarchive/modules/_template/{api,app,domain,infra}
touch nexusarchive-java/src/main/java/com/nexusarchive/modules/_template/{api,app,domain,infra}/.gitkeep
```

**Step 3: 验证目录创建**

```bash
ls -la nexusarchive-java/src/main/java/com/nexusarchive/modules/_template/
```

预期输出:
```
drwxr-xr-x  api
drwxr-xr-x  app
drwxr-xr-x  domain
drwxr-xr-x  infra
-rw-r--r--  README.md
```

**Step 4: 提交**

```bash
git add docs/plans/2026-01-05-backend-architecture-refactor.md
git add nexusarchive-java/src/main/java/com/nexusarchive/modules/_template/
git commit -m "feat(arch): add module template and infrastructure preparation"
```

---

### Task 0.2: 添加 ArchUnit 模块边界规则

**目标:** 在架构测试中添加模块边界检查规则，防止未来违反分层

**文件:**
- 修改: `nexusarchive-java/src/test/java/com/nexusarchive/ArchitectureTest.java`

**Step 1: 读取现有架构测试**

```bash
cat nexusarchive-java/src/test/java/com/nexusarchive/ArchitectureTest.java
```

**Step 2: 在 ArchitectureTest 中添加模块边界规则**

在 `ArchitectureTest.java` 中添加以下测试方法：

```java
@ArchTest
static final ArchRule module_borrowing_domain_should_not_depend_on_infra =
    noClasses()
        .that().resideInAPackage("..modules.borrowing.domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..modules.borrowing.infra..")
        .because("Domain layer should not depend on Infrastructure");

@ArchTest
static final ArchRule module_borrowing_api_should_only_depend_on_app =
    classes()
        .that().resideInAPackage("..modules.borrowing.api..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "..modules.borrowing.api..",
            "..modules.borrowing.app..",
            "..lombok..",
            "..springframework..",
            "..jakarta..",
            "java.."
        )
        .because("API layer should only depend on app layer and framework classes");

@ArchTest
static final ArchRule integration_layer_should_not_depend_on_service_impl =
    noClasses()
        .that().resideInAPackage("..integration..")
        .should().dependOnClassesThat()
        .resideInAPackage("..service.impl..")
        .because("Integration layer should depend on service interfaces, not implementations");
```

**Step 3: 运行测试验证新规则**

```bash
cd nexusarchive-java && mvn test -Dtest=ArchitectureTest -q
```

预期输出: `BUILD SUCCESS`

**Step 4: 提交**

```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/ArchitectureTest.java
git commit -m "test(arch): add module boundary rules to ArchitectureTest"
```

---

### Task 0.3: 创建重构追踪文档

**目标:** 创建一个进度追踪文档，记录每个文件的重构状态

**文件:**
- 创建: `docs/plans/architecture-refactor-progress.md`

**Step 1: 创建追踪文档**

```markdown
# 后端架构重构进度追踪

> 更新时间: 2026-01-05

## 文件行数统计基准

| 文件 | 当前行数 | 目标行数 | 状态 |
|------|----------|----------|------|
| YonSuiteErpAdapter.java | 728 | <300 | 待处理 |
| IngestServiceImpl.java | 685 | <300 | 待处理 |
| OriginalVoucherService.java | 658 | <300 | 待处理 |
| ArcFileContent.java | 635 | <300 | 待处理 |
| PoolController.java | 632 | <300 | 待处理 |
| YonSuiteClient.java | 619 | <300 | 待处理 |
| VoucherMatchingEngine.java | 565 | <300 | 待处理 |
| ModuleGovernanceService.java | 538 | <300 | 待处理 |
| ComplianceCheckService.java | 537 | <300 | 待处理 |
| ErpSyncService.java | 498 | <300 | 待处理 |
| ReconciliationServiceImpl.java | 495 | <300 | 待处理 |
| CollectionBatchServiceImpl.java | 453 | <300 | 待处理 |
| VoucherPdfGenerator.java | 446 | <300 | 待处理 |
| StreamingPreviewServiceImpl.java | 442 | <300 | 待处理 |
| ErpAdaptationController.java | 433 | <300 | 待处理 |
| Sm2SignatureService.java | 408 | <300 | 待处理 |
| PerformanceMetricsServiceImpl.java | 333 | <300 | 待处理 |
| FourNatureCheckServiceImpl.java | 365 | <300 | 待处理 |
| FondsHistoryServiceImpl.java | 346 | <300 | 待处理 |
| ArchiveAppraisalServiceImpl.java | 313 | <300 | 待处理 |
| UserLifecycleServiceImpl.java | 311 | <300 | 待处理 |
| LegacyImportOrchestrator.java | 320 | <300 | 待处理 |
| FourNatureChecker.java | 394 | <300 | 待处理 |
| MfaServiceImpl.java | 384 | <300 | 待处理 |

## 阶段完成状态

- [x] Phase 0: 基础设施准备
- [ ] Phase 1: 简单拆分（300-400行文件）
- [ ] Phase 2: 中等拆分（400-500行文件）
- [ ] Phase 3: 复杂拆分（500-700行文件）
- [ ] Phase 4: 层间依赖修复
- [ ] Phase 5: 模块化迁移

## 层间依赖违规统计

| 违规类型 | 数量 | 状态 |
|----------|------|------|
| Integration → Service (concrete) | 9 | 待修复 |
| Service → Integration | 23 | 待修复 |
| Controller 直接调用 Mapper | 0 | ✅ 良好 |

## 模块化迁移进度

- [x] modules/borrowing - 完成（参考模板）
- [ ] modules/ingest - 规划中
- [ ] modules/voucher - 规划中
- [ ] modules/erp-sync - 规划中
- [ ] modules/archival - 规划中
```

**Step 2: 提交**

```bash
git add docs/plans/architecture-refactor-progress.md
git commit -m "docs(arch): add architecture refactor progress tracker"
```

---

## Phase 1: 简单拆分（300-400行文件）

### Task 1.1: 拆分 UserLifecycleServiceImpl (311行)

**目标:** 将用户生命周期服务拆分为核心逻辑和辅助方法

**分析:**
- 文件: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/UserLifecycleServiceImpl.java`
- 当前行数: 311
- 主要问题: 可能包含密码重置、账户激活、停用等多个职责

**文件:**
- 修改: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/UserLifecycleServiceImpl.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/user/PasswordResetService.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/user/AccountActivationService.java`

**Step 1: 读取并分析现有代码**

```bash
cat nexusarchive-java/src/main/java/com/nexusarchive/service/impl/UserLifecycleServiceImpl.java
```

**Step 2: 识别可提取的方法组**

查找以下模式:
- 密码相关方法: `resetPassword`, `changePassword`, `validatePassword`
- 激活相关方法: `activateAccount`, `sendActivationEmail`
- 停用相关方法: `deactivateAccount`, `suspendAccount`

**Step 3: 创建 PasswordResetService**

```java
package com.nexusarchive.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final com.nexusarchive.mapper.UserMapper userMapper;
    private final com.nexusarchive.service.EmailService emailService;

    /**
     * 发送密码重置邮件
     */
    public void sendPasswordResetEmail(String email) {
        // TODO: 从原类迁移实现
    }

    /**
     * 重置密码
     */
    public void resetPassword(String token, String newPassword) {
        // TODO: 从原类迁移实现
    }

    /**
     * 验证密码强度
     */
    public boolean validatePasswordStrength(String password) {
        // TODO: 从原类迁移实现
        return true;
    }
}
```

**Step 4: 创建 AccountActivationService**

```java
package com.nexusarchive.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountActivationService {

    private final com.nexusarchive.mapper.UserMapper userMapper;
    private final com.nexusarchive.service.EmailService emailService;

    /**
     * 激活账户
     */
    public void activateAccount(String token) {
        // TODO: 从原类迁移实现
    }

    /**
     * 发送激活邮件
     */
    public void sendActivationEmail(String userId) {
        // TODO: 从原类迁移实现
    }
}
```

**Step 5: 重构原类，使用新的服务**

```java
@Service
@RequiredArgsConstructor
public class UserLifecycleServiceImpl implements UserLifecycleService {

    private final PasswordResetService passwordResetService;
    private final AccountActivationService accountActivationService;
    // 保留核心协调逻辑

    @Override
    public void initiatePasswordReset(String email) {
        passwordResetService.sendPasswordResetEmail(email);
    }

    @Override
    public void activateUserAccount(String token) {
        accountActivationService.activateAccount(token);
    }
}
```

**Step 6: 运行测试**

```bash
cd nexusarchive-java && mvn test -Dtest=*UserLifecycle* -q
```

**Step 7: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/
git commit -m "refactor(user): extract PasswordResetService and AccountActivationService from UserLifecycleServiceImpl"
```

---

### Task 1.2: 拆分 ArchiveAppraisalServiceImpl (313行)

**目标:** 将档案鉴定服务拆分为独立的业务规则组件

**文件:**
- 修改: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ArchiveAppraisalServiceImpl.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/appraisal/AppraisalRuleValidator.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/appraisal/AppraisalWorkflowService.java`

**Step 1: 读取现有代码**

```bash
cat nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ArchiveAppraisalServiceImpl.java
```

**Step 2: 识别职责边界**

查找:
- 规则验证逻辑: `validateAppraisal`, `checkEligibility`
- 工作流协调: `submitForApproval`, `approve`, `reject`

**Step 3: 创建 AppraisalRuleValidator**

```java
package com.nexusarchive.service.appraisal;

import com.nexusarchive.entity.Archive;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class AppraisalRuleValidator {

    /**
     * 验证档案是否符合鉴定条件
     */
    public boolean isValidForAppraisal(Archive archive) {
        // 业务规则:
        // 1. 档案状态必须为"已归档"
        // 2. 归档时间超过1年
        // 3. 保管期限未到期
        // TODO: 从原类迁移实现
        return false;
    }

    /**
     * 计算鉴定优先级
     */
    public int calculateAppraisalPriority(Archive archive) {
        // 基于保管期限、档案价值等计算
        // TODO: 从原类迁移实现
        return 0;
    }
}
```

**Step 4: 运行测试并提交**

```bash
cd nexusarchive-java && mvn test -Dtest=*Appraisal* -q
git add nexusarchive-java/src/main/java/com/nexusarchive/service/appraisal/
git commit -m "refactor(appraisal): extract AppraisalRuleValidator from ArchiveAppraisalServiceImpl"
```

---

### Task 1.3: 拆分 LegacyImportOrchestrator (320行)

**目标:** 将遗留导入编排器拆分为清晰的步骤处理器

**文件:**
- 修改: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/legacy/LegacyImportOrchestrator.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/legacy/import/ImportStepProcessor.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/legacy/import/ImportValidator.java`

**Step 1: 分析现有代码结构**

```bash
cat -n nexusarchive-java/src/main/java/com/nexusarchive/service/impl/legacy/LegacyImportOrchestrator.java
```

**Step 2: 创建 ImportStepProcessor 接口**

```java
package com.nexusarchive.service.legacy.import;

import com.nexusarchive.dto.legacy.ImportContext;

public interface ImportStepProcessor {
    /**
     * 处理导入步骤
     * @return true 表示继续下一步，false 表示终止
     */
    boolean process(ImportContext context);

    /**
     * 获取步骤名称
     */
    String getStepName();
}
```

**Step 3: 创建具体步骤处理器**

```java
package com.nexusarchive.service.legacy.import;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ValidationStepProcessor implements ImportStepProcessor {

    @Override
    public boolean process(ImportContext context) {
        // 验证逻辑
        return true;
    }

    @Override
    public String getStepName() {
        return "Validation";
    }
}

@Component
@Order(2)
public class DataTransformationStepProcessor implements ImportStepProcessor {

    @Override
    public boolean process(ImportContext context) {
        // 数据转换逻辑
        return true;
    }

    @Override
    public String getStepName() {
        return "DataTransformation";
    }
}
```

**Step 4: 运行测试并提交**

```bash
cd nexusarchive-java && mvn test -Dtest=*LegacyImport* -q
git add nexusarchive-java/src/main/java/com/nexusarchive/service/legacy/import/
git commit -m "refactor(legacy): extract step processors from LegacyImportOrchestrator"
```

---

### Task 1.4: 拆分 FondsHistoryServiceImpl (346行)

**目标:** 将全宗历史服务拆分为查询和更新两个独立组件

**文件:**
- 修改: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/FondsHistoryServiceImpl.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/fonds/FondsHistoryQueryService.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/fonds/FondsHistoryUpdateService.java`

**Step 1: 分析代码**

```bash
cat nexusarchive-java/src/main/java/com/nexusarchive/service/impl/FondsHistoryServiceImpl.java
```

**Step 2: 创建查询服务**

```java
package com.nexusarchive.service.fonds;

import com.nexusarchive.entity.FondsHistory;
import com.nexusarchive.mapper.FondsHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FondsHistoryQueryService {

    private final FondsHistoryMapper fondsHistoryMapper;

    public List<FondsHistory> getByFondsId(String fondsId) {
        QueryWrapper<FondsHistory> qw = new QueryWrapper<>();
        qw.eq("fonds_id", fondsId);
        qw.orderByDesc("change_date");
        return fondsHistoryMapper.selectList(qw);
    }

    public List<FondsHistory> getByDateRange(LocalDate start, LocalDate end) {
        QueryWrapper<FondsHistory> qw = new QueryWrapper<>();
        qw.between("change_date", start, end);
        return fondsHistoryMapper.selectList(qw);
    }
}
```

**Step 3: 创建更新服务**

```java
package com.nexusarchive.service.fonds;

import com.nexusarchive.entity.FondsHistory;
import com.nexusarchive.mapper.FondsHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FondsHistoryUpdateService {

    private final FondsHistoryMapper fondsHistoryMapper;

    @Transactional
    public void recordChange(FondsHistory history) {
        history.setChangeTime(LocalDateTime.now());
        fondsHistoryMapper.insert(history);
    }
}
```

**Step 4: 运行测试并提交**

```bash
cd nexusarchive-java && mvn test -Dtest=*FondsHistory* -q
git add nexusarchive-java/src/main/java/com/nexusarchive/service/fonds/
git commit -m "refactor(fonds): extract query and update services from FondsHistoryServiceImpl"
```

---

## Phase 2: 中等拆分（400-500行文件）

### Task 2.1: 拆分 VoucherPdfGenerator (446行)

**目标:** 将PDF生成器拆分为模板管理、内容生成、文件处理三个组件

**分析:**
- 文件包含PDF模板、样式、内容填充、文件保存等多个职责
- 可按PDF生成生命周期拆分

**文件:**
- 修改: `nexusarchive-java/src/main/java/com/nexusarchive/service/pdf/VoucherPdfGenerator.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/pdf/template/PdfTemplateManager.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/pdf/generator/PdfContentGenerator.java`
- 创建: `nexusarchive-java/src/main/java/com/nexusarchive/service/pdf/storage/PdfStorageService.java`

**Step 1: 读取现有代码**

```bash
cat nexusarchive-java/src/main/java/com/nexusarchive/service/pdf/VoucherPdfGenerator.java
```

**Step 2: 创建 PdfTemplateManager**

```java
package com.nexusarchive.service.pdf.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PdfTemplateManager {

    private final ResourceLoader resourceLoader;
    private final Map<String, byte[]> templateCache = new HashMap<>();

    /**
     * 加载PDF模板
     */
    public byte[] loadTemplate(String templateName) {
        return templateCache.computeIfAbsent(templateName, name -> {
            try {
                Resource resource = resourceLoader.getResource("classpath:templates/pdf/" + name + ".pdf");
                return resource.getContentAsByteArray();
            } catch (IOException e) {
                log.error("Failed to load template: {}", name, e);
                throw new RuntimeException("Template not found: " + name, e);
            }
        });
    }

    /**
     * 获取可用模板列表
     */
    public String[] getAvailableTemplates() {
        return new String[]{"voucher_standard", "voucher_simple", "voucher_detailed"};
    }
}
```

**Step 3: 创建 PdfContentGenerator**

```java
package com.nexusarchive.service.pdf.generator;

import com.nexusarchive.dto.VoucherDto;
import com.nexusarchive.entity.Voucher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PdfContentGenerator {

    private final PdfTemplateManager templateManager;

    /**
     * 生成凭证PDF内容
     */
    public byte[] generateVoucherPdf(Voucher voucher, String templateName) {
        byte[] template = templateManager.loadTemplate(templateName);
        // 使用 iText 或 similar 填充模板
        // TODO: 实现PDF填充逻辑
        return template;
    }

    /**
     * 生成批量凭证PDF
     */
    public byte[] generateBatchVoucherPdf(java.util.List<Voucher> vouchers) {
        // 合并多个凭证PDF
        // TODO: 实现批量生成逻辑
        return new byte[0];
    }
}
```

**Step 4: 创建 PdfStorageService**

```java
package com.nexusarchive.service.pdf.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
@Slf4j
public class PdfStorageService {

    @Value("${pdf.storage.path:./data/pdfs}")
    private String storagePath;

    /**
     * 保存PDF文件
     */
    public Path savePdf(String fileName, byte[] content) throws IOException {
        Path filePath = Path.of(storagePath, fileName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return filePath;
    }

    /**
     * 读取PDF文件
     */
    public byte[] readPdf(String fileName) throws IOException {
        Path filePath = Path.of(storagePath, fileName);
        return Files.readAllBytes(filePath);
    }
}
```

**Step 5: 重构主生成器类**

```java
package com.nexusarchive.service.pdf;

import com.nexusarchive.entity.Voucher;
import com.nexusarchive.service.pdf.generator.PdfContentGenerator;
import com.nexusarchive.service.pdf.storage.PdfStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoucherPdfGenerator {

    private final PdfContentGenerator contentGenerator;
    private final PdfStorageService storageService;

    /**
     * 生成并保存凭证PDF
     */
    public String generateAndSave(Voucher voucher) {
        byte[] pdfContent = contentGenerator.generateVoucherPdf(voucher, "voucher_standard");
        String fileName = "voucher_" + voucher.getId() + ".pdf";
        try {
            Path path = storageService.savePdf(fileName, pdfContent);
            return path.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save PDF", e);
        }
    }
}
```

**Step 6: 运行测试**

```bash
cd nexusarchive-java && mvn test -Dtest=*Pdf* -q
```

**Step 7: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/pdf/
git commit -m "refactor(pdf): split VoucherPdfGenerator into template, generator, and storage components"
```

---

## 执行总结

### 完成标准

每个Task完成后必须：
1. ✅ 所有相关测试通过
2. ✅ 代码已提交
3. ✅ 架构测试通过
4. ✅ 更新进度追踪文档

### 提交规范

```
refactor(module): description          # 重构
feat(arch): description                # 新功能(模块)
test(arch): description                # 测试
docs(arch): description                # 文档
```

### 回滚计划

如果某阶段引入问题：
1. 使用 `git revert <commit>` 回滚
2. 或 `git reset --hard HEAD~1` (如果未推送)
3. 重新分析问题并调整计划

---

## 进度检查点

- [ ] Checkpoint 1: Phase 0 完成 - 基础设施就绪
- [ ] Checkpoint 2: Phase 1 完成 - 4个简单文件拆分完成
- [ ] Checkpoint 3: Phase 2 完成 - 3个中等文件拆分完成
- [ ] Checkpoint 4: Phase 3 完成 - 3个复杂文件拆分完成
- [ ] Checkpoint 5: Phase 4 完成 - 层间依赖修复完成
- [ ] Checkpoint 6: Phase 5 完成 - 模块化迁移完成

---

**下一步:** 使用 `superpowers:executing-plans` 执行此计划，或使用 `superpowers:subagent-driven-development` 进行子代理驱动的开发。
