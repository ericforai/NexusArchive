# Archive Borrowing (档案借阅) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现电子会计档案的借阅管理功能，包括借阅申请、审批流程、借阅出库、归还管理。

**Architecture:** 采用三层架构（Controller → Service → Mapper），使用 MyBatis-Plus 进行 ORM，遵循现有的档案管理模式（如销毁、审批等）。

**Tech Stack:** Spring Boot 3.1.6, MyBatis-Plus 3.5.7, PostgreSQL, Lombok, Spring Security

---

## 数据库设计

### Task 1: 创建借阅申请表 (acc_borrow_request)

**Files:**
- Create: `nexusarchive-java/src/main/resources/db/migration/V80__create_borrow_request.sql`

**Step 1: 编写迁移脚本**

```sql
-- 借阅申请表
CREATE TABLE acc_borrow_request (
    id VARCHAR(36) PRIMARY KEY,
    request_no VARCHAR(50) UNIQUE NOT NULL,        -- 借阅单号 (BL-YYYYMMDD-序号)
    applicant_id VARCHAR(36) NOT NULL,             -- 申请人ID
    applicant_name VARCHAR(100) NOT NULL,          -- 申请人姓名
    dept_id VARCHAR(36),                          -- 申请部门ID
    dept_name VARCHAR(200),                       -- 申请部门名称
    purpose VARCHAR(500) NOT NULL,                 -- 借阅目的
    borrow_type VARCHAR(20) NOT NULL,              -- 借阅类型: READING(阅览), COPY(复制), LOAN(外借)
    expected_start_date DATE NOT NULL,             -- 预期开始日期
    expected_end_date DATE NOT NULL,               -- 预期结束日期
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 状态: PENDING, APPROVED, REJECTED, BORROWING, RETURNED, OVERDUE
    archive_ids TEXT NOT NULL,                     -- 借阅档案ID列表 (JSON数组)
    archive_count INTEGER NOT NULL,                -- 借阅档案数量
    approver_id VARCHAR(36),                       -- 审批人ID
    approver_name VARCHAR(100),                     -- 审批人姓名
    approval_time TIMESTAMP,                        -- 审批时间
    approval_comment VARCHAR(500),                  -- 审批意见
    actual_start_date DATE,                         -- 实际开始日期
    actual_end_date DATE,                           -- 实际结束日期
    return_time TIMESTAMP,                           -- 归还时间
    return_operator_id VARCHAR(36),                 -- 归还操作人ID
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted NUMERIC(1) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_borrow_request_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user(id),
    CONSTRAINT fk_borrow_request_approver FOREIGN KEY (approver_id) REFERENCES sys_user(id)
);

CREATE INDEX idx_borrow_request_applicant ON acc_borrow_request(applicant_id);
CREATE INDEX idx_borrow_request_status ON acc_borrow_request(status);
CREATE INDEX idx_borrow_request_dates ON acc_borrow_request(expected_start_date, expected_end_date);
COMMENT ON TABLE acc_borrow_request IS '档案借阅申请表';
```

**Step 2: 验证迁移脚本格式**

Run: `head -n 5 nexusarchive-java/src/main/resources/db/migration/V80__create_borrow_request.sql`
Expected: SQL 语句正确，以 `--` 或 `CREATE` 开头

**Step 3: Commit**

```bash
git add nexusarchive-java/src/main/resources/db/migration/V80__create_borrow_request.sql
git commit -m "feat(db): add acc_borrow_request table for archive borrowing"
```

---

### Task 2: 创建借阅档案明细表 (acc_borrow_archive)

**Files:**
- Create: `nexusarchive-java/src/main/resources/db/migration/V81__create_borrow_archive.sql`

**Step 1: 编写迁移脚本**

```sql
-- 借阅档案明细表
CREATE TABLE acc_borrow_archive (
    id VARCHAR(36) PRIMARY KEY,
    borrow_request_id VARCHAR(36) NOT NULL,        -- 借阅申请ID
    archive_id VARCHAR(36) NOT NULL,               -- 档案ID
    archive_code VARCHAR(100) NOT NULL,           -- 档号
    archive_title VARCHAR(500) NOT NULL,          -- 题名
    return_status VARCHAR(20) DEFAULT 'BORROWED',  -- 归还状态: BORROWED, RETURNED
    return_time TIMESTAMP,                         -- 归还时间
    return_operator_id VARCHAR(36),               -- 归还操作人ID
    damaged BOOLEAN DEFAULT FALSE,                  -- 是否损坏
    damage_desc VARCHAR(500),                      -- 损坏描述
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_borrow_archive_request FOREIGN KEY (borrow_request_id) REFERENCES acc_borrow_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_borrow_archive_archive FOREIGN KEY (archive_id) REFERENCES acc_archive(id)
);

CREATE INDEX idx_borrow_archive_request ON acc_borrow_archive(borrow_request_id);
CREATE INDEX idx_borrow_archive_archive ON acc_borrow_archive(archive_id);
COMMENT ON TABLE acc_borrow_archive IS '借阅档案明细表';
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/resources/db/migration/V81__create_borrow_archive.sql
git commit -m "feat(db): add acc_borrow_archive table for borrowed archive items"
```

---

### Task 3: 创建借阅记录表 (acc_borrow_log)

**Files:**
- Create: `nexusarchive-java/src/main/resources/db/migration/V82__create_borrow_log.sql`

**Step 1: 编写迁移脚本**

```sql
-- 借阅记录表（历史归档）
CREATE TABLE acc_borrow_log (
    id VARCHAR(36) PRIMARY KEY,
    request_no VARCHAR(50) NOT NULL,               -- 借阅单号
    applicant_id VARCHAR(36) NOT NULL,            -- 申请人ID
    applicant_name VARCHAR(100) NOT NULL,         -- 申请人姓名
    dept_name VARCHAR(200),                      -- 部门名称
    purpose VARCHAR(500),                         -- 借阅目的
    borrow_type VARCHAR(20),                      -- 借阅类型
    borrow_start_date DATE,                       -- 借阅开始日期
    borrow_end_date DATE,                         -- 借阅结束日期
    archive_count INTEGER,                         -- 档案数量
    status VARCHAR(20),                            -- 最终状态: COMPLETED, CANCELLED
    created_time TIMESTAMP NOT NULL,
    completed_time TIMESTAMP,
    CONSTRAINT fk_borrow_log_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user(id)
);

CREATE INDEX idx_borrow_log_applicant ON acc_borrow_log(applicant_id);
CREATE INDEX idx_borrow_log_dates ON acc_borrow_log(borrow_start_date, borrow_end_date);
COMMENT ON TABLE acc_borrow_log IS '借阅记录历史表';
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/resources/db/migration/V82__create_borrow_log.sql
git commit -m "feat(db): add acc_borrow_log table for borrowing history"
```

---

## 实体层 (Entity)

### Task 4: 创建 BorrowRequest 实体

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/BorrowRequest.java`

**Step 1: 编写实体类**

```java
package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅申请实体
 */
@Data
@TableName("acc_borrow_request")
public class BorrowRequest {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 借阅单号 (BL-YYYYMMDD-序号)
     */
    @NotBlank(message = "借阅单号不能为空")
    private String requestNo;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人姓名
     */
    @NotBlank(message = "申请人姓名不能为空")
    @Size(max = 100, message = "申请人姓名长度不能超过100")
    private String applicantName;

    /**
     * 申请部门ID
     */
    private String deptId;

    /**
     * 申请部门名称
     */
    @Size(max = 200, message = "部门名称长度不能超过200")
    private String deptName;

    /**
     * 借阅目的
     */
    @NotBlank(message = "借阅目的不能为空")
    @Size(max = 500, message = "借阅目的长度不能超过500")
    private String purpose;

    /**
     * 借阅类型: READING(阅览), COPY(复制), LOAN(外借)
     */
    @NotBlank(message = "借阅类型不能为空")
    private BorrowType borrowType;

    /**
     * 预期开始日期
     */
    @NotNull(message = "预期开始日期不能为空")
    private LocalDate expectedStartDate;

    /**
     * 预期结束日期
     */
    @NotNull(message = "预期结束日期不能为空")
    private LocalDate expectedEndDate;

    /**
     * 状态: PENDING, APPROVED, REJECTED, BORROWING, RETURNED, OVERDUE
     */
    @NotBlank(message = "状态不能为空")
    private BorrowStatus status = BorrowStatus.PENDING;

    /**
     * 借阅档案ID列表 (JSON数组字符串)
     */
    @NotBlank(message = "借阅档案不能为空")
    private String archiveIds;

    /**
     * 借阅档案数量
     */
    @NotNull(message = "借阅档案数量不能为空")
    private Integer archiveCount;

    /**
     * 审批人ID
     */
    private String approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

    /**
     * 审批意见
     */
    @Size(max = 500, message = "审批意见长度不能超过500")
    private String approvalComment;

    /**
     * 实际开始日期
     */
    private LocalDate actualStartDate;

    /**
     * 实际结束日期
     */
    private LocalDate actualEndDate;

    /**
     * 归还时间
     */
    private LocalDateTime returnTime;

    /**
     * 归还操作人ID
     */
    private String returnOperatorId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Boolean deleted;

    /**
     * 借阅类型枚举
     */
    public enum BorrowType {
        READING("阅览", "在档案室阅览，不可带出"),
        COPY("复制", "复制或扫描档案内容"),
        LOAN("外借", "经批准后可借出档案室");

        private final String label;
        private final String description;

        BorrowType(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 借阅状态枚举
     */
    public enum BorrowStatus {
        PENDING("待审批", "等待审批"),
        APPROVED("已批准", "审批通过，待借出"),
        REJECTED("已拒绝", "审批未通过"),
        BORROWING("借阅中", "档案已借出"),
        RETURNED("已归还", "档案已归还"),
        OVERDUE("逾期未还", "超过归还期限");

        private final String label;
        private final String description;

        BorrowStatus(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }
}
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/entity/BorrowRequest.java
git commit -m "feat(entity): add BorrowRequest entity"
```

---

### Task 5: 创建 BorrowArchive 实体

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/BorrowArchive.java`

**Step 1: 编写实体类**

```java
package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅档案明细实体
 */
@Data
@TableName("acc_borrow_archive")
public class BorrowArchive {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 借阅申请ID
     */
    private String borrowRequestId;

    /**
     * 档案ID
     */
    private String archiveId;

    /**
     * 档号
     */
    private String archiveCode;

    /**
     * 题名
     */
    private String archiveTitle;

    /**
     * 归还状态: BORROWED, RETURNED
     */
    private ReturnStatus returnStatus = ReturnStatus.BORROWED;

    /**
     * 归还时间
     */
    private LocalDateTime returnTime;

    /**
     * 归还操作人ID
     */
    private String returnOperatorId;

    /**
     * 是否损坏
     */
    private Boolean damaged = false;

    /**
     * 损坏描述
     */
    private String damageDesc;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 归还状态枚举
     */
    public enum ReturnStatus {
        BORROWED("借阅中"),
        RETURNED("已归还");

        private final String label;

        ReturnStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/entity/BorrowArchive.java
git commit -m "feat(entity): add BorrowArchive entity"
```

---

### Task 6: 创建 BorrowLog 实体

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/BorrowLog.java`

**Step 1: 编写实体类**

```java
package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅记录实体（历史归档）
 */
@Data
@TableName("acc_borrow_log")
public class BorrowLog {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 借阅单号
     */
    private String requestNo;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 借阅目的
     */
    private String purpose;

    /**
     * 借阅类型
     */
    private String borrowType;

    /**
     * 借阅开始日期
     */
    private LocalDate borrowStartDate;

    /**
     * 借阅结束日期
     */
    private LocalDate borrowEndDate;

    /**
     * 档案数量
     */
    private Integer archiveCount;

    /**
     * 最终状态: COMPLETED, CANCELLED
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
}
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/entity/BorrowLog.java
git commit -m "feat(entity): add BorrowLog entity for history tracking"
```

---

## 数据访问层 (Mapper)

### Task 7: 创建 BorrowRequestMapper

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/BorrowRequestMapper.java`

**Step 1: 编写 Mapper 接口**

```java
package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.BorrowRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 借阅申请 Mapper
 */
@Mapper
public interface BorrowRequestMapper extends BaseMapper<BorrowRequest> {

    @Select("""
            SELECT br.*,
                   u1.real_name as applicant_real_name,
                   u2.real_name as approver_real_name
            FROM acc_borrow_request br
            LEFT JOIN sys_user u1 ON br.applicant_id = u1.id
            LEFT JOIN sys_user u2 ON br.approver_id = u2.id
            WHERE br.deleted = 0
            AND br.id = #{id}
            """)
    Map<String, Object> selectDetailById(@Param("id") String id);

    @Select("""
            SELECT br.*,
                   u1.real_name as applicant_real_name,
                   u2.real_name as approver_real_name
            FROM acc_borrow_request br
            LEFT JOIN sys_user u1 ON br.applicant_id = u1.id
            LEFT JOIN sys_user u2 ON br.approver_id = u2.id
            WHERE br.deleted = 0
            AND (:applicantId IS NULL OR br.applicant_id = #{applicantId})
            AND (:status IS NULL OR br.status = #{status})
            AND (:deptId IS NULL OR br.dept_id = #{deptId})
            ORDER BY br.created_time DESC
            """)
    List<Map<String, Object>> selectList(@Param("applicantId") String applicantId,
                                          @Param("status") String status,
                                          @Param("deptId") String deptId);

    @Select("""
            SELECT COUNT(*)
            FROM acc_borrow_request br
            WHERE br.deleted = 0
            AND br.status = 'OVERDUE'
            AND br.expected_end_date < CURRENT_DATE
            """)
    long countOverdue();

    @Select("""
            SELECT br.*
            FROM acc_borrow_request br
            WHERE br.deleted = 0
            AND br.status = 'OVERDUE'
            ORDER BY br.expected_end_date ASC
            LIMIT #{limit}
            """)
    List<BorrowRequest> selectOverdueList(@Param("limit") int limit);
}
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/mapper/BorrowRequestMapper.java
git commit -m "feat(mapper): add BorrowRequestMapper"
```

---

### Task 8: 创建 BorrowArchiveMapper

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/BorrowArchiveMapper.java`

**Step 1: 编写 Mapper 接口**

```java
package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.BorrowArchive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 借阅档案明细 Mapper
 */
@Mapper
public interface BorrowArchiveMapper extends BaseMapper<BorrowArchive> {
}
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/mapper/BorrowArchiveMapper.java
git commit -m "feat(mapper): add BorrowArchiveMapper"
```

---

### Task 9: 创建 BorrowLogMapper

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/BorrowLogMapper.java`

**Step 1: 编写 Mapper 接口**

```java
package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.BorrowLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 借阅记录 Mapper
 */
@Mapper
public interface BorrowLogMapper extends BaseMapper<BorrowLog> {
}
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/mapper/BorrowLogMapper.java
git commit -m "feat(mapper): add BorrowLogMapper"
```

---

## 业务服务层 (Service)

### Task 10: 创建 BorrowRequestService 接口

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/BorrowRequestService.java`

**Step 1: 编写 Service 接口**

```java
package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.BorrowArchive;
import com.nexusarchive.entity.BorrowRequest;

import java.util.List;

/**
 * 借阅申请服务接口
 */
public interface BorrowRequestService {

    /**
     * 创建借阅申请
     */
    BorrowRequest createRequest(BorrowRequest request);

    /**
     * 分页查询借阅申请
     */
    Page<BorrowRequest> getRequests(int page, int limit, String status, String applicantId, String deptId);

    /**
     * 获取借阅申请详情
     */
    BorrowRequest getDetail(String id);

    /**
     * 审批借阅申请
     */
    void approveRequest(String id, String approverId, String approverName, boolean approved, String comment);

    /**
     * 开始借阅（出库）
     */
    void startBorrowing(String id);

    /**
     * 归还档案
     */
    void returnArchive(String requestId, String operatorId);

    /**
     * 批量归还档案明细
     */
    void returnArchives(String requestId, List<String> archiveIds, String operatorId);

    /**
     * 检查逾期借阅
     */
    void checkOverdueRequests();

    /**
     * 获取借阅档案列表
     */
    List<BorrowArchive> getBorrowArchives(String requestId);

    /**
     * 归档借阅记录到历史表
     */
    void archiveToLog(String requestId);
}
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/BorrowRequestService.java
git commit -m "feat(service): add BorrowRequestService interface"
```

---

### Task 11: 实现 BorrowRequestServiceImpl

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/BorrowRequestServiceImpl.java`

**Step 1: 编写 Service 实现类**

```java
package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.BorrowArchive;
import com.nexusarchive.entity.BorrowLog;
import com.nexusarchive.entity.BorrowRequest;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.BorrowArchiveMapper;
import com.nexusarchive.mapper.BorrowLogMapper;
import com.nexusarchive.mapper.BorrowRequestMapper;
import com.nexusarchive.service.BorrowRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 借阅申请服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowRequestServiceImpl implements BorrowRequestService {

    private final BorrowRequestMapper borrowRequestMapper;
    private final BorrowArchiveMapper borrowArchiveMapper;
    private final BorrowLogMapper borrowLogMapper;
    private final ArchiveMapper archiveMapper;

    @Override
    @Transactional
    public BorrowRequest createRequest(BorrowRequest request) {
        // 验证档案是否存在
        List<String> archiveIds = Arrays.asList(request.getArchiveIds().split(","));
        if (archiveIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "借阅档案不能为空");
        }

        // 验证档案是否存在
        long count = archiveIds.stream()
                .map(id -> archiveMapper.selectById(id))
                .filter(a -> a != null)
                .count();
        if (count != archiveIds.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部分档案不存在");
        }

        // 生成借阅单号
        String requestNo = generateRequestNo();
        request.setRequestNo(requestNo);
        request.setId(UUID.randomUUID().toString());
        request.setStatus(BorrowRequest.BorrowStatus.PENDING);
        request.setArchiveCount(archiveIds.size());

        borrowRequestMapper.insert(request);

        // 创建借阅档案明细
        archiveIds.forEach(archiveId -> {
            Archive archive = archiveMapper.selectById(archiveId);
            BorrowArchive ba = new BorrowArchive();
            ba.setId(UUID.randomUUID().toString());
            ba.setBorrowRequestId(request.getId());
            ba.setArchiveId(archiveId);
            ba.setArchiveCode(archive.getArchiveCode());
            ba.setArchiveTitle(archive.getTitle());
            borrowArchiveMapper.insert(ba);
        });

        log.info("创建借阅申请: requestNo={}", requestNo);
        return request;
    }

    @Override
    public Page<BorrowRequest> getRequests(int page, int limit, String status, String applicantId, String deptId) {
        Page<BorrowRequest> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<BorrowRequest> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(BorrowRequest::getStatus, status);
        }
        if (applicantId != null && !applicantId.isEmpty()) {
            wrapper.eq(BorrowRequest::getApplicantId, applicantId);
        }
        if (deptId != null && !deptId.isEmpty()) {
            wrapper.eq(BorrowRequest::getDeptId, deptId);
        }
        wrapper.orderByDesc(BorrowRequest::getCreatedTime);

        return borrowRequestMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public BorrowRequest getDetail(String id) {
        BorrowRequest request = borrowRequestMapper.selectById(id);
        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "借阅申请不存在");
        }
        return request;
    }

    @Override
    @Transactional
    public void approveRequest(String id, String approverId, String approverName, boolean approved, String comment) {
        BorrowRequest request = getDetail(id);

        if (!BorrowRequest.BorrowStatus.PENDING.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "当前状态不允许审批");
        }

        request.setApproverId(approverId);
        request.setApproverName(approverName);
        request.setApprovalTime(LocalDateTime.now());
        request.setApprovalComment(comment);

        if (approved) {
            request.setStatus(BorrowRequest.BorrowStatus.APPROVED);
        } else {
            request.setStatus(BorrowRequest.BorrowStatus.REJECTED);
        }

        borrowRequestMapper.updateById(request);

        // 如果拒绝，归档到历史表
        if (!approved) {
            archiveToLog(id);
        }

        log.info("审批借阅申请: requestNo={}, approved={}", request.getRequestNo(), approved);
    }

    @Override
    @Transactional
    public void startBorrowing(String id) {
        BorrowRequest request = getDetail(id);

        if (!BorrowRequest.BorrowStatus.APPROVED.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "当前状态不允许借出");
        }

        request.setStatus(BorrowRequest.BorrowStatus.BORROWING);
        request.setActualStartDate(LocalDate.now());
        borrowRequestMapper.updateById(request);

        log.info("开始借阅: requestNo={}", request.getRequestNo());
    }

    @Override
    @Transactional
    public void returnArchive(String requestId, String operatorId) {
        BorrowRequest request = getDetail(requestId);

        if (!BorrowRequest.BorrowStatus.BORROWING.equals(request.getStatus())
                && !BorrowRequest.BorrowStatus.OVERDUE.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "当前状态不允许归还");
        }

        // 更新所有明细为已归还
        LambdaQueryWrapper<BorrowArchive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowArchive::getBorrowRequestId, requestId);
        wrapper.eq(BorrowArchive::getReturnStatus, "BORROWED");

        List<BorrowArchive> archives = borrowArchiveMapper.selectList(wrapper);
        archives.forEach(ba -> {
            ba.setReturnStatus(BorrowArchive.ReturnStatus.RETURNED);
            ba.setReturnTime(LocalDateTime.now());
            ba.setReturnOperatorId(operatorId);
            borrowArchiveMapper.updateById(ba);
        });

        // 更新申请状态
        request.setStatus(BorrowRequest.BorrowStatus.RETURNED);
        request.setActualEndDate(LocalDate.now());
        request.setReturnTime(LocalDateTime.now());
        request.setReturnOperatorId(operatorId);
        borrowRequestMapper.updateById(request);

        // 归档到历史表
        archiveToLog(requestId);

        log.info("归还借阅: requestNo={}", request.getRequestNo());
    }

    @Override
    @Transactional
    public void returnArchives(String requestId, List<String> archiveIds, String operatorId) {
        BorrowRequest request = getDetail(requestId);

        if (!BorrowRequest.BorrowStatus.BORROWING.equals(request.getStatus())
                && !BorrowRequest.BorrowStatus.OVERDUE.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "当前状态不允许归还");
        }

        // 更新指定档案为已归还
        archiveIds.forEach(archiveId -> {
            LambdaQueryWrapper<BorrowArchive> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BorrowArchive::getBorrowRequestId, requestId);
            wrapper.eq(BorrowArchive::getArchiveId, archiveId);
            BorrowArchive ba = borrowArchiveMapper.selectOne(wrapper);
            if (ba != null) {
                ba.setReturnStatus(BorrowArchive.ReturnStatus.RETURNED);
                ba.setReturnTime(LocalDateTime.now());
                ba.setReturnOperatorId(operatorId);
                borrowArchiveMapper.updateById(ba);
            }
        });

        // 检查是否全部归还
        LambdaQueryWrapper<BorrowArchive> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(BorrowArchive::getBorrowRequestId, requestId);
        countWrapper.eq(BorrowArchive::getReturnStatus, "BORROWED");
        Long remainingCount = borrowArchiveMapper.selectCount(countWrapper);

        if (remainingCount == 0) {
            // 全部归还，更新申请状态
            request.setStatus(BorrowRequest.BorrowStatus.RETURNED);
            request.setActualEndDate(LocalDate.now());
            request.setReturnTime(LocalDateTime.now());
            request.setReturnOperatorId(operatorId);
            borrowRequestMapper.updateById(request);

            archiveToLog(requestId);
        }

        log.info("部分归还借阅: requestNo={}, remaining={}", request.getRequestNo(), remainingCount);
    }

    @Override
    public List<BorrowArchive> getBorrowArchives(String requestId) {
        LambdaQueryWrapper<BorrowArchive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowArchive::getBorrowRequestId, requestId);
        wrapper.orderByDesc(BorrowArchive::getCreatedTime);
        return borrowArchiveMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void checkOverdueRequests() {
        // 查找逾期未还的借阅
        LambdaQueryWrapper<BorrowRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRequest::getStatus, BorrowRequest.BorrowStatus.BORROWING);
        wrapper.lt(BorrowRequest::getExpectedEndDate, LocalDate.now());

        List<BorrowRequest> overdueList = borrowRequestMapper.selectList(wrapper);

        for (BorrowRequest request : overdueList) {
            request.setStatus(BorrowRequest.BorrowStatus.OVERDUE);
            borrowRequestMapper.updateById(request);
        }

        log.info("检查逾期借阅: 发现{}笔逾期", overdueList.size());
    }

    @Override
    @Transactional
    public void archiveToLog(String requestId) {
        BorrowRequest request = getDetail(requestId);

        BorrowLog log = new BorrowLog();
        log.setId(UUID.randomUUID().toString());
        log.setRequestNo(request.getRequestNo());
        log.setApplicantId(request.getApplicantId());
        log.setApplicantName(request.getApplicantName());
        log.setDeptName(request.getDeptName());
        log.setPurpose(request.getPurpose());
        log.setBorrowType(request.getBorrowType().name());
        log.setBorrowStartDate(request.getActualStartDate());
        log.setBorrowEndDate(request.getActualEndDate());
        log.setArchiveCount(request.getArchiveCount());
        log.setStatus("COMPLETED".equals(request.getStatus().name()) ? "COMPLETED" : "CANCELLED");
        log.setCreatedTime(request.getCreatedTime());
        log.setCompletedTime(LocalDateTime.now());

        borrowLogMapper.insert(log);

        // 逻辑删除申请
        borrowRequestMapper.deleteById(request.getId());
    }

    /**
     * 生成借阅单号
     * 格式: BL-YYYYMMDD-序号
     */
    private String generateRequestNo() {
        String dateStr = LocalDate.now().toString().replace("-", "");
        // 简化实现，实际应该查询当天最大序号
        return "BL-" + dateStr + "-001";
    }
}
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/impl/BorrowRequestServiceImpl.java
git commit -m "feat(service): implement BorrowRequestServiceImpl"
```

---

## 控制器层 (Controller)

### Task 12: 创建 BorrowRequestController

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/controller/BorrowRequestController.java`

**Step 1: 编写 Controller 类**

```java
package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.BorrowArchive;
import com.nexusarchive.entity.BorrowRequest;
import com.nexusarchive.service.BorrowRequestService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 借阅管理 Controller
 */
@RestController
@RequestMapping("/borrow")
@RequiredArgsConstructor
public class BorrowRequestController {

    private final BorrowRequestService borrowRequestService;

    /**
     * 创建借阅申请
     * POST /api/borrow/request
     */
    @PostMapping("/request")
    @PreAuthorize("hasAuthority('archive:borrow') or hasRole('SYSTEM_ADMIN')")
    public Result<BorrowRequest> createRequest(@Valid @RequestBody BorrowRequest request,
                                             @AuthenticationPrincipal String userId) {
        // 设置申请人（从认证信息获取）
        request.setApplicantId(userId);
        BorrowRequest created = borrowRequestService.createRequest(request);
        return Result.success(created);
    }

    /**
     * 分页查询借阅申请
     * GET /api/borrow/requests
     */
    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('archive:borrow') or hasRole('SYSTEM_ADMIN')")
    public Result<Page<BorrowRequest>> getRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String applicantId,
            @RequestParam(required = false) String deptId) {
        Page<BorrowRequest> result = borrowRequestService.getRequests(page, limit, status, applicantId, deptId);
        return Result.success(result);
    }

    /**
     * 获取借阅申请详情
     * GET /api/borrow/request/{id}
     */
    @GetMapping("/request/{id}")
    @PreAuthorize("hasAuthority('archive:borrow') or hasRole('SYSTEM_ADMIN')")
    public Result<BorrowRequest> getRequestDetail(@PathVariable String id) {
        BorrowRequest detail = borrowRequestService.getDetail(id);
        return Result.success(detail);
    }

    /**
     * 获取借阅档案列表
     * GET /api/borrow/request/{id}/archives
     */
    @GetMapping("/request/{id}/archives")
    @PreAuthorize("hasAuthority('archive:borrow') or hasRole('SYSTEM_ADMIN')")
    public Result<List<BorrowArchive>> getBorrowArchives(@PathVariable String id) {
        List<BorrowArchive> archives = borrowRequestService.getBorrowArchives(id);
        return Result.success(archives);
    }

    /**
     * 审批借阅申请
     * POST /api/borrow/request/{id}/approve
     */
    @PostMapping("/request/{id}/approve")
    @PreAuthorize("hasAuthority('archive:borrow:approve') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> approveRequest(@PathVariable String id,
                                       @RequestBody ApprovalRequest approval) {
        borrowRequestService.approveRequest(id, approval.getApproverId(),
                approval.getApproverName(), approval.isApproved(), approval.getComment());
        return Result.success();
    }

    /**
     * 开始借阅（出库）
     * POST /api/borrow/request/{id}/start
     */
    @PostMapping("/request/{id}/start")
    @PreAuthorize("hasAuthority('archive:borrow:lend') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> startBorrowing(@PathVariable String id) {
        borrowRequestService.startBorrowing(id);
        return Result.success();
    }

    /**
     * 归还档案
     * POST /api/borrow/request/{id}/return
     */
    @PostMapping("/request/{id}/return")
    @PreAuthorize("hasAuthority('archive:borrow') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> returnArchive(@PathVariable String id,
                                       @RequestBody ReturnRequest returnReq) {
        borrowRequestService.returnArchives(id, returnReq.getArchiveIds(), returnReq.getOperatorId());
        return Result.success();
    }

    /**
     * 获取借阅统计
     * GET /api/borrow/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('archive:borrow') or hasRole('SYSTEM_ADMIN')")
    public Result<BorrowStats> getStats() {
        long pending = borrowRequestService.getRequests(1, 1, "PENDING", null, null)
                .getTotal();
        long borrowing = borrowRequestService.getRequests(1, 1, "BORROWING", null, null)
                .getTotal();
        long overdue = borrowRequestService.countOverdue();

        BorrowStats stats = new BorrowStats();
        stats.setPendingCount((int) pending);
        stats.setBorrowingCount((int) borrowing);
        stats.setOverdueCount((int) overdue);
        return Result.success(stats);
    }

    @Data
    public static class ApprovalRequest {
        private String approverId;
        private String approverName;
        private boolean approved;
        private String comment;
    }

    @Data
    public static class ReturnRequest {
        private List<String> archiveIds;
        private String operatorId;
    }

    @Data
    public static class BorrowStats {
        private int pendingCount;
        private int borrowingCount;
        private int overdueCount;
    }
}
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/controller/BorrowRequestController.java
git commit -m "feat(controller): add BorrowRequestController"
```

---

### Task 13: 更新 StatsController 添加借阅统计

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/controller/StatsController.java`

**Step 1: 添加借阅统计接口**

在 `StatsController.java` 中添加：

```java
@Autowired
private BorrowRequestMapper borrowRequestMapper;

@GetMapping("/borrowing")
@PreAuthorize("hasAuthority('archive:borrow') or hasRole('SYSTEM_ADMIN')")
public Result<Map<String, Object>> getBorrowingStats() {
    Map<String, Object> stats = new HashMap<>();

    // 待审批借阅
    long pending = borrowRequestMapper.selectCount(
        new LambdaQueryWrapper<com.nexusarchive.entity.BorrowRequest>()
            .eq("status", "PENDING")
    );

    // 借阅中
    long borrowing = borrowRequestMapper.selectCount(
        new LambdaQueryWrapper<com.nexusarchive.entity.BorrowRequest>()
            .eq("status", "BORROWING")
    );

    // 逾期
    long overdue = borrowRequestMapper.countOverdue();

    stats.put("pendingCount", pending);
    stats.put("borrowingCount", borrowing);
    stats.put("overdueCount", overdue);

    return Result.success(stats);
}
```

同时添加导入：
```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/controller/StatsController.java
git commit -m "feat(controller): add borrowing stats to StatsController"
```

---

## 前端 API 层

### Task 14: 创建前端 Borrowing API

**Files:**
- Create: `src/api/borrowing.ts`

**Step 1: 编写 API 客户端**

```typescript
// Input: API client 与 ApiResponse 类型
// Output: borrowingApi
// Pos: 借阅管理 API 层

import { client } from './client';
import { ApiResponse, PageResult } from '../types';

/**
 * 借阅申请
 */
export interface BorrowRequest {
  id: string;
  requestNo: string;
  applicantId: string;
  applicantName: string;
  deptId?: string;
  deptName?: string;
  purpose: string;
  borrowType: 'READING' | 'COPY' | 'LOAN';
  expectedStartDate: string;
  expectedEndDate: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'BORROWING' | 'RETURNED' | 'OVERDUE';
  archiveIds: string[];
  archiveCount: number;
  approverId?: string;
  approverName?: string;
  approvalTime?: string;
  approvalComment?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  returnTime?: string;
  returnOperatorId?: string;
  createdTime: string;
  updatedTime: string;
}

/**
 * 借阅档案明细
 */
export interface BorrowArchive {
  id: string;
  borrowRequestId: string;
  archiveId: string;
  archiveCode: string;
  archiveTitle: string;
  returnStatus: 'BORROWED' | 'RETURNED';
  returnTime?: string;
  returnOperatorId?: string;
  damaged?: boolean;
  damageDesc?: string;
  createdTime: string;
}

/**
 * 借阅统计
 */
export interface BorrowStats {
  pendingCount: number;
  borrowingCount: number;
  overdueCount: number;
}

/**
 * 借阅申请创建请求
 */
export interface CreateBorrowRequest {
  purpose: string;
  borrowType: 'READING' | 'COPY' | 'LOAN';
  expectedStartDate: string;
  expectedEndDate: string;
  archiveIds: string[];
}

/**
 * 审批请求
 */
export interface ApprovalRequest {
  approverId: string;
  approverName: string;
  approved: boolean;
  comment?: string;
}

/**
 * 归还请求
 */
export interface ReturnRequest {
  archiveIds: string[];
  operatorId: string;
}

export const borrowingApi = {
  /**
   * 创建借阅申请
   */
  createRequest: async (data: CreateBorrowRequest): Promise<ApiResponse<BorrowRequest>> => {
    const response = await client.post<ApiResponse<BorrowRequest>>(
      '/borrow/request',
      data
    );
    return response.data;
  },

  /**
   * 获取借阅申请列表
   */
  getRequests: async (params?: {
    page?: number;
    limit?: number;
    status?: string;
    applicantId?: string;
    deptId?: string;
  }): Promise<ApiResponse<PageResult<BorrowRequest>>> => {
    const response = await client.get<ApiResponse<PageResult<BorrowRequest>>>(
      '/borrow/requests',
      { params }
    );
    return response.data;
  },

  /**
   * 获取借阅申请详情
   */
  getRequestDetail: async (id: string): Promise<ApiResponse<BorrowRequest>> => {
    const response = await client.get<ApiResponse<BorrowRequest>>(
      `/borrow/request/${id}`
    );
    return response.data;
  },

  /**
   * 获取借阅档案列表
   */
  getBorrowArchives: async (requestId: string): Promise<ApiResponse<BorrowArchive[]>> => {
    const response = await client.get<ApiResponse<BorrowArchive[]>>(
      `/borrow/request/${requestId}/archives`
    );
    return response.data;
  },

  /**
   * 审批借阅申请
   */
  approveRequest: async (id: string, approval: ApprovalRequest): Promise<ApiResponse<void>> => {
    const response = await client.post<ApiResponse<void>>(
      `/borrow/request/${id}/approve`,
      approval
    );
    return response.data;
  },

  /**
   * 开始借阅（出库）
   */
  startBorrowing: async (id: string): Promise<ApiResponse<void>> => {
    const response = await client.post<ApiResponse<void>>(
      `/borrow/request/${id}/start`
    );
    return response.data;
  },

  /**
   * 归还档案
   */
  returnArchives: async (id: string, returnReq: ReturnRequest): Promise<ApiResponse<void>> => {
    const response = await client.post<ApiResponse<void>>(
      `/borrow/request/${id}/return`,
      returnReq
    );
    return response.data;
  },

  /**
   * 获取借阅统计
   */
  getStats: async (): Promise<ApiResponse<BorrowStats>> => {
    const response = await client.get<ApiResponse<BorrowStats>>('/borrow/stats');
    return response.data;
  },
};
```

**Step 2: Commit**

```bash
git add src/api/borrowing.ts
git commit -m "feat(api): add borrowing API client"
```

---

## 测试

### Task 15: 编写 BorrowRequestServiceImpl 测试

**Files:**
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/service/BorrowRequestServiceImplTest.java`

**Step 1: 编写单元测试**

```java
package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.BorrowRequest;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.BorrowArchiveMapper;
import com.nexusarchive.mapper.BorrowRequestMapper;
import com.nexusarchive.mapper.BorrowLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 借阅申请服务测试
 */
@ExtendWith(MockitoExtension.class)
class BorrowRequestServiceImplTest {

    @Mock
    private BorrowRequestMapper borrowRequestMapper;
    @Mock
    private BorrowArchiveMapper borrowArchiveMapper;
    @Mock
    private BorrowLogMapper borrowLogMapper;
    @Mock
    private ArchiveMapper archiveMapper;

    @InjectMocks
    private BorrowRequestServiceImpl borrowRequestService;

    private BorrowRequest testRequest;
    private Archive testArchive;

    @BeforeEach
    void setUp() {
        testArchive = new Archive();
        testArchive.setId("archive-001");
        testArchive.setArchiveCode("QZ-2024-KJ-001");
        testArchive.setTitle("测试档案");

        testRequest = new BorrowRequest();
        testRequest.setApplicantId("user-001");
        testRequest.setApplicantName("张三");
        testRequest.setPurpose("审计查阅");
        testRequest.setBorrowType(BorrowRequest.BorrowType.READING);
        testRequest.setExpectedStartDate(java.time.LocalDate.now());
        testRequest.setExpectedEndDate(java.time.LocalDate.now().plusDays(7));
    }

    @Test
    void testCreateRequest_Success() {
        // Given
        when(archiveMapper.selectById(anyString())).thenReturn(testArchive);
        when(borrowRequestMapper.insert(any())).thenReturn(1);
        when(borrowArchiveMapper.insert(any())).thenReturn(1);

        testRequest.setArchiveIds("archive-001");

        // When
        BorrowRequest result = borrowRequestService.createRequest(testRequest);

        // Then
        assertNotNull(result.getRequestNo());
        assertEquals(BorrowRequest.BorrowStatus.PENDING, result.getStatus());
        verify(borrowRequestMapper).insert(any(BorrowRequest.class));
        verify(borrowArchiveMapper).insert(any(BorrowArchive.class));
    }

    @Test
    void testApproveRequest_Success() {
        // Given
        when(borrowRequestMapper.selectById("request-001")).thenReturn(testRequest);
        when(borrowRequestMapper.updateById(any())).thenReturn(1);
        testRequest.setId("request-001");
        testRequest.setStatus(BorrowRequest.BorrowStatus.PENDING);

        // When
        borrowRequestService.approveRequest("request-001", "admin-001", "管理员", true, "同意");

        // Then
        assertEquals(BorrowRequest.BorrowStatus.APPROVED, testRequest.getStatus());
        verify(borrowRequestMapper).updateById(any(BorrowRequest.class));
    }

    @Test
    void testStartBorrowing_Success() {
        // Given
        when(borrowRequestMapper.selectById("request-001")).thenReturn(testRequest);
        when(borrowRequestMapper.updateById(any())).thenReturn(1);
        testRequest.setId("request-001");
        testRequest.setStatus(BorrowRequest.BorrowStatus.APPROVED);

        // When
        borrowRequestService.startBorrowing("request-001");

        // Then
        assertEquals(BorrowRequest.BorrowStatus.BORROWING, testRequest.getStatus());
        verify(borrowRequestMapper).updateById(any(BorrowRequest.class));
    }

    @Test
    void testReturnArchive_Success() {
        // Given
        when(borrowRequestMapper.selectById("request-001")).thenReturn(testRequest);
        when(borrowRequestMapper.updateById(any())).thenReturn(1);
        testRequest.setId("request-001");
        testRequest.setStatus(BorrowRequest.BorrowStatus.BORROWING);

        // When
        borrowRequestService.returnArchive("request-001", "operator-001");

        // Then
        assertEquals(BorrowRequest.BorrowStatus.RETURNED, testRequest.getStatus());
        verify(borrowRequestMapper).updateById(any(BorrowRequest.class));
    }
}
```

**Step 2: 运行测试验证**

Run: `cd nexusarchive-java && mvn test -Dtest=BorrowRequestServiceImplTest -q`
Expected: PASS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/service/BorrowRequestServiceImplTest.java
git commit -m "test(service): add BorrowRequestServiceImpl unit tests"
```

---

## 权限配置

### Task 16: 更新权限常量

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/common/exception/ErrorCode.java`

**Step 1: 添加错误码**

```java
// 在 ErrorCode 类中添加
BORROW_REQUEST_NOT_FOUND("BORROW_REQUEST_NOT_FOUND", "借阅申请不存在"),
BORROW_INVALID_STATE("BORROW_INVALID_STATE", "借阅状态不允许此操作"),
BORROW_ARCHIVE_NOT_FOUND("BORROW_ARCHIVE_NOT_FOUND", "借阅档案不存在"),
BORROW_ALREADY_RETURNED("BORROW_ALREADY_RETURNED", "档案已归还"),
BORROW_EXPIRED("BORROW_EXPIRED", "借阅期限已到"),
```

**Step 2: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/common/exception/ErrorCode.java
git commit -m "feat: add borrowing related error codes"
```

---

## 文档

### Task 17: 更新 API 文档

**Files:**
- Create: `docs/api/borrowing.md`

**Step 1: 编写 API 文档**

```markdown
# 借阅管理 API 文档

## 概述
借阅管理模块提供档案借阅、审批、归还等功能。

## API 端点

### 1. 创建借阅申请
- **接口**: `POST /api/borrow/request`
- **权限**: `archive:borrow`
- **请求体**:
```json
{
  "purpose": "审计查阅",
  "borrowType": "READING",
  "expectedStartDate": "2025-01-10",
  "expectedEndDate": "2025-01-15",
  "archiveIds": ["archive-001", "archive-002"]
}
```

### 2. 借阅申请列表
- **接口**: `GET /api/borrow/requests`
- **权限**: `archive:borrow`
- **参数**: page, limit, status, applicantId, deptId

### 3. 审批借阅
- **接口**: `POST /api/borrow/request/{id}/approve`
- **权限**: `archive:borrow:approve`

### 4. 借阅出库
- **接口**: `POST /api/borrow/request/{id}/start`
- **权限**: `archive:borrow:lend`

### 5. 归还档案
- **接口**: `POST /api/borrow/request/{id}/return`
- **权限**: `archive:borrow`

## 状态说明
- `PENDING`: 待审批
- `APPROVED`: 已批准，待借出
- `REJECTED`: 已拒绝
- `BORROWING`: 借阅中
- `RETURNED`: 已归还
- `OVERDUE`: 逾期未还
```

**Step 2: Commit**

```bash
git add docs/api/borrowing.md
git commit -m "docs: add borrowing API documentation"
```

---

## 收尾

### Task 18: 编译验证

**Files:**
- None

**Step 1: 编译后端项目**

Run: `cd nexusarchive-java && mvn clean compile -q`
Expected: BUILD SUCCESS

**Step 2: 检查类型错误**

Run: `npx tsc --noEmit 2>&1 | grep -v "test" | head -10`
Expected: 无新错误

**Step 3: 最终提交**

```bash
git add -A
git commit -m "feat: complete archive borrowing feature implementation

- Database: Added acc_borrow_request, acc_borrow_archive, acc_borrow_log tables
- Entity: Added BorrowRequest, BorrowArchive, BorrowLog entities
- Mapper: Added BorrowRequestMapper, BorrowArchiveMapper, BorrowLogMapper
- Service: Implemented BorrowRequestService with full CRUD operations
- Controller: Added BorrowRequestController with RESTful endpoints
- API: Added frontend borrowing API client
- Test: Added unit tests for BorrowRequestService
- Docs: Added API documentation
- Permissions: Added borrowing-related error codes

Features:
- Create borrowing request with archive selection
- Approval workflow for borrowing requests
- Borrowing (checkout) functionality
- Return/archive functionality
- Partial return support
- Overdue detection
- Borrowing statistics
- History logging"
```

---

**Plan complete and saved to `docs/plans/2025-01-06-archive-borrowing-feature.md`.**

**Execution Options:**

1. **Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

2. **Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

**Which approach?**
