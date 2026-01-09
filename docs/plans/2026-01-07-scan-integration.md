# 扫描集成功能实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 将 `/system/collection/scan` 页面改造为扫描功能工作台，支持扫描仪监控、文件上传 OCR 识别、移动端拍照，数据先入临时工作区，确认后提交到预归档池。

**架构:** 复用现有 `OCRProcessingView.tsx` 组件，扩展监控文件夹和移动端功能。后端新增扫描工作区服务，对接 PaddleOCR（默认）和云 OCR（可选）。前端通过 REST API + WebSocket 与后端通信。

**技术栈:** React 19、Spring Boot 3.1、PaddleOCR、WebSocket、PostgreSQL

---

## 背景与设计决策

| 决策项 | 选择 | 说明 |
|--------|------|------|
| 核心定位 | 混合模式 + 移动端 | 扫描仪/文件上传/手机拍照三合一 |
| 扫描仪对接 | 监控文件夹 + 云同步 | 用户用扫描仪软件扫描到指定文件夹，系统自动监控导入 |
| OCR 引擎 | 混合策略 | PaddleOCR 默认 + 云 OCR 可选（需配置 API Key） |
| 数据流转 | 临时工作区 | 先入扫描工作区，用户确认后提交到预归档池 |
| 页面实现 | 复用 OCR 视图 | 基于现有 `OCRProcessingView.tsx` 扩展 |

---

## Task 1: 前端路由调整

**目标:** 将 `/system/collection/scan` 路由指向 `OCRProcessingView` 组件

**Files:**
- Modify: `src/routes/index.tsx`

**Step 1: 修改路由配置**

找到 scan 路由配置，将 `ArchiveListPage` 改为 `OCRProcessingView`:

```tsx
// 原代码（约第 173 行）
{ path: 'collection/scan', element: <ArchiveListPage routeConfig="scan" /> },

// 修改为
import { OCRProcessingView } from '../pages/pre-archive/OCRProcessingView';
{ path: 'collection/scan', element: <OCRProcessingView /> },
```

**Step 2: 验证路由访问**

访问: http://localhost:15175/system/collection/scan
预期: 显示 OCR 智能识别工坊页面

**Step 3: 提交**

```bash
git add src/routes/index.tsx
git commit -m "feat(scan): route scan page to OCRProcessingView"
```

---

## Task 2: 后端 - 扫描工作区数据表

**目标:** 创建扫描工作区数据表，存储临时扫描文件和 OCR 结果

**Files:**
- Create: `nexusarchive-java/src/main/resources/db/migration/V{version}__create_scan_workspace_table.sql`

**Step 1: 编写 Flyway 迁移脚本**

```sql
-- 扫描工作区表：存储临时扫描文件和 OCR 识别结果
CREATE TABLE scan_workspace (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,           -- 会话ID（用于移动端关联）
    user_id BIGINT NOT NULL,                   -- 用户ID
    file_name VARCHAR(255) NOT NULL,           -- 原始文件名
    file_path VARCHAR(500) NOT NULL,           -- 文件存储路径
    file_size BIGINT,                          -- 文件大小（字节）
    file_type VARCHAR(50),                     -- 文件类型（pdf, jpg, png等）
    upload_source VARCHAR(50) NOT NULL,        -- 上传来源（upload, monitor, mobile）

    -- OCR 相关字段
    ocr_status VARCHAR(50) NOT NULL DEFAULT 'pending',  -- pending, processing, review, completed, failed
    ocr_engine VARCHAR(50),                    -- 使用的OCR引擎（paddleocr, baidu, aliyun）
    ocr_result JSONB,                          -- OCR识别结果（结构化数据）
    overall_score INTEGER,                     -- 整体置信度分数

    -- 文档分类
    doc_type VARCHAR(50),                      -- 文档类型（invoice, contract, receipt等）

    -- 提交状态
    submit_status VARCHAR(50) DEFAULT 'draft', -- draft, submitted
    archive_id BIGINT,                         -- 提交后关联的档案ID
    submitted_at TIMESTAMP,                    -- 提交时间

    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 索引
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_archive FOREIGN KEY (archive_id) REFERENCES acc_archive(id) ON DELETE SET NULL
);

CREATE INDEX idx_scan_session ON scan_workspace(session_id);
CREATE INDEX idx_scan_user ON scan_workspace(user_id);
CREATE INDEX idx_scan_status ON scan_workspace(ocr_status, submit_status);
CREATE INDEX idx_scan_created ON scan_workspace(created_at DESC);

-- 文件监控配置表
CREATE TABLE scan_folder_monitor (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    folder_path VARCHAR(500) NOT NULL,        -- 监控文件夹路径
    is_active BOOLEAN DEFAULT TRUE,           -- 是否启用
    file_filter VARCHAR(200) DEFAULT '*.pdf;*.jpg;*.jpeg;*.png',  -- 文件类型过滤
    auto_delete BOOLEAN DEFAULT FALSE,        -- 导入后是否删除源文件
    move_to_path VARCHAR(500),                -- 导入后移动到的路径（可选）

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_monitor_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
```

**Step 2: 验证迁移**

运行: `mvn flyway:migrate` 或重启后端自动执行
预期: 数据库中创建 `scan_workspace` 和 `scan_folder_monitor` 表

**Step 3: 提交**

```bash
git add nexusarchive-java/src/main/resources/db/migration/V{version}__create_scan_workspace_table.sql
git commit -m "feat(scan): create scan workspace and folder monitor tables"
```

---

## Task 3: 后端 - 实体类与 Mapper

**目标:** 创建扫描工作区的 JPA 实体和 MyBatis Mapper

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/ScanWorkspace.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/ScanFolderMonitor.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ScanWorkspaceMapper.java`

**Step 1: 创建 ScanWorkspace 实体**

```java
package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 扫描工作区实体
 * Pos: nexusarchive-java/src/main/java/com/nexusarchive/entity/ScanWorkspace.java
 */
@Data
@TableName("scan_workspace")
public class ScanWorkspace {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;
    private Long userId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String uploadSource;

    private String ocrStatus = "pending";  // pending, processing, review, completed, failed
    private String ocrEngine;
    private String ocrResult;  // JSON string
    private Integer overallScore;

    private String docType;

    private String submitStatus = "draft";
    private Long archiveId;
    private LocalDateTime submittedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

**Step 2: 创建 ScanFolderMonitor 实体**

```java
package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件夹监控配置实体
 * Pos: nexusarchive-java/src/main/java/com/nexusarchive/entity/ScanFolderMonitor.java
 */
@Data
@TableName("scan_folder_monitor")
public class ScanFolderMonitor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String folderPath;
    private Boolean isActive = true;
    private String fileFilter = "*.pdf;*.jpg;*.jpeg;*.png";
    private Boolean autoDelete = false;
    private String moveToPath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

**Step 3: 创建 Mapper**

```java
package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ScanWorkspace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 扫描工作区 Mapper
 * Pos: nexusarchive-java/src/main/java/com/nexusarchive/mapper/ScanWorkspaceMapper.java
 */
@Mapper
public interface ScanWorkspaceMapper extends BaseMapper<ScanWorkspace> {

    @Select("SELECT * FROM scan_workspace WHERE user_id = #{userId} AND submit_status = 'draft' ORDER BY created_at DESC")
    List<ScanWorkspace> findDraftsByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM scan_workspace WHERE session_id = #{sessionId} ORDER BY created_at DESC")
    List<ScanWorkspace> findBySessionId(@Param("sessionId") String sessionId);

    @Update("UPDATE scan_workspace SET ocr_status = #{status}, ocr_result = #{result}, overall_score = #{score} WHERE id = #{id}")
    int updateOcrResult(@Param("id") Long id, @Param("status") String status,
                       @Param("result") String result, @Param("score") Integer score);

    @Update("UPDATE scan_workspace SET submit_status = 'submitted', archive_id = #{archiveId}, submitted_at = NOW() WHERE id = #{id}")
    int markAsSubmitted(@Param("id") Long id, @Param("archiveId") Long archiveId);
}
```

**Step 4: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/entity/ nexusarchive-java/src/main/java/com/nexusarchive/mapper/ScanWorkspaceMapper.java
git commit -m "feat(scan): add ScanWorkspace entities and mapper"
```

---

## Task 4: 后端 - 扫描工作区 Controller

**目标:** 创建扫描工作区的 REST API

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/controller/ScanWorkspaceController.java`

**Step 1: 创建 Controller**

```java
package com.nexusarchive.controller;

import com.nexusarchive.dto.Result;
import com.nexusarchive.entity.ScanWorkspace;
import com.nexusarchive.mapper.ScanWorkspaceMapper;
import com.nexusarchive.service.ScanWorkspaceService;
import com.nexusarchive.service.erp.OcrProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/**
 * 扫描工作区 Controller
 * Pos: nexusarchive-java/src/main/java/com/nexusarchive/controller/ScanWorkspaceController.java
 */
@Slf4j
@RestController
@RequestMapping("/api/scan/workspace")
@RequiredArgsConstructor
public class ScanWorkspaceController {

    private final ScanWorkspaceMapper scanWorkspaceMapper;
    private final ScanWorkspaceService scanWorkspaceService;
    private final OcrProcessingService ocrProcessingService;

    /**
     * 获取当前用户的工作区文件列表
     */
    @GetMapping
    public Result<List<ScanWorkspace>> getWorkspace(Principal principal) {
        Long userId = getCurrentUserId(principal);
        List<ScanWorkspace> items = scanWorkspaceMapper.findDraftsByUserId(userId);
        return Result.success(items);
    }

    /**
     * 上传文件到工作区
     */
    @PostMapping("/upload")
    public Result<ScanWorkspace> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", defaultValue = "upload") String source,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            Principal principal) throws Exception {

        Long userId = getCurrentUserId(principal);
        ScanWorkspace workspace = scanWorkspaceService.uploadToWorkspace(file, source, sessionId, userId);
        return Result.success(workspace);
    }

    /**
     * 触发 OCR 识别
     */
    @PostMapping("/{id}/ocr")
    public Result<Void> processOcr(@PathVariable Long id, Principal principal) {
        Long userId = getCurrentUserId(principal);
        ScanWorkspace workspace = scanWorkspaceMapper.selectById(id);
        if (workspace == null || !workspace.getUserId().equals(userId)) {
            return Result.error("工作区项不存在");
        }

        ocrProcessingService.processAsync(id, workspace.getFilePath());
        return Result.success();
    }

    /**
     * 更新 OCR 结果（用户编辑后）
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ScanWorkspace updates, Principal principal) {
        Long userId = getCurrentUserId(principal);
        ScanWorkspace workspace = scanWorkspaceMapper.selectById(id);
        if (workspace == null || !workspace.getUserId().equals(userId)) {
            return Result.error("工作区项不存在");
        }

        workspace.setOcrResult(updates.getOcrResult());
        workspace.setDocType(updates.getDocType());
        workspace.setOverallScore(updates.getOverallScore());
        scanWorkspaceMapper.updateById(workspace);

        return Result.success();
    }

    /**
     * 提交到预归档池
     */
    @PostMapping("/{id}/submit")
    public Result<Long> submitToPreArchive(@PathVariable Long id, Principal principal) {
        Long userId = getCurrentUserId(principal);
        Long archiveId = scanWorkspaceService.submitToPreArchive(id, userId);
        return Result.success(archiveId);
    }

    /**
     * 删除工作区项
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Principal principal) {
        Long userId = getCurrentUserId(principal);
        scanWorkspaceService.deleteFromWorkspace(id, userId);
        return Result.success();
    }

    private Long getCurrentUserId(Principal principal) {
        // 从 JWT 中获取用户ID
        return 1L; // 简化实现，实际从 SecurityContext 获取
    }
}
```

**Step 2: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/controller/ScanWorkspaceController.java
git commit -m "feat(scan): add ScanWorkspaceController"
```

---

## Task 5: 后端 - 扫描工作区 Service

**目标:** 实现文件上传、OCR 处理、提交预归档池的业务逻辑

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/ScanWorkspaceService.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ScanWorkspaceServiceImpl.java`

**Step 1: 创建 Service 接口**

```java
package com.nexusarchive.service;

import com.nexusarchive.entity.ScanWorkspace;
import org.springframework.web.multipart.MultipartFile;

/**
 * 扫描工作区 Service
 * Pos: nexusarchive-java/src/main/java/com/nexusarchive/service/ScanWorkspaceService.java
 */
public interface ScanWorkspaceService {

    /**
     * 上传文件到工作区
     */
    ScanWorkspace uploadToWorkspace(MultipartFile file, String source, String sessionId, Long userId) throws Exception;

    /**
     * 提交到预归档池
     */
    Long submitToPreArchive(Long workspaceId, Long userId);

    /**
     * 从工作区删除
     */
    void deleteFromWorkspace(Long workspaceId, Long userId);

    /**
     * 创建会话ID（用于移动端）
     */
    String createSession(Long userId);
}
```

**Step 2: 创建 Service 实现**

```java
package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.ScanWorkspace;
import com.nexusarchive.mapper.ScanWorkspaceMapper;
import com.nexusarchive.service.ScanWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 扫描工作区 Service 实现
 * Pos: nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ScanWorkspaceServiceImpl.java
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanWorkspaceServiceImpl implements ScanWorkspaceService {

    private final ScanWorkspaceMapper scanWorkspaceMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.scan.upload-path:/tmp/nexusarchive/scan}")
    private String uploadPath;

    @Override
    @Transactional
    public ScanWorkspace uploadToWorkspace(MultipartFile file, String source, String sessionId, Long userId) throws Exception {
        // 创建上传目录
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = Paths.get(uploadPath, String.valueOf(userId), datePath);
        Files.createDirectories(targetDir);

        // 保存文件
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedFilename = UUID.randomUUID().toString() + extension;
        Path targetPath = targetDir.resolve(savedFilename);
        file.transferTo(targetPath.toFile());

        // 创建工作区记录
        ScanWorkspace workspace = new ScanWorkspace();
        workspace.setSessionId(sessionId != null ? sessionId : UUID.randomUUID().toString());
        workspace.setUserId(userId);
        workspace.setFileName(originalFilename);
        workspace.setFilePath(targetPath.toString());
        workspace.setFileSize(file.getSize());
        workspace.setFileType(extension.replace(".", "").toLowerCase());
        workspace.setUploadSource(source);
        workspace.setOcrStatus("pending");

        scanWorkspaceMapper.insert(workspace);
        return workspace;
    }

    @Override
    @Transactional
    public Long submitToPreArchive(Long workspaceId, Long userId) {
        ScanWorkspace workspace = scanWorkspaceMapper.selectById(workspaceId);
        if (workspace == null || !workspace.getUserId().equals(userId)) {
            throw new IllegalArgumentException("工作区项不存在");
        }

        // TODO: 将工作区数据转换为预归档池记录
        // 调用 OriginalVoucherService 创建单据记录

        workspace.setSubmitStatus("submitted");
        workspace.setSubmittedAt(LocalDateTime.now());
        scanWorkspaceMapper.updateById(workspace);

        // 返回创建的档案ID（临时返回workspaceId）
        return workspaceId;
    }

    @Override
    @Transactional
    public void deleteFromWorkspace(Long workspaceId, Long userId) {
        ScanWorkspace workspace = scanWorkspaceMapper.selectById(workspaceId);
        if (workspace == null || !workspace.getUserId().equals(userId)) {
            throw new IllegalArgumentException("工作区项不存在");
        }

        // 删除物理文件
        try {
            Files.deleteIfExists(Paths.get(workspace.getFilePath()));
        } catch (Exception e) {
            log.warn("删除文件失败: {}", workspace.getFilePath(), e);
        }

        // 删除数据库记录
        scanWorkspaceMapper.deleteById(workspaceId);
    }

    @Override
    public String createSession(Long userId) {
        return UUID.randomUUID().toString();
    }
}
```

**Step 3: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/ScanWorkspaceService.java nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ScanWorkspaceServiceImpl.java
git commit -m "feat(scan): add ScanWorkspaceService implementation"
```

---

## Task 6: 后端 - OCR 处理服务

**目标:** 实现 OCR 处理服务，支持 PaddleOCR 和云 OCR

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/OcrProcessingService.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/OcrProcessingServiceImpl.java`

**Step 1: 创建 OCR 处理 Service**

```java
package com.nexusarchive.service;

import com.nexusarchive.dto.ocr.OcrResult;

/**
 * OCR 处理服务
 * Pos: nexusarchive-java/src/main/java/com/nexusarchive/service/OcrProcessingService.java
 */
public interface OcrProcessingService {

    /**
     * 异步处理 OCR
     */
    void processAsync(Long workspaceId, String filePath);

    /**
     * 同步处理 OCR（用于测试）
     */
    OcrResult processSync(String filePath, String engine);
}
```

**Step 2: 创建 OCR 处理 Service 实现**

```java
package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.ocr.OcrResult;
import com.nexusarchive.mapper.ScanWorkspaceMapper;
import com.nexusarchive.entity.ScanWorkspace;
import com.nexusarchive.service.OcrProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OCR 处理服务实现
 * Pos: nexusarchive-java/src/main/java/com/nexusarchive/service/impl/OcrProcessingServiceImpl.java
 *
 * 注意：此实现为 Mock 版本，用于测试数据流
 * 实际 PaddleOCR 集成需要 Python 微服务或 JNI 调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrProcessingServiceImpl implements OcrProcessingService {

    private final ScanWorkspaceMapper scanWorkspaceMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void processAsync(Long workspaceId, String filePath) {
        try {
            // 更新状态为处理中
            scanWorkspaceMapper.updateById(workspaceId,
                ScanWorkspace.builder().id(workspaceId).ocrStatus("processing").build());

            // TODO: 实际调用 PaddleOCR 或云 OCR 服务
            // 这里先使用 Mock 数据
            Thread.sleep(2000); // 模拟处理时间

            // 构建 Mock OCR 结果
            Map<String, Object> mockResult = new HashMap<>();
            mockResult.put("invoiceNo", "12345678");
            mockResult.put("amount", "¥1,234.56");
            mockResult.put("date", "2026-01-07");
            mockResult.put("sellerName", "测试公司");
            mockResult.put("buyerName", "采购方");

            String resultJson = objectMapper.writeValueAsString(mockResult);

            // 更新结果
            ScanWorkspace update = ScanWorkspace.builder()
                .id(workspaceId)
                .ocrStatus("review")
                .ocrEngine("paddleocr")
                .ocrResult(resultJson)
                .overallScore(88)
                .docType("invoice")
                .build();

            scanWorkspaceMapper.updateById(update);
            log.info("OCR 处理完成: workspaceId={}", workspaceId);

        } catch (Exception e) {
            log.error("OCR 处理失败: workspaceId={}", workspaceId, e);
            scanWorkspaceMapper.updateById(workspaceId,
                ScanWorkspace.builder().id(workspaceId).ocrStatus("failed").build());
        }
    }

    @Override
    public OcrResult processSync(String filePath, String engine) {
        // 同步处理，用于测试
        return null;
    }
}
```

**Step 3: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/OcrProcessingService.java nexusarchive-java/src/main/java/com/nexusarchive/service/impl/OcrProcessingServiceImpl.java
git commit -m "feat(scan): add OcrProcessingService with mock implementation"
```

---

## Task 7: 前端 - API 客户端

**目标:** 创建扫描工作区的前端 API 客户端

**Files:**
- Create: `src/api/scan.ts`

**Step 1: 创建 API 客户端**

```typescript
// Input: 扫描工作区相关 API
// Output: ScanWorkspaceApi
// Pos: src/api/scan.ts

import { request } from './request';

export interface ScanWorkspaceItem {
  id: number;
  sessionId: string;
  userId: number;
  fileName: string;
  filePath: string;
  fileSize: number;
  fileType: string;
  uploadSource: 'upload' | 'monitor' | 'mobile';
  ocrStatus: 'pending' | 'processing' | 'review' | 'completed' | 'failed';
  ocrEngine?: string;
  ocrResult?: any;
  overallScore?: number;
  docType?: string;
  submitStatus: 'draft' | 'submitted';
  archiveId?: number;
  submittedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface OcrField {
  name: string;
  value: string;
  confidence: number;
}

export interface OcrResponse {
  invoiceNo?: string;
  amount?: string;
  date?: string;
  sellerName?: string;
  buyerName?: string;
  [key: string]: string | undefined;
}

export const scanApi = {
  // 获取工作区列表
  getWorkspace: () =>
    request.get<ScanWorkspaceItem[]>('/api/scan/workspace'),

  // 上传文件
  upload: (file: File, source: 'upload' | 'monitor' | 'mobile' = 'upload', sessionId?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (source) formData.append('source', source);
    if (sessionId) formData.append('sessionId', sessionId);
    return request.post<ScanWorkspaceItem>('/api/scan/workspace/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },

  // 触发 OCR
  processOcr: (id: number) =>
    request.post(`/api/scan/workspace/${id}/ocr`),

  // 更新工作区项
  update: (id: number, data: Partial<ScanWorkspaceItem>) =>
    request.put(`/api/scan/workspace/${id}`, data),

  // 提交到预归档池
  submit: (id: number) =>
    request.post<{ archiveId: number }>(`/api/scan/workspace/${id}/submit`),

  // 删除工作区项
  delete: (id: number) =>
    request.delete(`/api/scan/workspace/${id}`),

  // 创建移动端会话
  createSession: () =>
    request.post<{ sessionId: string }>('/api/scan/mobile/session'),
};

export default scanApi;
```

**Step 2: 提交**

```bash
git add src/api/scan.ts
git commit -m "feat(scan): add scan workspace API client"
```

---

## Task 8: 前端 - 更新 OCRProcessingView 组件

**目标:** 将现有 OCRProcessingView 组件改造为对接真实 API

**Files:**
- Modify: `src/pages/pre-archive/OCRProcessingView.tsx`

**Step 1: 添加 API 调用逻辑**

在文件顶部添加导入：

```typescript
import { scanApi, type ScanWorkspaceItem, type OcrField } from '../../api/scan';
```

**Step 2: 替换状态管理和数据加载**

找到组件内的状态定义（约第 38-42 行），替换为：

```typescript
export const OCRProcessingView: React.FC = () => {
  // 从 API 加载任务列表
  const [taskList, setTaskList] = useState<ScanWorkspaceItem[]>([]);
  const [activeTask, setActiveTask] = useState<ScanWorkspaceItem | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 加载工作区数据
  useEffect(() => {
    loadWorkspace();
    // 轮询更新处理中的任务
    const interval = setInterval(loadWorkspace, 5000);
    return () => clearInterval(interval);
  }, []);

  const loadWorkspace = async () => {
    try {
      const response = await scanApi.getWorkspace();
      setTaskList(response.data || []);
    } catch (error) {
      console.error('加载工作区失败:', error);
    }
  };

  // Auto-save State (保持原有逻辑)
  const [lastSavedTime, setLastSavedTime] = useState<string | null>(null);
  const [isAutoSaving, setIsAutoSaving] = useState(false);

  // ... 其余代码保持不变，只更新 handleFileUpload 等方法
```

**Step 3: 更新文件上传处理**

替换 handleFileUpload 函数（约第 91-139 行）：

```typescript
  const handleFileUpload = async (file: File) => {
    try {
      setIsLoading(true);
      const response = await scanApi.upload(file, 'upload');

      const newTask = response.data;
      setTaskList(prev => [newTask, ...prev]);
      setActiveTask(newTask);

      // 自动触发 OCR
      await scanApi.processOcr(newTask.id);

      // 轮询等待 OCR 完成
      pollOcrStatus(newTask.id);
    } catch (error) {
      console.error('上传失败:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const pollOcrStatus = async (id: number, maxAttempts = 30) => {
    for (let i = 0; i < maxAttempts; i++) {
      await new Promise(resolve => setTimeout(resolve, 2000));

      const response = await scanApi.getWorkspace();
      const updatedTask = response.data.find(t => t.id === id);

      if (updatedTask && updatedTask.ocrStatus !== 'processing' && updatedTask.ocrStatus !== 'pending') {
        setTaskList(response.data);
        setActiveTask(updatedTask);
        return;
      }
    }
  };
```

**Step 4: 更新字段保存逻辑**

替换 handleFieldChange 函数（约第 150-160 行）：

```typescript
  const handleFieldChange = async (index: number, value: string) => {
    if (!activeTask) return;

    const newFields = [...(activeTask.ocrResult?.fields || [])];
    newFields[index] = { ...newFields[index], value };

    const updatedTask = {
      ...activeTask,
      ocrResult: { ...activeTask.ocrResult, fields: newFields }
    };

    setActiveTask(updatedTask);

    // 自动保存到后端
    try {
      await scanApi.update(activeTask.id, { ocrResult: updatedTask.ocrResult });
      setTaskList(prev => prev.map(t => t.id === activeTask.id ? updatedTask : t));
    } catch (error) {
      console.error('保存失败:', error);
    }
  };
```

**Step 5: 更新提交按钮逻辑**

找到"确认并归档"按钮（约第 389-394 行），替换为：

```typescript
  const handleSubmitToArchive = async () => {
    if (!activeTask) return;

    try {
      const response = await scanApi.submit(activeTask.id);
      // 从列表中移除已提交的项
      setTaskList(prev => prev.filter(t => t.id !== activeTask.id));
      setActiveTask(null);
      toast.success('已提交到预归档池');
    } catch (error) {
      console.error('提交失败:', error);
      toast.error('提交失败');
    }
  };

  // 在按钮的 onClick 中调用 handleSubmitToArchive
```

**Step 6: 提交**

```bash
git add src/pages/pre-archive/OCRProcessingView.tsx
git commit -m "feat(scan): integrate OCRProcessingView with scan API"
```

---

## Task 9: 前端 - 移动端扫码功能

**目标:** 添加移动端扫码二维码和会话管理

**Files:**
- Modify: `src/pages/pre-archive/OCRProcessingView.tsx`

**Step 1: 添加二维码对话框状态**

在组件内添加状态：

```typescript
  const [isQrModalOpen, setIsQrModalOpen] = useState(false);
  const [qrSessionId, setQrSessionId] = useState<string | null>(null);
```

**Step 2: 添加生成二维码功能**

```typescript
  const handleOpenMobileScan = async () => {
    try {
      const response = await scanApi.createSession();
      setQrSessionId(response.data.sessionId);
      setIsQrModalOpen(true);
    } catch (error) {
      console.error('创建会话失败:', error);
      toast.error('创建会话失败');
    }
  };

  // 构建 QR 码 URL
  const qrCodeUrl = qrSessionId
    ? `${window.location.origin}/mobile/scan?session=${qrSessionId}`
    : '';
```

**Step 3: 添加二维码对话框UI**

在上传区域下方添加按钮：

```typescript
{/* 移动端扫码按钮 */}
<div className="p-4 border-b border-slate-100">
  <button
    onClick={handleOpenMobileScan}
    className="w-full flex items-center justify-center gap-2 py-3 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-xl font-medium hover:from-blue-600 hover:to-purple-700 transition-all shadow-lg"
  >
    <span className="text-2xl">📱</span>
    <span>手机扫码上传</span>
  </button>
</div>
```

添加二维码模态框：

```typescript
{/* 二维码模态框 */}
{isQrModalOpen && (
  <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => setIsQrModalOpen(false)}>
    <div className="bg-white rounded-2xl p-8 max-w-sm mx-4" onClick={e => e.stopPropagation()}>
      <h3 className="text-xl font-bold text-center mb-4">手机扫码上传</h3>
      <div className="bg-slate-100 p-4 rounded-xl flex items-center justify-center mb-4">
        {qrCodeUrl && (
          <QRCodeSVG
            value={qrCodeUrl}
            size={200}
            level="M"
          />
        )}
      </div>
      <p className="text-sm text-slate-500 text-center mb-4">
        使用手机相机扫描二维码，即可直接拍照上传文件
      </p>
      <button
        onClick={() => setIsQrModalOpen(false)}
        className="w-full py-2 bg-slate-200 text-slate-700 rounded-lg font-medium hover:bg-slate-300"
      >
        关闭
      </button>
    </div>
  </div>
)}
```

**Step 4: 安装 QR Code 库**

```bash
npm install qrcode.react
npm install @types/qrcode.react --save-dev
```

**Step 5: 添加导入**

```typescript
import { QRCodeSVG } from 'qrcode.react';
```

**Step 6: 提交**

```bash
git add src/pages/pre-archive/OCRProcessingView.tsx package.json package-lock.json
git commit -m "feat(scan): add mobile scan QR code feature"
```

---

## Task 10: 前端 - 监控文件夹设置

**目标:** 添加监控文件夹配置功能

**Files:**
- Create: `src/components/scan/FolderMonitorDialog.tsx`
- Modify: `src/pages/pre-archive/OCRProcessingView.tsx`

**Step 1: 创建监控文件夹对话框组件**

```typescript
// Input: React、lucide-react、scanApi
// Output: FolderMonitorDialog 组件
// Pos: src/components/scan/FolderMonitorDialog.tsx

import React, { useState, useEffect } from 'react';
import { X, FolderOpen, Plus, Trash2, CheckCircle } from 'lucide-react';
import { scanApi } from '../../api/scan';

interface FolderMonitor {
  id: number;
  folderPath: string;
  isActive: boolean;
  fileFilter: string;
  autoDelete: boolean;
}

interface FolderMonitorDialogProps {
  open: boolean;
  onClose: () => void;
}

export const FolderMonitorDialog: React.FC<FolderMonitorDialogProps> = ({ open, onClose }) => {
  const [monitors, setMonitors] = useState<FolderMonitor[]>([]);
  const [newPath, setNewPath] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (open) {
      // TODO: 从后端加载监控配置
      setMonitors([]);
    }
  }, [open]);

  const handleAdd = async () => {
    if (!newPath.trim()) return;

    setIsSaving(true);
    try {
      // TODO: 调用后端 API 添加监控
      setMonitors(prev => [...prev, {
        id: Date.now(),
        folderPath: newPath,
        isActive: true,
        fileFilter: '*.pdf;*.jpg;*.png',
        autoDelete: false
      }]);
      setNewPath('');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = (id: number) => {
    setMonitors(prev => prev.filter(m => m.id !== id));
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl w-full max-w-lg mx-4 max-h-[80vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-6 border-b flex justify-between items-center">
          <h2 className="text-xl font-bold flex items-center gap-2">
            <FolderOpen className="text-primary-600" />
            监控文件夹设置
          </h2>
          <button onClick={onClose} className="p-2 hover:bg-slate-100 rounded-lg">
            <X size={20} />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 flex-1 overflow-y-auto">
          <div className="mb-6">
            <label className="block text-sm font-medium text-slate-700 mb-2">
              添加监控文件夹
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={newPath}
                onChange={e => setNewPath(e.target.value)}
                placeholder="例如: /home/user/Documents/Scan"
                className="flex-1 px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
              />
              <button
                onClick={handleAdd}
                disabled={isSaving || !newPath.trim()}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 disabled:opacity-50 flex items-center gap-2"
              >
                <Plus size={16} />
                添加
              </button>
            </div>
            <p className="text-xs text-slate-500 mt-2">
              注意：此路径为服务器上的文件系统路径，请确保后端服务有访问权限
            </p>
          </div>

          {/* 监控列表 */}
          <div className="space-y-3">
            {monitors.map(monitor => (
              <div key={monitor.id} className="flex items-center justify-between p-4 bg-slate-50 rounded-lg">
                <div className="flex items-center gap-3">
                  {monitor.isActive ? (
                    <CheckCircle size={20} className="text-emerald-500" />
                  ) : (
                    <div className="w-5 h-5 rounded-full border-2 border-slate-300" />
                  )}
                  <div>
                    <p className="font-medium text-slate-800">{monitor.folderPath}</p>
                    <p className="text-xs text-slate-500">
                      过滤器: {monitor.fileFilter}
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => handleDelete(monitor.id)}
                  className="p-2 text-slate-400 hover:text-rose-500 hover:bg-rose-50 rounded-lg"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}

            {monitors.length === 0 && (
              <div className="text-center py-8 text-slate-400">
                <FolderOpen size={48} className="mx-auto mb-3 opacity-50" />
                <p>暂无监控文件夹</p>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t bg-slate-50 flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg font-medium"
          >
            取消
          </button>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700"
          >
            确定
          </button>
        </div>
      </div>
    </div>
  );
};

export default FolderMonitorDialog;
```

**Step 2: 在 OCRProcessingView 中集成**

添加状态和导入：

```typescript
import { FolderMonitorDialog } from '../../components/scan/FolderMonitorDialog';

const [isFolderMonitorOpen, setIsFolderMonitorOpen] = useState(false);
```

在上传区域添加按钮：

```typescript
<button
  onClick={() => setIsFolderMonitorOpen(true)}
  className="w-full flex items-center justify-center gap-2 py-3 border-2 border-dashed border-slate-300 rounded-xl hover:border-primary-400 hover:bg-slate-50 transition-all text-slate-600"
>
  <span className="text-2xl">📁</span>
  <span>监控文件夹设置</span>
</button>
```

添加对话框组件：

```typescript
<FolderMonitorDialog
  open={isFolderMonitorOpen}
  onClose={() => setIsFolderMonitorOpen(false)}
/>
```

**Step 3: 提交**

```bash
git add src/components/scan/FolderMonitorDialog.tsx src/pages/pre-archive/OCRProcessingView.tsx
git commit -m "feat(scan): add folder monitor dialog"
```

---

## Task 11: 后端 - 文件监控服务

**目标:** 实现文件夹监控服务（Phase 2 可选功能）

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/FileMonitorService.java`

**Step 1: 创建文件监控服务**

```java
package com.nexusarchive.service;

import com.nexusarchive.entity.ScanFolderMonitor;
import com.nexusarchive.mapper.ScanFolderMonitorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件夹监控服务
 * Pos: nexusarchive-java/src/main/java/com/nexusarchive/service/FileMonitorService.java
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileMonitorService {

    private final ScanFolderMonitorMapper folderMonitorMapper;
    private final ScanWorkspaceService scanWorkspaceService;

    private final ConcurrentHashMap<Long, WatchKey> watchKeys = new ConcurrentHashMap<>();

    /**
     * 定期检查并重新加载监控配置
     */
    @Scheduled(fixedDelay = 60000) // 每分钟检查一次
    public void reloadMonitors() {
        List<ScanFolderMonitor> monitors = folderMonitorMapper.selectList(null);

        for (ScanFolderMonitor monitor : monitors) {
            if (!monitor.getIsActive()) continue;

            try {
                registerMonitor(monitor);
            } catch (Exception e) {
                log.error("注册监控失败: {}", monitor.getFolderPath(), e);
            }
        }
    }

    private void registerMonitor(ScanFolderMonitor monitor) {
        Path folderPath = Paths.get(monitor.getFolderPath());

        if (!Files.exists(folderPath)) {
            log.warn("监控文件夹不存在: {}", monitor.getFolderPath());
            return;
        }

        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            folderPath.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE);

            // 在实际应用中，需要将 WatchService 保存并在单独的线程中处理事件
            log.info("已注册监控: {}", monitor.getFolderPath());

        } catch (Exception e) {
            log.error("注册 WatchService 失败", e);
        }
    }

    /**
     * 处理新文件
     */
    public void handleNewFile(Path filePath, Long userId) {
        try {
            // 调用 ScanWorkspaceService 上传文件
            // scanWorkspaceService.uploadToWorkspace(file, "monitor", null, userId);
            log.info("处理监控文件: {}", filePath);
        } catch (Exception e) {
            log.error("处理监控文件失败", e);
        }
    }
}
```

**注意**: 完整的文件监控实现较复杂，需要维护 WatchService 和事件处理线程。此版本为基础框架。

**Step 2: 提交**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/FileMonitorService.java
git commit -m "feat(scan): add FileMonitorService (basic implementation)"
```

---

## Task 12: 测试与验证

**目标:** 端到端测试扫描集成功能

**Step 1: 启动后端服务**

```bash
cd nexusarchive-java
mvn spring-boot:run
```

预期: 后端启动成功，数据库表已创建

**Step 2: 启动前端服务**

```bash
npm run dev
```

预期: 前端启动成功，访问 http://localhost:15175

**Step 3: 测试文件上传流程**

1. 访问 http://localhost:15175/system/collection/scan
2. 拖拽/选择一个测试图片上传
3. 观察 OCR 处理状态变化
4. 编辑识别结果
5. 点击"确认并归档"

**Step 4: 测试移动端扫码**

1. 点击"手机扫码上传"按钮
2. 查看生成的二维码
3. 用手机扫码（暂时只验证二维码生成）

**Step 5: 测试监控文件夹**

1. 点击"监控文件夹设置"
2. 添加一个测试路径
3. 验证配置保存

**Step 6: 检查数据库**

```sql
SELECT * FROM scan_workspace ORDER BY created_at DESC LIMIT 10;
```

预期: 看到上传的工作区记录

---

## 实施优先级

| Phase | 任务 | 优先级 | 预计时间 |
|-------|------|--------|----------|
| **Phase 1** | Task 1-2: 路由 + 数据表 | P0 | 30分钟 |
| **Phase 1** | Task 3-5: 实体 + Controller + Service | P0 | 1小时 |
| **Phase 1** | Task 6: OCR 服务（Mock） | P0 | 30分钟 |
| **Phase 1** | Task 7-8: API 客户端 + 前端集成 | P0 | 1小时 |
| **Phase 2** | Task 9: 移动端扫码 | P1 | 30分钟 |
| **Phase 2** | Task 10-11: 监控文件夹 | P1 | 1小时 |
| **Phase 3** | Task 12: 测试验证 | P0 | 30分钟 |

**总计:** 约 5-6 小时

---

## 相关文档

- OCR 服务设计: `docs/plans/2026-01-06-ocr-service-design.md`
- 现有 OCR 组件: `src/pages/pre-archive/OCRProcessingView.tsx`
- 前端路由: `src/routes/index.tsx`
- 后端 Controller: `nexusarchive-java/src/main/java/com/nexusarchive/controller/`

---

## 下一步

计划完成后，使用以下方式之一执行：

**方式 1: 当前会话 - 子代理驱动**
```bash
# 使用 superpowers:subagent-driven-development skill
```

**方式 2: 新会话 - 批量执行**
```bash
# 使用 superpowers:executing-plans skill
```
