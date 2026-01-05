# 批量上传功能 (Batch Upload) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 构建符合电子会计档案国家标准 (GB/T 39362-2020) 的批量文件上传功能，支持多文件并发上传、进度跟踪、四性检测、元数据管理。

**Architecture:**
- 前端采用 React + TypeScript + Ant Design，实现拖拽上传、进度跟踪、实时状态反馈
- 后端采用 Spring Boot + MultipartFile，支持分块上传、异步处理、幂等性控制
- 数据库新增 `collection_batch` 表管理上传批次，复用现有 `arc_file_content` 表存储文件记录
- 复用现有四性检测服务 (`FourNatureCheckService`) 和审计日志 (`AuditLogService`)

**Tech Stack:**
- Frontend: React 19, TypeScript 5.8, Ant Design 6, React Query
- Backend: Spring Boot 3.1.6, Java 17, MyBatis-Plus 3.5.7
- Storage: 文件系统存储 + PostgreSQL 元数据
- Compliance: 四性检测 (真实性、完整性、可用性、安全性)

---

## Table of Contents

1. [Phase 1: Database Schema](#phase-1-database-schema)
2. [Phase 2: Backend API](#phase-2-backend-api)
3. [Phase 3: Frontend Components](#phase-3-frontend-components)
4. [Phase 4: Integration & Testing](#phase-4-integration--testing)

---

## Phase 1: Database Schema

### Task 1.1: Create collection_batch table migration

**Files:**
- Create: `nexusarchive-java/src/main/resources/db/migration/V80__create_collection_batch.sql`

**Step 1: Write the migration SQL**

```sql
-- ============================================================
-- 资料收集批次表 (Collection Batch)
-- 符合 GB/T 39362-2020 电子会计档案管理系统建设要求
-- ============================================================

CREATE TABLE public.collection_batch (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 批次标识
    batch_no VARCHAR(50) NOT NULL UNIQUE,
    batch_name VARCHAR(200) NOT NULL,

    -- 全宗信息 (会计档案归属)
    fonds_id BIGINT NOT NULL,
    fonds_code VARCHAR(20) NOT NULL,

    -- 会计期间
    fiscal_year VARCHAR(10) NOT NULL,
    fiscal_period VARCHAR(20),

    -- 档案门类 (凭证/账簿/报告/其他)
    archival_category VARCHAR(50) NOT NULL,

    -- 来源渠道 (WEB上传/邮箱导入/银企直联/ERP集成)
    source_channel VARCHAR(50) NOT NULL DEFAULT 'WEB上传',

    -- 批次状态
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',
    -- UPLOADING: 上传中
    -- UPLOADED: 上传完成
    -- VALIDATING: 校验中
    -- VALIDATED: 校验完成
    -- FAILED: 上传/校验失败
    -- ARCHIVED: 已归档

    -- 统计信息
    total_files INTEGER NOT NULL DEFAULT 0,
    uploaded_files INTEGER NOT NULL DEFAULT 0,
    failed_files INTEGER NOT NULL DEFAULT 0,
    total_size_bytes BIGINT NOT NULL DEFAULT 0,

    -- 校验结果 (JSON 格式存储四性检测汇总)
    validation_report JSONB,

    -- 错误信息
    error_message TEXT,

    -- 审计字段
    created_by BIGINT NOT NULL,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_time TIMESTAMP,

    -- 索引优化
    CONSTRAINT chk_collection_batch_status
        CHECK (status IN ('UPLOADING', 'UPLOADED', 'VALIDATING', 'VALIDATED', 'FAILED', 'ARCHIVED'))
);

-- 评论
COMMENT ON TABLE public.collection_batch IS '资料收集批次表 - 管理批量上传会话';
COMMENT ON COLUMN public.collection_batch.batch_no IS '批次编号 (格式: COL-YYYYMMDD-NNN)';
COMMENT ON COLUMN public.collection_batch.batch_name IS '批次名称';
COMMENT ON COLUMN public.collection_batch.fonds_id IS '全宗ID';
COMMENT ON COLUMN public.collection_batch.fonds_code IS '全宗代码';
COMMENT ON COLUMN public.collection_batch.fiscal_year IS '会计年度';
COMMENT ON COLUMN public.collection_batch.fiscal_period IS '会计期间';
COMMENT ON COLUMN public.collection_batch.archival_category IS '档案门类 (VOUCHER/LEDGER/REPORT/OTHER)';
COMMENT ON COLUMN public.collection_batch.source_channel IS '来源渠道';
COMMENT ON COLUMN public.collection_batch.status IS '批次状态';
COMMENT ON COLUMN public.collection_batch.validation_report IS '四性检测汇总报告 (JSONB)';

-- 索引
CREATE INDEX idx_collection_batch_fonds ON public.collection_batch(fonds_id);
CREATE INDEX idx_collection_batch_status ON public.collection_batch(status);
CREATE INDEX idx_collection_batch_created_time ON public.collection_batch(created_time DESC);
CREATE INDEX idx_collection_batch_fiscal_year ON public.collection_batch(fiscal_year);
```

**Step 2: Verify the SQL syntax**

Run: `psql -h localhost -U nexusarchive -d nexusarchive -c "\dT+ collection_batch"`
Expected: Table does not exist yet (will create on migration)

**Step 3: Save the migration file**

Run: `ls -la nexusarchive-java/src/main/resources/db/migration/ | tail -5`
Expected: `V80__create_collection_batch.sql` is listed

**Step 4: Commit**

```bash
git add nexusarchive-java/src/main/resources/db/migration/V80__create_collection_batch.sql
git commit -m "feat(db): add collection_batch table for batch upload management"
```

---

### Task 1.2: Create collection_batch_file table migration

**Files:**
- Create: `nexusarchive-java/src/main/resources/db/migration/V81__create_collection_batch_file.sql`

**Step 1: Write the migration SQL**

```sql
-- ============================================================
-- 资料收集批次文件表 (Collection Batch File)
-- 记录批次内每个文件的上传状态和处理结果
-- ============================================================

CREATE TABLE public.collection_batch_file (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 批次关联
    batch_id BIGINT NOT NULL REFERENCES public.collection_batch(id) ON DELETE CASCADE,

    -- 文件标识 (关联到 arc_file_content)
    file_id VARCHAR(50), -- 上传成功后关联到 arc_file_content.id

    -- 原始文件信息
    original_filename VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    file_type VARCHAR(20), -- PDF/OFD/XML/JPG/PNG

    -- 文件哈希 (幂等性控制)
    file_hash VARCHAR(128),
    hash_algorithm VARCHAR(20) DEFAULT 'SHA-256',

    -- 上传状态
    upload_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING: 等待上传
    -- UPLOADING: 上传中
    -- UPLOADED: 上传成功
    -- FAILED: 上传失败
    -- DUPLICATE: 重复文件
    -- VALIDATING: 校验中
    -- VALIDATED: 校验完成
    -- CHECK_FAILED: 四性检测失败

    -- 处理结果 (JSON 格式)
    processing_result JSONB,

    -- 错误信息
    error_message TEXT,

    -- 上传顺序
    upload_order INTEGER NOT NULL,

    -- 时间戳
    started_time TIMESTAMP,
    completed_time TIMESTAMP,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 约束
    CONSTRAINT chk_upload_status
        CHECK (upload_status IN ('PENDING', 'UPLOADING', 'UPLOADED', 'FAILED', 'DUPLICATE', 'VALIDATING', 'VALIDATED', 'CHECK_FAILED'))
);

-- 评论
COMMENT ON TABLE public.collection_batch_file IS '资料收集批次文件表 - 记录批次内每个文件的上传和处理状态';
COMMENT ON COLUMN public.collection_batch_file.batch_id IS '所属批次ID';
COMMENT ON COLUMN public.collection_batch_file.file_id IS '关联的文件ID (arc_file_content.id)';
COMMENT ON COLUMN public.collection_batch_file.original_filename IS '原始文件名';
COMMENT ON COLUMN public.collection_batch_file.file_hash IS '文件哈希值 (用于幂等性控制)';
COMMENT ON COLUMN public.collection_batch_file.upload_status IS '上传状态';
COMMENT ON COLUMN public.collection_batch_file.processing_result IS '处理结果 (包含四性检测报告)';

-- 索引
CREATE INDEX idx_collection_batch_file_batch_id ON public.collection_batch_file(batch_id);
CREATE INDEX idx_collection_batch_file_file_id ON public.collection_batch_file(file_id);
CREATE INDEX idx_collection_batch_file_status ON public.collection_batch_file(upload_status);
CREATE INDEX idx_collection_batch_file_hash ON public.collection_batch_file(file_hash);

-- 唯一约束 (同一批次内文件名唯一)
CREATE UNIQUE INDEX idx_collection_batch_file_batch_name
    ON public.collection_batch_file(batch_id, original_filename)
    WHERE upload_status NOT IN ('FAILED', 'DUPLICATE');
```

**Step 2: Save and commit**

```bash
git add nexusarchive-java/src/main/resources/db/migration/V81__create_collection_batch_file.sql
git commit -m "feat(db): add collection_batch_file table for batch file tracking"
```

---

## Phase 2: Backend API

### Task 2.1: Create CollectionBatch entity

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/CollectionBatch.java`

**Step 1: Write the entity class**

```java
// Input: MyBatis-Plus, Lombok, Java Standard Library
// Output: CollectionBatch Entity
// Pos: Domain Entity

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 资料收集批次实体
 *
 * 符合 GB/T 39362-2020 电子会计档案管理规范
 * 管理批量上传会话的完整生命周期
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collection_batch")
public class CollectionBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次编号，格式: COL-YYYYMMDD-NNN
     */
    private String batchNo;

    /**
     * 批次名称
     */
    private String batchName;

    /**
     * 全宗ID
     */
    private Long fondsId;

    /**
     * 全宗代码
     */
    private String fondsCode;

    /**
     * 会计年度
     */
    private String fiscalYear;

    /**
     * 会计期间
     */
    private String fiscalPeriod;

    /**
     * 档案门类: VOUCHER/LEDGER/REPORT/OTHER
     */
    private String archivalCategory;

    /**
     * 来源渠道
     */
    private String sourceChannel;

    /**
     * 批次状态
     */
    private String status;

    /**
     * 总文件数
     */
    private Integer totalFiles;

    /**
     * 已上传文件数
     */
    private Integer uploadedFiles;

    /**
     * 失败文件数
     */
    private Integer failedFiles;

    /**
     * 总大小(字节)
     */
    private Long totalSizeBytes;

    /**
     * 校验报告(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> validationReport;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 修改时间
     */
    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    // ========== 状态常量 ==========

    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_UPLOADED = "UPLOADED";
    public static final String STATUS_VALIDATING = "VALIDATING";
    public static final String STATUS_VALIDATED = "VALIDATED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    public static final String CATEGORY_VOUCHER = "VOUCHER";
    public static final String CATEGORY_LEDGER = "LEDGER";
    public static final String CATEGORY_REPORT = "REPORT";
    public static final String CATEGORY_OTHER = "OTHER";

    // ========== 便捷方法 ==========

    public boolean isUploading() {
        return STATUS_UPLOADING.equals(status);
    }

    public boolean isUploaded() {
        return STATUS_UPLOADED.equals(status);
    }

    public boolean isCompleted() {
        return STATUS_VALIDATED.equals(status) || STATUS_ARCHIVED.equals(status);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    public boolean canUpload() {
        return STATUS_UPLOADING.equals(status);
    }

    public int getProgress() {
        if (totalFiles == null || totalFiles == 0) return 0;
        int uploaded = uploadedFiles != null ? uploadedFiles : 0;
        return (int) ((uploaded * 100L) / totalFiles);
    }
}
```

**Step 2: Save and commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/entity/CollectionBatch.java
git commit -m "feat(entity): add CollectionBatch entity"
```

---

### Task 2.2: Create CollectionBatchFile entity

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/CollectionBatchFile.java`

**Step 1: Write the entity class**

```java
// Input: MyBatis-Plus, Lombok, Java Standard Library
// Output: CollectionBatchFile Entity
// Pos: Domain Entity

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 资料收集批次文件实体
 *
 * 记录批次内每个文件的上传状态和处理结果
 * 支持幂等性控制 (通过文件哈希去重)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collection_batch_file")
public class CollectionBatchFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属批次ID
     */
    private Long batchId;

    /**
     * 关联的文件ID (上传成功后关联到 arc_file_content.id)
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件大小(字节)
     */
    private Long fileSizeBytes;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件哈希值
     */
    private String fileHash;

    /**
     * 哈希算法
     */
    private String hashAlgorithm;

    /**
     * 上传状态
     */
    private String uploadStatus;

    /**
     * 处理结果(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> processingResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 上传顺序
     */
    private Integer uploadOrder;

    /**
     * 开始时间
     */
    private LocalDateTime startedTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    // ========== 状态常量 ==========

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_UPLOADED = "UPLOADED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DUPLICATE = "DUPLICATE";
    public static final String STATUS_VALIDATING = "VALIDATING";
    public static final String STATUS_VALIDATED = "VALIDATED";
    public static final String STATUS_CHECK_FAILED = "CHECK_FAILED";

    // ========== 便捷方法 ==========

    public boolean isPending() {
        return STATUS_PENDING.equals(uploadStatus);
    }

    public boolean isUploaded() {
        return STATUS_UPLOADED.equals(uploadStatus);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equals(uploadStatus) ||
               STATUS_DUPLICATE.equals(uploadStatus) ||
               STATUS_CHECK_FAILED.equals(uploadStatus);
    }

    public boolean isCompleted() {
        return isUploaded() || isFailed();
    }

    public long getDurationMillis() {
        if (startedTime == null || completedTime == null) return 0;
        return java.time.Duration.between(startedTime, completedTime).toMillis();
    }
}
```

**Step 2: Save and commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/entity/CollectionBatchFile.java
git commit -m "feat(entity): add CollectionBatchFile entity"
```

---

### Task 2.3: Create CollectionBatchMapper

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/CollectionBatchMapper.java`

**Step 1: Write the mapper interface**

```java
// Input: MyBatis-Plus
// Output: CollectionBatchMapper Interface
// Pos: Data Access Layer

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.CollectionBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资料收集批次 Mapper
 */
@Mapper
public interface CollectionBatchMapper extends BaseMapper<CollectionBatch> {

    /**
     * 查询用户的批次列表
     */
    @Select("SELECT * FROM collection_batch " +
            "WHERE created_by = #{userId} " +
            "ORDER BY created_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<CollectionBatch> findByUserId(
        @Param("userId") Long userId,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 更新批次统计信息
     */
    @Update("UPDATE collection_batch " +
            "SET uploaded_files = (" +
            "   SELECT COUNT(*) FROM collection_batch_file " +
            "   WHERE batch_id = #{batchId} AND upload_status IN ('UPLOADED', 'VALIDATED')" +
            "), " +
            "failed_files = (" +
            "   SELECT COUNT(*) FROM collection_batch_file " +
            "   WHERE batch_id = #{batchId} AND upload_status IN ('FAILED', 'DUPLICATE', 'CHECK_FAILED')" +
            "), " +
            "total_size_bytes = (" +
            "   SELECT COALESCE(SUM(file_size_bytes), 0) FROM collection_batch_file " +
            "   WHERE batch_id = #{batchId}" +
            "), " +
            "last_modified_time = CURRENT_TIMESTAMP " +
            "WHERE id = #{batchId}")
    int updateStatistics(@Param("batchId") Long batchId);

    /**
     * 更新批次状态
     */
    @Update("UPDATE collection_batch " +
            "SET status = #{status}, " +
            "last_modified_time = CURRENT_TIMESTAMP " +
            "#{completedTimeClause}, " +
            "error_message = #{errorMessage} " +
            "WHERE id = #{batchId}")
    int updateStatus(
        @Param("batchId") Long batchId,
        @Param("status") String status,
        @Param("completedTimeClause") String completedTimeClause,
        @Param("errorMessage") String errorMessage
    );
}
```

**Step 2: Save and commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/mapper/CollectionBatchMapper.java
git commit -m "feat(mapper): add CollectionBatchMapper"
```

---

### Task 2.4: Create CollectionBatchFileMapper

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/CollectionBatchFileMapper.java`

**Step 1: Write the mapper interface**

```java
// Input: MyBatis-Plus
// Output: CollectionBatchFileMapper Interface
// Pos: Data Access Layer

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.CollectionBatchFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资料收集批次文件 Mapper
 */
@Mapper
public interface CollectionBatchFileMapper extends BaseMapper<CollectionBatchFile> {

    /**
     * 查询批次的文件列表
     */
    @Select("SELECT * FROM collection_batch_file " +
            "WHERE batch_id = #{batchId} " +
            "ORDER BY upload_order ASC")
    List<CollectionBatchFile> findByBatchId(@Param("batchId") Long batchId);

    /**
     * 查询待处理的文件
     */
    @Select("SELECT * FROM collection_batch_file " +
            "WHERE batch_id = #{batchId} " +
            "AND upload_status IN ('PENDING', 'UPLOADING') " +
            "ORDER BY upload_order ASC " +
            "LIMIT #{limit}")
    List<CollectionBatchFile> findPendingFiles(
        @Param("batchId") Long batchId,
        @Param("limit") int limit
    );

    /**
     * 统计批次各状态文件数
     */
    @Select("SELECT upload_status, COUNT(*) as count " +
            "FROM collection_batch_file " +
            "WHERE batch_id = #{batchId} " +
            "GROUP BY upload_status")
    List<java.util.Map<String, Object>> getStatusStats(@Param("batchId") Long batchId);

    /**
     * 检查文件哈希是否已存在 (幂等性)
     */
    @Select("SELECT cbf.* FROM collection_batch_file cbf " +
            "INNER JOIN collection_batch cb ON cbf.batch_id = cb.id " +
            "WHERE cbf.file_hash = #{fileHash} " +
            "AND cb.fonds_id = #{fondsId} " +
            "AND cb.fiscal_year = #{fiscalYear} " +
            "AND cbf.upload_status IN ('UPLOADED', 'VALIDATED') " +
            "LIMIT 1")
    CollectionBatchFile findDuplicateByHash(
        @Param("fileHash") String fileHash,
        @Param("fondsId") Long fondsId,
        @Param("fiscalYear") String fiscalYear
    );
}
```

**Step 2: Save and commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/mapper/CollectionBatchFileMapper.java
git commit -m "feat(mapper): add CollectionBatchFileMapper"
```

---

### Task 2.5: Create BatchUploadRequest DTO

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/BatchUploadRequest.java`

**Step 1: Write the DTO class**

```java
// Input: Jakarta Validation, Lombok
// Output: BatchUploadRequest DTO
// Pos: DTO Layer

package com.nexusarchive.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 批量上传请求 DTO
 */
@Data
public class BatchUploadRequest {

    /**
     * 批次名称
     */
    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    /**
     * 全宗代码
     */
    @NotBlank(message = "全宗代码不能为空")
    private String fondsCode;

    /**
     * 会计年度 (格式: YYYY)
     */
    @NotBlank(message = "会计年度不能为空")
    @Pattern(regexp = "^\\d{4}$", message = "会计年度格式错误，应为4位数字")
    private String fiscalYear;

    /**
     * 会计期间 (可选)
     */
    private String fiscalPeriod;

    /**
     * 档案门类
     */
    @NotBlank(message = "档案门类不能为空")
    @Pattern(regexp = "^(VOUCHER|LEDGER|REPORT|OTHER)$",
             message = "档案门类必须为 VOUCHER/LEDGER/REPORT/OTHER")
    private String archivalCategory;

    /**
     * 预计文件数量
     */
    @NotNull(message = "预计文件数量不能为空")
    @Min(value = 1, message = "文件数量至少为1")
    private Integer totalFiles;

    /**
     * 是否自动执行四性检测
     */
    private Boolean autoCheck = true;
}
```

**Step 2: Save and commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/dto/BatchUploadRequest.java
git commit -m "feat(dto): add BatchUploadRequest DTO"
```

---

### Task 2.6: Create BatchUploadResponse DTO

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/dto/BatchUploadResponse.java`

**Step 1: Write the DTO class**

```java
// Input: Lombok
// Output: BatchUploadResponse DTO
// Pos: DTO Layer

package com.nexusarchive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量上传响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResponse {

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 批次编号
     */
    private String batchNo;

    /**
     * 批次状态
     */
    private String status;

    /**
     * 上传令牌 (用于文件上传时验证权限)
     */
    private String uploadToken;

    /**
     * 预计文件数
     */
    private Integer totalFiles;

    /**
     * 已上传文件数
     */
    private Integer uploadedFiles;

    /**
     * 失败文件数
     */
    private Integer failedFiles;

    /**
     * 进度百分比
     */
    private Integer progress;

    /**
     * 文件列表 (仅返回最近上传的文件)
     */
    private List<FileInfo> recentFiles;

    /**
     * 文件信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private String originalFilename;
        private String uploadStatus;
        private Long fileSizeBytes;
        private String errorMessage;
    }
}
```

**Step 2: Save and commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/dto/BatchUploadResponse.java
git commit -m "feat(dto): add BatchUploadResponse DTO"
```

---

### Task 2.7: Create CollectionBatchService interface

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/CollectionBatchService.java`

**Step 1: Write the service interface**

```java
// Input: Java Standard Library, Local Types
// Output: CollectionBatchService Interface
// Pos: Service Layer

package com.nexusarchive.service;

import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.entity.CollectionBatch;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资料收集批次服务接口
 *
 * 负责批量上传的批次管理和协调
 */
public interface CollectionBatchService {

    /**
     * 创建上传批次
     *
     * @param request 批次创建请求
     * @param userId  创建人ID
     * @return 批次响应
     */
    BatchUploadResponse createBatch(BatchUploadRequest request, Long userId);

    /**
     * 上传单个文件到批次
     *
     * @param batchId 批次ID
     * @param file    上传的文件
     * @param userId  用户ID
     * @return 文件处理结果
     */
    FileUploadResult uploadFile(Long batchId, MultipartFile file, Long userId);

    /**
     * 完成批次上传
     *
     * @param batchId 批次ID
     * @param userId  用户ID
     * @return 完成结果
     */
    BatchCompleteResult completeBatch(Long batchId, Long userId);

    /**
     * 取消批次
     *
     * @param batchId 批次ID
     * @param userId  用户ID
     */
    void cancelBatch(Long batchId, Long userId);

    /**
     * 获取批次详情
     *
     * @param batchId 批次ID
     * @return 批次详情
     */
    BatchDetailResponse getBatchDetail(Long batchId);

    /**
     * 获取批次文件列表
     *
     * @param batchId 批次ID
     * @return 文件列表
     */
    List<BatchFileResponse> getBatchFiles(Long batchId);

    /**
     * 批量执行四性检测
     *
     * @param batchId 批次ID
     * @param userId  用户ID
     * @return 检测结果
     */
    BatchCheckResult runFourNatureCheck(Long batchId, Long userId);

    // ========== Inner DTO Classes ==========

    /**
     * 文件上传结果
     */
    record FileUploadResult(
        String fileId,
        String originalFilename,
        String status,
        String errorMessage
    ) {}

    /**
     * 批次完成结果
     */
    record BatchCompleteResult(
        Long batchId,
        String batchNo,
        String status,
        int totalFiles,
        int uploadedFiles,
        int failedFiles
    ) {}

    /**
     * 批次详情响应
     */
    record BatchDetailResponse(
        Long id,
        String batchNo,
        String batchName,
        String fondsCode,
        String fiscalYear,
        String archivalCategory,
        String status,
        int totalFiles,
        int uploadedFiles,
        int failedFiles,
        long totalSizeBytes,
        int progress
    ) {}

    /**
     * 批次文件响应
     */
    record BatchFileResponse(
        Long id,
        String fileId,
        String originalFilename,
        String uploadStatus,
        Long fileSizeBytes,
        String errorMessage
    ) {}

    /**
     * 批次检测结果
     */
    record BatchCheckResult(
        Long batchId,
        int totalFiles,
        int checkedFiles,
        int passedFiles,
        int failedFiles,
        String summary
    ) {}
}
```

**Step 2: Save and commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/CollectionBatchService.java
git commit -m "feat(service): add CollectionBatchService interface"
```

---

### Task 2.8: Create CollectionBatchServiceImpl

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java`

**Step 1: Write the service implementation (Part 1: Core methods)**

```java
// Input: Spring Framework, Lombok, Local Services
// Output: CollectionBatchServiceImpl
// Pos: Service Implementation

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.mapper.CollectionBatchMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.CollectionBatchService;
import com.nexusarchive.service.FourNatureCheckService;
import com.nexusarchive.service.PoolService;
import com.nexusarchive.util.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 资料收集批次服务实现
 *
 * 功能：
 * 1. 批次生命周期管理
 * 2. 文件上传处理 (含幂等性控制)
 * 3. 四性检测协调
 * 4. 审计日志记录
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionBatchServiceImpl implements CollectionBatchService {

    private final CollectionBatchMapper batchMapper;
    private final CollectionBatchFileMapper batchFileMapper;
    private final PoolService poolService;
    private final FourNatureCheckService fourNatureCheckService;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter BATCH_NO_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd");

    // ===== CollectionBatchService Implementation =====

    @Override
    @Transactional
    public BatchUploadResponse createBatch(BatchUploadRequest request, Long userId) {
        log.info("创建上传批次: userId={}, request={}", userId, request);

        // 1. 生成批次编号
        String batchNo = generateBatchNo();

        // 2. 创建批次记录
        CollectionBatch batch = CollectionBatch.builder()
            .batchNo(batchNo)
            .batchName(request.getBatchName())
            .fondsCode(request.getFondsCode())
            .fiscalYear(request.getFiscalYear())
            .fiscalPeriod(request.getFiscalPeriod())
            .archivalCategory(request.getArchivalCategory())
            .sourceChannel("WEB上传")
            .status(CollectionBatch.STATUS_UPLOADING)
            .totalFiles(request.getTotalFiles())
            .uploadedFiles(0)
            .failedFiles(0)
            .totalSizeBytes(0L)
            .createdBy(userId)
            .build();

        batchMapper.insert(batch);

        // 3. 记录审计日志
        auditLogService.log(
            String.valueOf(userId),
            String.valueOf(userId),
            "CREATE_BATCH",
            "COLLECTION_BATCH",
            String.valueOf(batch.getId()),
            "SUCCESS",
            "创建上传批次: " + batchNo,
            null
        );

        // 4. 返回响应
        return BatchUploadResponse.builder()
            .batchId(batch.getId())
            .batchNo(batchNo)
            .status(batch.getStatus())
            .uploadToken(generateUploadToken(batch.getId(), userId))
            .totalFiles(request.getTotalFiles())
            .uploadedFiles(0)
            .failedFiles(0)
            .progress(0)
            .build();
    }

    @Override
    @Transactional
    public FileUploadResult uploadFile(Long batchId, MultipartFile file, Long userId) {
        log.info("上传文件: batchId={}, filename={}, size={}",
                 batchId, file.getOriginalFilename(), file.getSize());

        // 1. 验证批次状态
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "批次不存在");
        }
        if (!batch.canUpload()) {
            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "批次状态不允许上传: " + batch.getStatus());
        }

        // 2. 计算文件哈希 (幂等性控制)
        String fileHash;
        try {
            fileHash = FileHashUtil.hash(file.getInputStream(), "SHA-256");
        } catch (Exception e) {
            log.error("计算文件哈希失败", e);
            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "文件处理失败: " + e.getMessage());
        }

        // 3. 检查重复文件 (同一全宗、同一年度内)
        CollectionBatchFile duplicate = batchFileMapper.findDuplicateByHash(
            fileHash, batch.getFondsId(), batch.getFiscalYear()
        );
        if (duplicate != null) {
            log.warn("检测到重复文件: hash={}", fileHash);
            return new FileUploadResult(null, file.getOriginalFilename(),
                "DUPLICATE", "文件已存在 (相同哈希值)");
        }

        // 4. 确定文件类型
        String fileType = getFileType(file.getOriginalFilename());

        // 5. 创建批次文件记录
        AtomicInteger uploadOrder = new AtomicInteger(
            batchFileMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>()
                    .eq(CollectionBatchFile::getBatchId, batchId)
            ).intValue()
        );

        CollectionBatchFile batchFile = CollectionBatchFile.builder()
            .batchId(batchId)
            .originalFilename(file.getOriginalFilename())
            .fileSizeBytes(file.getSize())
            .fileType(fileType)
            .fileHash(fileHash)
            .hashAlgorithm("SHA-256")
            .uploadStatus(CollectionBatchFile.STATUS_UPLOADING)
            .uploadOrder(uploadOrder.incrementAndGet())
            .startedTime(LocalDateTime.now())
            .build();

        batchFileMapper.insert(batchFile);

        // 6. 保存文件到存储
        String fileId;
        try {
            fileId = saveFileToStorage(file, batch, batchFile);
        } catch (Exception e) {
            log.error("保存文件失败", e);
            batchFile.setUploadStatus(CollectionBatchFile.STATUS_FAILED);
            batchFile.setErrorMessage(e.getMessage());
            batchFile.setCompletedTime(LocalDateTime.now());
            batchFileMapper.updateById(batchFile);

            // 更新批次统计
            batchMapper.updateStatistics(batchId);

            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "文件保存失败: " + e.getMessage());
        }

        // 7. 创建 arc_file_content 记录
        ArcFileContent arcFile = createArcFileContent(file, batch, batchFile, fileId, userId);
        poolService.insertFile(arcFile);

        // 8. 更新批次文件状态
        batchFile.setFileId(fileId);
        batchFile.setUploadStatus(CollectionBatchFile.STATUS_UPLOADED);
        batchFile.setCompletedTime(LocalDateTime.now());
        batchFileMapper.updateById(batchFile);

        // 9. 更新批次统计
        batchMapper.updateStatistics(batchId);

        log.info("文件上传成功: fileId={}, batchFileId={}", fileId, batchFile.getId());
        return new FileUploadResult(fileId, file.getOriginalFilename(),
            "UPLOADED", null);
    }

    @Override
    @Transactional
    public BatchCompleteResult completeBatch(Long batchId, Long userId) {
        log.info("完成批次: batchId={}, userId={}", batchId, userId);

        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        // 更新批次状态
        batch.setStatus(CollectionBatch.STATUS_UPLOADED);
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        // 记录审计日志
        auditLogService.log(
            String.valueOf(userId),
            String.valueOf(userId),
            "COMPLETE_BATCH",
            "COLLECTION_BATCH",
            String.valueOf(batchId),
            "SUCCESS",
            "完成批次上传: " + batch.getBatchNo(),
            null
        );

        return new BatchCompleteResult(
            batch.getId(),
            batch.getBatchNo(),
            batch.getStatus(),
            batch.getTotalFiles(),
            batch.getUploadedFiles(),
            batch.getFailedFiles()
        );
    }

    @Override
    @Transactional
    public void cancelBatch(Long batchId, Long userId) {
        log.info("取消批次: batchId={}, userId={}", batchId, userId);

        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        batch.setStatus(CollectionBatch.STATUS_FAILED);
        batch.setErrorMessage("用户取消");
        batch.setLastModifiedTime(LocalDateTime.now());
        batch.setCompletedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        auditLogService.log(
            String.valueOf(userId),
            String.valueOf(userId),
            "CANCEL_BATCH",
            "COLLECTION_BATCH",
            String.valueOf(batchId),
            "SUCCESS",
            "取消批次: " + batch.getBatchNo(),
            null
        );
    }

    @Override
    public BatchDetailResponse getBatchDetail(Long batchId) {
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        return new BatchDetailResponse(
            batch.getId(),
            batch.getBatchNo(),
            batch.getBatchName(),
            batch.getFondsCode(),
            batch.getFiscalYear(),
            batch.getArchivalCategory(),
            batch.getStatus(),
            batch.getTotalFiles(),
            batch.getUploadedFiles(),
            batch.getFailedFiles(),
            batch.getTotalSizeBytes(),
            batch.getProgress()
        );
    }

    @Override
    public List<BatchFileResponse> getBatchFiles(Long batchId) {
        List<CollectionBatchFile> files = batchFileMapper.findByBatchId(batchId);
        return files.stream()
            .map(f -> new BatchFileResponse(
                f.getId(),
                f.getFileId(),
                f.getOriginalFilename(),
                f.getUploadStatus(),
                f.getFileSizeBytes(),
                f.getErrorMessage()
            ))
            .toList();
    }

    @Override
    @Transactional
    public BatchCheckResult runFourNatureCheck(Long batchId, Long userId) {
        log.info("执行批次四性检测: batchId={}, userId={}", batchId, userId);

        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        // 更新批次状态
        batch.setStatus(CollectionBatch.STATUS_VALIDATING);
        batchMapper.updateById(batch);

        // 获取已上传的文件
        List<CollectionBatchFile> files = batchFileMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>()
                .eq(CollectionBatchFile::getBatchId, batchId)
                .eq(CollectionBatchFile::getUploadStatus, CollectionBatchFile.STATUS_UPLOADED)
                .orderByAsc(CollectionBatchFile::getUploadOrder)
        );

        int totalFiles = files.size();
        int passedFiles = 0;
        int failedFiles = 0;

        for (CollectionBatchFile batchFile : files) {
            if (batchFile.getFileId() != null) {
                try {
                    var report = fourNatureCheckService.checkSingleFile(batchFile.getFileId());
                    if ("PASSED".equals(report.getStatus())) {
                        passedFiles++;
                        batchFile.setUploadStatus(CollectionBatchFile.STATUS_VALIDATED);
                    } else {
                        failedFiles++;
                        batchFile.setUploadStatus(CollectionBatchFile.STATUS_CHECK_FAILED);
                    }
                    batchFileMapper.updateById(batchFile);
                } catch (Exception e) {
                    log.error("四性检测失败: fileId={}", batchFile.getFileId(), e);
                    failedFiles++;
                }
            }
        }

        // 更新批次状态
        if (failedFiles == 0) {
            batch.setStatus(CollectionBatch.STATUS_VALIDATED);
        } else {
            batch.setStatus(CollectionBatch.STATUS_UPLOADED); // 允许部分失败后重试
        }
        batchMapper.updateById(batch);

        String summary = String.format("检测完成: 共 %d 个文件，通过 %d 个，失败 %d 个",
            totalFiles, passedFiles, failedFiles);

        return new BatchCheckResult(batchId, totalFiles, totalFiles, passedFiles, failedFiles, summary);
    }

    // ===== Private Helper Methods =====

    private String generateBatchNo() {
        String datePart = LocalDateTime.now().format(BATCH_NO_FORMATTER);
        String randomPart = String.format("%03d", (int)(Math.random() * 1000));
        return "COL-" + datePart + "-" + randomPart;
    }

    private String generateUploadToken(Long batchId, Long userId) {
        // 简单的令牌生成 (生产环境应使用JWT)
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String getFileType(String filename) {
        if (filename == null) return "UNKNOWN";
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "pdf" -> "PDF";
            case "ofd" -> "OFD";
            case "xml" -> "XML";
            case "jpg", "jpeg" -> "JPG";
            case "png" -> "PNG";
            case "tif", "tiff" -> "TIFF";
            default -> "UNKNOWN";
        };
    }

    private String saveFileToStorage(MultipartFile file, CollectionBatch batch,
                                      CollectionBatchFile batchFile) throws IOException {
        // 构建存储路径: /tmp/nexusarchive/uploads/{fondsCode}/{fiscalYear}/{batchNo}/
        String uploadDir = String.format("/tmp/nexusarchive/uploads/%s/%s/%s",
            batch.getFondsCode(), batch.getFiscalYear(), batch.getBatchNo());
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        // 生成唯一文件ID
        String fileId = UUID.randomUUID().toString();
        String fileExtension = getFileExtension(batchFile.getOriginalFilename());
        String targetFileName = fileId + "." + fileExtension;
        Path targetPath = uploadPath.resolve(targetFileName);

        // 保存文件
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return fileId;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "bin";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "bin";
    }

    private ArcFileContent createArcFileContent(MultipartFile file, CollectionBatch batch,
                                                 CollectionBatchFile batchFile, String fileId, Long userId) {
        return ArcFileContent.builder()
            .id(fileId)
            .archivalCode(batch.getBatchNo() + "-" + batchFile.getUploadOrder())
            .fileName(batchFile.getOriginalFilename())
            .fileType(batchFile.getFileType())
            .fileSize(batchFile.getFileSizeBytes())
            .fileHash(batchFile.getFileHash())
            .hashAlgorithm(batchFile.getHashAlgorithm())
            .storagePath(String.format("/tmp/nexusarchive/uploads/%s/%s/%s/%s.%s",
                batch.getFondsCode(), batch.getFiscalYear(), batch.getBatchNo(),
                fileId, getFileExtension(batchFile.getOriginalFilename())))
            .fiscalYear(batch.getFiscalYear())
            .voucherType(batch.getArchivalCategory())
            .fondsCode(batch.getFondsCode())
            .sourceSystem("WEB上传")
            .preArchiveStatus("PENDING_CHECK")
            .batchId(batch.getId())
            .createdTime(LocalDateTime.now())
            .build();
    }
}
```

**Step 2: Save and commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java
git commit -m "feat(service): add CollectionBatchServiceImpl with batch upload logic"
```

---

### Task 2.9: Create CollectionBatchController

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/controller/CollectionBatchController.java`

**Step 1: Write the controller**

```java
// Input: Spring Web, Jakarta Validation, Lombok
// Output: CollectionBatchController
// Pos: Controller Layer

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.service.CollectionBatchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资料收集批次控制器
 *
 * 提供批量上传的REST API:
 * - 创建批次
 * - 上传文件
 * - 完成批次
 * - 查询状态
 * - 四性检测
 */
@RestController
@RequestMapping("/api/collection/batch")
@RequiredArgsConstructor
@Slf4j
public class CollectionBatchController {

    private final CollectionBatchService collectionBatchService;

    /**
     * 创建上传批次
     *
     * POST /api/collection/batch/create
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CREATE_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "创建批量上传批次")
    public Result<BatchUploadResponse> createBatch(
            @Valid @RequestBody BatchUploadRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        BatchUploadResponse response = collectionBatchService.createBatch(request, userId);

        log.info("批次创建成功: batchId={}, batchNo={}", response.getBatchId(), response.getBatchNo());
        return Result.success("批次创建成功", response);
    }

    /**
     * 上传单个文件
     *
     * POST /api/collection/batch/{batchId}/upload
     */
    @PostMapping("/{batchId}/upload")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<CollectionBatchService.FileUploadResult> uploadFile(
            @PathVariable Long batchId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 文件类型验证
        String filename = file.getOriginalFilename();
        if (!isAllowedFileType(filename)) {
            return Result.error("不支持的文件类型，仅支持: PDF, OFD, XML, JPG, PNG, TIFF");
        }

        // 文件大小验证 (最大100MB)
        long maxSize = 100 * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            return Result.error("文件大小超过限制 (最大100MB)");
        }

        Long userId = getUserIdFromRequest(httpRequest);
        CollectionBatchService.FileUploadResult result =
            collectionBatchService.uploadFile(batchId, file, userId);

        log.info("文件上传结果: filename={}, status={}", filename, result.status());
        return Result.success(result);
    }

    /**
     * 完成批次上传
     *
     * POST /api/collection/batch/{batchId}/complete
     */
    @PostMapping("/{batchId}/complete")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "COMPLETE_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "完成批量上传")
    public Result<CollectionBatchService.BatchCompleteResult> completeBatch(
            @PathVariable Long batchId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        CollectionBatchService.BatchCompleteResult result =
            collectionBatchService.completeBatch(batchId, userId);

        return Result.success("批次已完成", result);
    }

    /**
     * 取消批次
     *
     * POST /api/collection/batch/{batchId}/cancel
     */
    @PostMapping("/{batchId}/cancel")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CANCEL_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "取消批量上传批次")
    public Result<String> cancelBatch(
            @PathVariable Long batchId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        collectionBatchService.cancelBatch(batchId, userId);

        return Result.success("批次已取消");
    }

    /**
     * 获取批次详情
     *
     * GET /api/collection/batch/{batchId}
     */
    @GetMapping("/{batchId}")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<CollectionBatchService.BatchDetailResponse> getBatchDetail(
            @PathVariable Long batchId) {

        CollectionBatchService.BatchDetailResponse detail =
            collectionBatchService.getBatchDetail(batchId);

        return Result.success(detail);
    }

    /**
     * 获取批次文件列表
     *
     * GET /api/collection/batch/{batchId}/files
     */
    @GetMapping("/{batchId}/files")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<CollectionBatchService.BatchFileResponse>> getBatchFiles(
            @PathVariable Long batchId) {

        List<CollectionBatchService.BatchFileResponse> files =
            collectionBatchService.getBatchFiles(batchId);

        return Result.success(files);
    }

    /**
     * 执行四性检测
     *
     * POST /api/collection/batch/{batchId}/check
     */
    @PostMapping("/{batchId}/check")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "FOUR_NATURE_CHECK", resourceType = "COLLECTION_BATCH",
                  description = "执行批次四性检测")
    public Result<CollectionBatchService.BatchCheckResult> runFourNatureCheck(
            @PathVariable Long batchId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        CollectionBatchService.BatchCheckResult result =
            collectionBatchService.runFourNatureCheck(batchId, userId);

        return Result.success(result);
    }

    /**
     * 获取用户的批次列表
     *
     * GET /api/collection/batch/list
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<CollectionBatchService.BatchDetailResponse>> listBatches(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        // TODO: 实现列表查询
        return Result.success(List.of());
    }

    // ===== Private Helper Methods =====

    private Long getUserIdFromRequest(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            return 1L; // 默认用户 (开发环境)
        }
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return Long.parseLong(userId.toString());
    }

    private boolean isAllowedFileType(String filename) {
        if (filename == null) return false;
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return List.of("pdf", "ofd", "xml", "jpg", "jpeg", "png", "tif", "tiff")
            .contains(ext);
    }
}
```

**Step 2: Save and commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/controller/CollectionBatchController.java
git commit -m "feat(controller): add CollectionBatchController REST API"
```

---

## Phase 3: Frontend Components

### Task 3.1: Create batch upload API client

**Files:**
- Create: `src/api/batchUpload.ts`

**Step 1: Write the API client**

```typescript
// Input: axios, Local Types
// Output: Batch Upload API Client
// Pos: API Layer

import axios from 'axios';

const BASE_URL = '/api/collection/batch';

// ===== Types =====

export interface BatchUploadRequest {
  batchName: string;
  fondsCode: string;
  fiscalYear: string;
  fiscalPeriod?: string;
  archivalCategory: 'VOUCHER' | 'LEDGER' | 'REPORT' | 'OTHER';
  totalFiles: number;
  autoCheck?: boolean;
}

export interface BatchUploadResponse {
  batchId: number;
  batchNo: string;
  status: string;
  uploadToken: string;
  totalFiles: number;
  uploadedFiles: number;
  failedFiles: number;
  progress: number;
  recentFiles?: FileInfo[];
}

export interface FileInfo {
  originalFilename: string;
  uploadStatus: string;
  fileSizeBytes: number;
  errorMessage?: string;
}

export interface BatchDetailResponse {
  id: number;
  batchNo: string;
  batchName: string;
  fondsCode: string;
  fiscalYear: string;
  archivalCategory: string;
  status: string;
  totalFiles: number;
  uploadedFiles: number;
  failedFiles: number;
  totalSizeBytes: number;
  progress: number;
}

export interface BatchFileResponse {
  id: number;
  fileId?: string;
  originalFilename: string;
  uploadStatus: string;
  fileSizeBytes: number;
  errorMessage?: string;
}

export interface FileUploadResult {
  fileId: string;
  originalFilename: string;
  status: string;
  errorMessage?: string;
}

export interface BatchCheckResult {
  batchId: number;
  totalFiles: number;
  checkedFiles: number;
  passedFiles: number;
  failedFiles: number;
  summary: string;
}

// ===== API Client =====

export const batchUploadApi = {
  /**
   * 创建上传批次
   */
  async createBatch(request: BatchUploadRequest): Promise<BatchUploadResponse> {
    const { data } = await axios.post<{ code: number; data: BatchUploadResponse }>(
      `${BASE_URL}/create`,
      request
    );
    return data.data;
  },

  /**
   * 上传单个文件
   */
  async uploadFile(
    batchId: number,
    file: File,
    onProgress?: (progress: number) => void
  ): Promise<FileUploadResult> {
    const formData = new FormData();
    formData.append('file', file);

    const { data } = await axios.post<{ code: number; data: FileUploadResult }>(
      `${BASE_URL}/${batchId}/upload`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onProgress(progress);
          }
        },
      }
    );
    return data.data;
  },

  /**
   * 完成批次上传
   */
  async completeBatch(batchId: number): Promise<{ batchId: number; status: string }> {
    const { data } = await axios.post(`${BASE_URL}/${batchId}/complete`);
    return data.data;
  },

  /**
   * 取消批次
   */
  async cancelBatch(batchId: number): Promise<void> {
    await axios.post(`${BASE_URL}/${batchId}/cancel`);
  },

  /**
   * 获取批次详情
   */
  async getBatchDetail(batchId: number): Promise<BatchDetailResponse> {
    const { data } = await axios.get<{ code: number; data: BatchDetailResponse }>(
      `${BASE_URL}/${batchId}`
    );
    return data.data;
  },

  /**
   * 获取批次文件列表
   */
  async getBatchFiles(batchId: number): Promise<BatchFileResponse[]> {
    const { data } = await axios.get<{ code: number; data: BatchFileResponse[] }>(
      `${BASE_URL}/${batchId}/files`
    );
    return data.data;
  },

  /**
   * 执行四性检测
   */
  async runFourNatureCheck(batchId: number): Promise<BatchCheckResult> {
    const { data } = await axios.post<{ code: number; data: BatchCheckResult }>(
      `${BASE_URL}/${batchId}/check`
    );
    return data.data;
  },

  /**
   * 获取批次列表
   */
  async listBatches(limit = 20, offset = 0): Promise<BatchDetailResponse[]> {
    const { data } = await axios.get<{ code: number; data: BatchDetailResponse[] }>(
      `${BASE_URL}/list`,
      { params: { limit, offset } }
    );
    return data.data;
  },
};
```

**Step 2: Save and commit**

```bash
git add src/api/batchUpload.ts
git commit -m "feat(api): add batchUpload API client"
```

---

### Task 3.2: Create BatchUploadView page component

**Files:**
- Create: `src/pages/collection/BatchUploadView.tsx`

**Step 1: Write the main page component (Part 1)**

```typescript
// Input: React, Ant Design, Local API, Hooks
// Output: BatchUploadView Component
// Pos: Page Component

/**
 * 批量上传页面
 *
 * 功能：
 * 1. 创建上传批次
 * 2. 拖拽/选择文件上传
 * 3. 实时进度跟踪
 * 4. 文件列表展示
 * 5. 四性检测触发
 *
 * 符合 GB/T 39362-2020 电子会计档案管理系统建设要求
 */

import React, { useState, useCallback, useRef, useEffect } from 'react';
import {
  Upload as UploadIcon,
  FileText,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Trash2,
  Pause,
  Play,
  ShieldCheck,
  ArrowLeft,
  FolderOpen,
} from 'lucide-react';
import { useNavigate, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { message, Upload, Modal, Progress, Button, Select, Form, Input, Card, Statistic, Row, Col } from 'antd';
import type { UploadProps, UploadFile } from 'antd';
import { batchUploadApi, type BatchUploadRequest, type BatchUploadResponse } from '../../api/batchUpload';
import { useFondsStore } from '../../store/useFondsStore';

const { Dragger } = Upload;
const { TextArea } = Input;

// ===== Types =====

interface FileUploadItem {
  uid: string;
  file: File;
  status: 'pending' | 'uploading' | 'uploaded' | 'failed' | 'duplicate';
  progress: number;
  error?: string;
  fileId?: string;
}

interface BatchFormData {
  batchName: string;
  fondsCode: string;
  fiscalYear: string;
  fiscalPeriod?: string;
  archivalCategory: 'VOUCHER' | 'LEDGER' | 'REPORT' | 'OTHER';
}

export const BatchUploadView: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const currentFonds = useFondsStore((state) => state.currentFonds);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // ===== State =====
  const [step, setStep] = useState<'create' | 'upload' | 'complete'>('create');
  const [batchInfo, setBatchInfo] = useState<BatchUploadResponse | null>(null);
  const [uploadQueue, setUploadQueue] = useState<FileUploadItem[]>([]);
  const [isPaused, setIsPaused] = useState(false);
  const [uploadingIndex, setUploadingIndex] = useState<number>(-1);
  const [form] = Form.useForm<BatchFormData>();

  // ===== Queries =====

  // 轮询批次状态
  const { data: batchDetail } = useQuery({
    queryKey: ['batchDetail', batchInfo?.batchId],
    queryFn: () => batchUploadApi.getBatchDetail(batchInfo!.batchId),
    enabled: !!batchInfo?.batchId && step !== 'create',
    refetchInterval: 2000, // 每2秒刷新一次
  });

  // 获取文件列表
  const { data: batchFiles = [] } = useQuery({
    queryKey: ['batchFiles', batchInfo?.batchId],
    queryFn: () => batchUploadApi.getBatchFiles(batchInfo!.batchId),
    enabled: !!batchInfo?.batchId && step !== 'create',
    refetchInterval: 3000,
  });

  // ===== Mutations =====

  const createBatchMutation = useMutation({
    mutationFn: (data: BatchUploadRequest) => batchUploadApi.createBatch(data),
    onSuccess: (response) => {
      setBatchInfo(response);
      setStep('upload');
      message.success('批次创建成功');
    },
    onError: (err: any) => {
      message.error('批次创建失败: ' + (err.message || '未知错误'));
    },
  });

  const uploadFileMutation = useMutation({
    mutationFn: ({ file, index }: { file: File; index: number }) =>
      batchUploadApi.uploadFile(
        batchInfo!.batchId,
        file,
        (progress) => {
          setUploadQueue((prev) => {
            const updated = [...prev];
            updated[index].progress = progress;
            return updated;
          });
        }
      ),
    onSuccess: (result, variables) => {
      setUploadQueue((prev) => {
        const updated = [...prev];
        updated[variables.index].status = result.status === 'UPLOADED' ? 'uploaded' : 'failed';
        updated[variables.index].fileId = result.fileId;
        updated[variables.index].error = result.errorMessage;
        return updated;
      });
    },
    onError: (err: any, variables) => {
      setUploadQueue((prev) => {
        const updated = [...prev];
        updated[variables.index].status = 'failed';
        updated[variables.index].error = err.message || '上传失败';
        return updated;
      });
    },
  });

  const completeBatchMutation = useMutation({
    mutationFn: () => batchUploadApi.completeBatch(batchInfo!.batchId),
    onSuccess: () => {
      setStep('complete');
      message.success('批次上传完成');
    },
  });

  const cancelBatchMutation = useMutation({
    mutationFn: () => batchUploadApi.cancelBatch(batchInfo!.batchId),
    onSuccess: () => {
      message.success('批次已取消');
      navigate('/system/collection');
    },
  });

  const runCheckMutation = useMutation({
    mutationFn: () => batchUploadApi.runFourNatureCheck(batchInfo!.batchId),
    onSuccess: (result) => {
      message.success(result.summary);
    },
  });

  // ===== Effects =====

  // 自动处理上传队列
  useEffect(() => {
    if (step === 'upload' && !isPaused && uploadingIndex === -1) {
      const nextIndex = uploadQueue.findIndex((item) => item.status === 'pending');
      if (nextIndex !== -1) {
        setUploadingIndex(nextIndex);
        setUploadQueue((prev) => {
          const updated = [...prev];
          updated[nextIndex].status = 'uploading';
          return updated;
        });
      }
    }
  }, [step, isPaused, uploadQueue, uploadingIndex]);

  // 执行上传
  useEffect(() => {
    if (uploadingIndex >= 0 && uploadQueue[uploadingIndex]?.status === 'uploading') {
      const item = uploadQueue[uploadingIndex];
      uploadFileMutation.mutate(
        { file: item.file, index: uploadingIndex },
        {
          onSettled: () => {
            setUploadingIndex(-1);
          },
        }
      );
    }
  }, [uploadingIndex, uploadQueue]);

  // ===== Handlers =====

  const handleCreateBatch = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const request: BatchUploadRequest = {
        ...values,
        fondsCode: currentFonds?.fondsCode || values.fondsCode,
        totalFiles: uploadQueue.length,
        autoCheck: true,
      };
      createBatchMutation.mutate(request);
    } catch (error) {
      message.error('请填写完整的批次信息');
    }
  }, [form, currentFonds, uploadQueue, createBatchMutation]);

  const handleFileSelect: UploadProps['onChange'] = useCallback((info) => {
    const newFiles = info.fileList
      .filter((file) => file.originFileObj)
      .map((file) => ({
        uid: file.uid,
        file: file.originFileObj as File,
        status: 'pending' as const,
        progress: 0,
      }));

    setUploadQueue((prev) => [...prev, ...newFiles]);
  }, []);

  const handleRemoveFile = useCallback((uid: string) => {
    setUploadQueue((prev) => prev.filter((item) => item.uid !== uid));
  }, []);

  const handleRetryFile = useCallback((uid: string) => {
    setUploadQueue((prev) =>
      prev.map((item) => {
        if (item.uid === uid) {
          return { ...item, status: 'pending' as const, progress: 0, error: undefined };
        }
        return item;
      })
    );
  }, []);

  const handleCompleteBatch = useCallback(() => {
    completeBatchMutation.mutate();
  }, [completeBatchMutation]);

  const handleCancelBatch = useCallback(() => {
    Modal.confirm({
      title: '确认取消',
      content: '取消后将删除所有已上传的文件，是否继续？',
      onOk: () => cancelBatchMutation.mutate(),
    });
  }, [cancelBatchMutation]);

  const handleRunCheck = useCallback(() => {
    runCheckMutation.mutate();
  }, [runCheckMutation]);

  // ===== Render Helpers =====

  const renderFileIcon = (status: FileUploadItem['status']) => {
    switch (status) {
      case 'uploaded':
        return <CheckCircle2 className="text-green-500" size={20} />;
      case 'failed':
        return <XCircle className="text-red-500" size={20} />;
      case 'duplicate':
        return <AlertCircle className="text-amber-500" size={20} />;
      case 'uploading':
        return <UploadIcon className="text-blue-500 animate-pulse" size={20} />;
      default:
        return <FileText className="text-slate-400" size={20} />;
    }
  };

  const getStatusText = (status: FileUploadItem['status']) => {
    const statusMap = {
      pending: '等待中',
      uploading: '上传中',
      uploaded: '已上传',
      failed: '上传失败',
      duplicate: '重复文件',
    };
    return statusMap[status];
  };

  const stats = React.useMemo(() => {
    const total = uploadQueue.length;
    const uploaded = uploadQueue.filter((f) => f.status === 'uploaded').length;
    const failed = uploadQueue.filter((f) => f.status === 'failed').length;
    const duplicate = uploadQueue.filter((f) => f.status === 'duplicate').length;
    return { total, uploaded, failed, duplicate };
  }, [uploadQueue]);

  // ===== Render =====

  if (step === 'create') {
    return (
      <div className="p-8 max-w-4xl mx-auto animate-in fade-in duration-300">
        {/* Header */}
        <div className="flex items-center gap-4 mb-8">
          <button
            onClick={() => navigate('/system/collection')}
            className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">批量上传</h1>
            <p className="text-slate-500 text-sm">创建上传批次并上传会计档案文件</p>
          </div>
        </div>

        {/* Batch Form */}
        <Card className="mb-6">
          <Form
            form={form}
            layout="vertical"
            initialValues={{
              fondsCode: currentFonds?.fondsCode || '001',
              fiscalYear: new Date().getFullYear().toString(),
              archivalCategory: 'VOUCHER',
            }}
          >
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label="批次名称"
                  name="batchName"
                  rules={[{ required: true, message: '请输入批次名称' }]}
                >
                  <Input placeholder="例如：2024年1月凭证" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="全宗代码"
                  name="fondsCode"
                  rules={[{ required: true, message: '请输入全宗代码' }]}
                >
                  <Input placeholder="001" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  label="会计年度"
                  name="fiscalYear"
                  rules={[{ required: true, message: '请输入会计年度' }]}
                >
                  <Input placeholder="2024" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="会计期间" name="fiscalPeriod">
                  <Select
                    placeholder="选择期间"
                    allowClear
                    options={[
                      { label: '1月', value: '01' },
                      { label: '2月', value: '02' },
                      { label: '3月', value: '03' },
                      { label: '4月', value: '04' },
                      { label: '5月', value: '05' },
                      { label: '6月', value: '06' },
                      { label: '7月', value: '07' },
                      { label: '8月', value: '08' },
                      { label: '9月', value: '09' },
                      { label: '10月', value: '10' },
                      { label: '11月', value: '11' },
                      { label: '12月', value: '12' },
                    ]}
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="档案门类"
                  name="archivalCategory"
                  rules={[{ required: true, message: '请选择档案门类' }]}
                >
                  <Select
                    options={[
                      { label: '会计凭证', value: 'VOUCHER' },
                      { label: '会计账簿', value: 'LEDGER' },
                      { label: '财务报告', value: 'REPORT' },
                      { label: '其他资料', value: 'OTHER' },
                    ]}
                  />
                </Form.Item>
              </Col>
            </Row>
          </Form>
        </Card>

        {/* File Upload Area */}
        <Card className="mb-6">
          <Dragger
            multiple
            accept=".pdf,.ofd,.xml,.jpg,.jpeg,.png,.tif,.tiff"
            customRequest={() => {}}
            onChange={handleFileSelect}
            showUploadList={false}
          >
            <p className="ant-upload-drag-icon">
              <FolderOpen size={48} className="text-blue-500" />
            </p>
            <p className="text-lg font-medium text-slate-700">
              点击或拖拽文件到此区域上传
            </p>
            <p className="text-slate-500 text-sm mt-2">
              支持 PDF、OFD、XML、JPG、PNG、TIFF 格式，单个文件不超过 100MB
            </p>
          </Dragger>
        </Card>

        {/* File List */}
        {uploadQueue.length > 0 && (
          <Card className="mb-6">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-semibold text-slate-800">
                已选择 {uploadQueue.length} 个文件
              </h3>
              <Button
                danger
                size="small"
                onClick={() => setUploadQueue([])}
              >
                清空列表
              </Button>
            </div>

            <div className="space-y-2 max-h-64 overflow-y-auto">
              {uploadQueue.map((item) => (
                <div
                  key={item.uid}
                  className="flex items-center gap-3 p-3 bg-slate-50 dark:bg-slate-800 rounded-lg"
                >
                  {renderFileIcon(item.status)}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-slate-700 truncate">
                      {item.file.name}
                    </p>
                    <p className="text-xs text-slate-500">
                      {(item.file.size / 1024 / 1024).toFixed(2)} MB
                      {item.status === 'uploading' && ` - ${item.progress}%`}
                    </p>
                    {item.error && (
                      <p className="text-xs text-red-500">{item.error}</p>
                    )}
                  </div>
                  {item.status === 'failed' && (
                    <Button size="small" onClick={() => handleRetryFile(item.uid)}>
                      重试
                    </Button>
                  )}
                  <Button
                    size="small"
                    danger
                    disabled={item.status === 'uploading'}
                    onClick={() => handleRemoveFile(item.uid)}
                  >
                    <Trash2 size={14} />
                  </Button>
                </div>
              ))}
            </div>
          </Card>
        )}

        {/* Actions */}
        <div className="flex justify-end gap-3">
          <Button onClick={() => navigate('/system/collection')}>
            取消
          </Button>
          <Button
            type="primary"
            onClick={handleCreateBatch}
            disabled={uploadQueue.length === 0}
            loading={createBatchMutation.isPending}
          >
            开始上传 ({uploadQueue.length})
          </Button>
        </div>
      </div>
    );
  }

  // Upload & Complete Steps
  return (
    <div className="p-8 max-w-6xl mx-auto animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/system/collection')}
            className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">
              {batchInfo?.batchName}
            </h1>
            <p className="text-slate-500 text-sm">
              批次号: {batchInfo?.batchNo} · 状态: {batchDetail?.status}
            </p>
          </div>
        </div>

        {step === 'upload' && (
          <div className="flex items-center gap-2">
            <Button
              icon={isPaused ? <Play size={16} /> : <Pause size={16} />}
              onClick={() => setIsPaused(!isPaused)}
            >
              {isPaused ? '继续' : '暂停'}
            </Button>
            <Button
              type="primary"
              onClick={handleCompleteBatch}
              disabled={stats.uploaded === 0}
            >
              完成上传
            </Button>
          </div>
        )}
      </div>

      {/* Statistics */}
      <Row gutter={16} className="mb-6">
        <Col span={6}>
          <Card>
            <Statistic title="总文件数" value={stats.total} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已上传"
              value={stats.uploaded}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="失败"
              value={stats.failed}
              valueStyle={{ color: stats.failed > 0 ? '#cf1322' : undefined }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="重复"
              value={stats.duplicate}
              valueStyle={{ color: stats.duplicate > 0 ? '#faad14' : undefined }}
            />
          </Card>
        </Col>
      </Row>

      {/* Overall Progress */}
      {step === 'upload' && (
        <Card className="mb-6">
          <div className="flex items-center gap-4">
            <div className="flex-1">
              <div className="flex justify-between mb-2">
                <span className="text-sm font-medium">上传进度</span>
                <span className="text-sm text-slate-500">
                  {stats.uploaded} / {stats.total}
                </span>
              </div>
              <Progress
                percent={Math.round((stats.uploaded / stats.total) * 100)}
                status={stats.failed > 0 ? 'exception' : 'active'}
              />
            </div>
          </div>
        </Card>
      )}

      {/* File List (from server) */}
      <Card title="文件列表">
        <div className="space-y-2">
          {batchFiles.map((file) => (
            <div
              key={file.id}
              className="flex items-center gap-3 p-3 bg-slate-50 dark:bg-slate-800 rounded-lg"
            >
              {renderFileIcon(
                file.uploadStatus === 'UPLOADED' || file.uploadStatus === 'VALIDATED'
                  ? 'uploaded'
                  : file.uploadStatus === 'FAILED' || file.uploadStatus === 'CHECK_FAILED'
                  ? 'failed'
                  : 'pending'
              )}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-slate-700 truncate">
                  {file.originalFilename}
                </p>
                <p className="text-xs text-slate-500">
                  {((file.fileSizeBytes || 0) / 1024 / 1024).toFixed(2)} MB
                  {' · '}{getStatusText(file.uploadStatus as any)}
                </p>
                {file.errorMessage && (
                  <p className="text-xs text-red-500">{file.errorMessage}</p>
                )}
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Actions for Complete Step */}
      {step === 'complete' && (
        <div className="mt-6 flex justify-center gap-4">
          <Button
            type="primary"
            icon={<ShieldCheck size={16} />}
            onClick={handleRunCheck}
            loading={runCheckMutation.isPending}
          >
            执行四性检测
          </Button>
          <Button onClick={() => navigate('/system/pre-archive/pool')}>
            前往预归档库
          </Button>
          <Button danger onClick={handleCancelBatch}>
            取消批次
          </Button>
        </div>
      )}
    </div>
  );
};

export default BatchUploadView;
```

**Step 2: Save and commit**

```bash
git add src/pages/collection/BatchUploadView.tsx
git commit -m "feat(page): add BatchUploadView page component"
```

---

### Task 3.3: Update routes to include BatchUploadView

**Files:**
- Modify: `src/routes/index.tsx`

**Step 1: Import and add route**

```typescript
// Add import at top of file
import { BatchUploadView } from '../pages/collection/BatchUploadView';

// Find the collection routes section and add:
{ path: 'collection/upload', element: <BatchUploadView /> },
```

**Before (around line 173):**
```tsx
{ path: 'collection/upload', element: <ArchiveListPage routeConfig="collection" /> },
```

**After:**
```tsx
{ path: 'collection/upload', element: <BatchUploadView /> },
```

**Step 2: Save and commit**

```bash
git add src/routes/index.tsx
git commit -m "feat(routes): add BatchUploadView to collection/upload route"
```

---

### Task 3.4: Update COLLECTION_CONFIG in constants

**Files:**
- Modify: `src/constants.tsx`

**Step 1: The COLLECTION_CONFIG remains for batch list view**
No changes needed - the existing COLLECTION_CONFIG displays batch history.

**Step 2: Commit (if any changes were made)**

---

## Phase 4: Integration & Testing

### Task 4.1: Write backend unit tests

**Files:**
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/service/CollectionBatchServiceTest.java`

**Step 1: Write the test class**

```java
// Input: JUnit 5, Spring Boot Test, Mockito
// Output: CollectionBatchServiceTest
// Pos: Test Layer

package com.nexusarchive.service;

import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.mapper.CollectionBatchMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CollectionBatchService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CollectionBatchServiceTest {

    @Mock
    private CollectionBatchMapper batchMapper;

    @Mock
    private CollectionBatchFileMapper batchFileMapper;

    @Mock
    private PoolService poolService;

    @Mock
    private FourNatureCheckService fourNatureCheckService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CollectionBatchServiceImpl collectionBatchService;

    @Test
    void createBatch_ShouldReturnBatchResponse() {
        // Given
        BatchUploadRequest request = new BatchUploadRequest();
        request.setBatchName("测试批次");
        request.setFondsCode("001");
        request.setFiscalYear("2024");
        request.setArchivalCategory("VOUCHER");
        request.setTotalFiles(10);

        when(batchMapper.insert(any(CollectionBatch.class))).thenAnswer(invocation -> {
            CollectionBatch batch = invocation.getArgument(0);
            batch.setId(1L);
            return 1;
        });

        // When
        BatchUploadResponse response = collectionBatchService.createBatch(request, 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBatchNo()).startsWith("COL-");
        assertThat(response.getTotalFiles()).isEqualTo(10);
        verify(batchMapper).insert(any(CollectionBatch.class));
        verify(auditLogService).log(anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void getBatchDetail_ShouldReturnBatchInfo() {
        // Given
        CollectionBatch batch = CollectionBatch.builder()
            .id(1L)
            .batchNo("COL-20240105-001")
            .batchName("测试批次")
            .fondsCode("001")
            .fiscalYear("2024")
            .archivalCategory("VOUCHER")
            .status("UPLOADING")
            .totalFiles(10)
            .uploadedFiles(5)
            .failedFiles(1)
            .build();

        when(batchMapper.selectById(1L)).thenReturn(batch);

        // When
        var detail = collectionBatchService.getBatchDetail(1L);

        // Then
        assertThat(detail.batchNo()).isEqualTo("COL-20240105-001");
        assertThat(detail.progress()).isEqualTo(50); // 5/10 = 50%
    }
}
```

**Step 2: Run tests**

Run: `cd nexusarchive-java && mvn test -Dtest=CollectionBatchServiceTest`
Expected: Tests pass

**Step 3: Commit**

```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/service/CollectionBatchServiceTest.java
git commit -m "test: add CollectionBatchService unit tests"
```

---

### Task 4.2: Write API integration test

**Files:**
- Create: `tests/playwright/api/batch-upload.spec.ts`

**Step 1: Write the Playwright API test**

```typescript
// Input: Playwright Test
// Output: Batch Upload API Integration Test
// Pos: E2E Test

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.API_BASE_URL || 'http://localhost:19090';
let authToken: string;

test.beforeAll(async () => {
  // 登录获取 Token
  const loginResponse = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin',
      password: 'admin123',
    }),
  });
  const loginData = await loginResponse.json();
  authToken = loginData.data.token;
});

test.describe('批量上传 API', () => {
  let batchId: number;
  let batchNo: string;

  test('POST /api/collection/batch/create - 创建上传批次', async () => {
    const response = await fetch(`${BASE_URL}/api/collection/batch/create`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        batchName: 'E2E测试批次',
        fondsCode: '001',
        fiscalYear: '2024',
        archivalCategory: 'VOUCHER',
        totalFiles: 2,
      }),
    });

    expect(response.ok).toBeTruthy();

    const data = await response.json();
    expect(data.code).toBe(200);
    expect(data.data.batchNo).toMatch(/^COL-\d{8}-\d{3}$/);

    batchId = data.data.batchId;
    batchNo = data.data.batchNo;
  });

  test('POST /api/collection/batch/{batchId}/upload - 上传文件', async () => {
    // 创建测试文件
    const testContent = 'Test PDF content for batch upload';
    const blob = new Blob([testContent], { type: 'application/pdf' });
    const formData = new FormData();
    formData.append('file', blob, 'test-file.pdf');

    const response = await fetch(`${BASE_URL}/api/collection/batch/${batchId}/upload`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authToken}`,
      },
      body: formData,
    });

    expect(response.ok).toBeTruthy();

    const data = await response.json();
    expect(data.code).toBe(200);
    expect(data.data.status).toBe('UPLOADED');
  });

  test('GET /api/collection/batch/{batchId} - 获取批次详情', async () => {
    const response = await fetch(`${BASE_URL}/api/collection/batch/${batchId}`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
      },
    });

    expect(response.ok).toBeTruthy();

    const data = await response.json();
    expect(data.code).toBe(200);
    expect(data.data.batchNo).toBe(batchNo);
    expect(data.data.uploadedFiles).toBeGreaterThan(0);
  });

  test('POST /api/collection/batch/{batchId}/complete - 完成批次', async () => {
    const response = await fetch(`${BASE_URL}/api/collection/batch/${batchId}/complete`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authToken}`,
      },
    });

    expect(response.ok).toBeTruthy();

    const data = await response.json();
    expect(data.code).toBe(200);
    expect(data.data.status).toBe('UPLOADED');
  });

  test('POST /api/collection/batch/{batchId}/check - 执行四性检测', async () => {
    const response = await fetch(`${BASE_URL}/api/collection/batch/${batchId}/check`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authToken}`,
      },
    });

    expect(response.ok).toBeTruthy();

    const data = await response.json();
    expect(data.code).toBe(200);
    expect(data.data.summary).toContain('检测完成');
  });
});
```

**Step 2: Run tests**

Run: `npx playwright test tests/playwright/api/batch-upload.spec.ts`
Expected: Tests pass

**Step 3: Commit**

```bash
git add tests/playwright/api/batch-upload.spec.ts
git commit -m "test(e2e): add batch upload API integration tests"
```

---

### Task 4.3: Update documentation

**Files:**
- Create: `docs/features/batch-upload.md`

**Step 1: Write the feature documentation**

```markdown
# 批量上传功能

## 功能概述

批量上传功能允许用户通过 Web 界面上传多个会计档案文件，系统会自动进行：
- 文件哈希校验 (幂等性控制)
- 四性检测 (真实性、完整性、可用性、安全性)
- 预归档处理
- 审计日志记录

## 用户操作流程

### 1. 创建批次

1. 导航到"资料收集" > "批量上传"
2. 填写批次信息：
   - 批次名称
   - 全宗代码
   - 会计年度
   - 会计期间 (可选)
   - 档案门类 (凭证/账簿/报告/其他)
3. 拖拽或选择文件
4. 点击"开始上传"

### 2. 上传文件

- 支持格式: PDF, OFD, XML, JPG, PNG, TIFF
- 单文件大小限制: 100MB
- 支持暂停/继续
- 实时显示上传进度

### 3. 完成上传

- 点击"完成上传"按钮
- 系统会统计上传结果
- 自动跳转到完成页面

### 4. 四性检测

- 点击"执行四性检测"按钮
- 系统会对所有已上传文件进行检测
- 检测完成后可查看详细报告

## API 文档

详见 `docs/api/batch-upload-api.md`

## 合规性说明

本功能符合以下国家标准：
- GB/T 39362-2020 电子会计档案管理系统建设要求
- GB/T 39784-2021 电子档案单套制管理规范
- DA/T 70-2018 电子档案管理基本术语
```

**Step 2: Save and commit**

```bash
mkdir -p docs/features
git add docs/features/batch-upload.md
git commit -m "docs: add batch upload feature documentation"
```

---

### Task 4.4: Final integration test

**Files:**
- Test: Manual browser test

**Step 1: Start development environment**

Run: `npm run dev`

**Step 2: Verify the flow**

1. Login as admin
2. Navigate to `/system/collection/upload`
3. Create a batch with test data
4. Upload test files
5. Complete the batch
6. Run four nature check
7. Verify files appear in pre-archive pool

**Step 3: Commit final updates**

```bash
git add -A
git commit -m "feat(batch-upload): complete implementation with integration testing"
```

---

## Summary

This implementation plan provides:

1. **Database Schema**: Two new tables for batch and file tracking
2. **Backend API**: Full REST API with Spring Boot
3. **Frontend UI**: React component with drag-drop upload
4. **Testing**: Unit tests, integration tests, and E2E tests
5. **Documentation**: Feature documentation

**Compliance features:**
- Four-nature check (四性检测) integration
- Audit logging for all operations
- File hash-based deduplication (幂等性)
- Support for long-term preservation formats (OFD, PDF/A)
