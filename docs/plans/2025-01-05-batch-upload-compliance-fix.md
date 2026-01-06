# 批量上传合规性修改实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复批量上传功能的合规性问题，将定位从"直接上传凭证"改为"原始凭证附件上传器"，确保符合 DA/T 94-2022《电子会计档案管理规范》要求。

**Architecture:**
- 上传完成后立即创建 `acc_archive` 档案记录（状态：PENDING_METADATA）
- 异步触发智能解析更新元数据
- 前端引导用户到凭证关联页面
- 提供手动归档兜底机制

**Tech Stack:**
- Backend: Spring Boot 3.1.6, Java 17, MyBatis-Plus 3.5.7
- Frontend: React 19, TypeScript 5.8, Ant Design 6
- Compliance: DA/T 94-2022, GB/T 39362-2020

---

## 背景与问题

### 问题描述
当前批量上传功能允许用户直接上传凭证文件，但根据 **DA/T 94-2022《电子会计档案管理规范》** 第14条要求：
> 在电子会计资料归档和电子会计档案管理过程中应**同时捕获、归档和管理元数据**

直接上传的文件缺少完整元数据，存在合规风险。

### 解决方案定位
将批量上传功能重新定位为 **"原始凭证附件上传器"**：
- 用于上传发票扫描件、合同、银行回单等**原始凭证附件**
- 上传后需要关联到已有的记账凭证
- 系统自动尝试OCR提取元数据，用户需确认/补录

---

## Phase 1: 数据库迁移 (P0)

### Task 1.1: 添加批次文件关联字段

**Files:**
- Create: \`nexusarchive-java/src/main/resources/db/migration/V81__add_batch_file_archive_id.sql\`

**Step 1: 创建迁移SQL文件**

\`\`\`sql
-- ============================================================
-- 批量上传合规性修改
-- 添加 collection_batch_file.archive_id 字段关联档案记录
-- 符合 DA/T 94-2022 元数据同步捕获要求
-- ============================================================

-- 添加档案关联字段
ALTER TABLE public.collection_batch_file
ADD COLUMN archive_id VARCHAR(50) COMMENT '关联的档案ID (acc_archive.id)';

-- 创建索引优化查询
CREATE INDEX idx_collection_batch_file_archive_id
ON public.collection_batch_file(archive_id);

-- 添加注释
COMMENT ON COLUMN public.collection_batch_file.archive_id IS '上传完成后立即创建的档案记录ID，用于凭证关联';
\`\`\`

**Step 2: 验证迁移**

Run: \`mvn flyway:migrate\` 或重启应用自动迁移
Expected: 表新增 \`archive_id\` 字段，索引创建成功

**Step 3: 提交**

\`\`\`bash
git add nexusarchive-java/src/main/resources/db/migration/V81__add_batch_file_archive_id.sql
git commit -m "feat(db): add archive_id to collection_batch_file for compliance"
\`\`\`

---

## Phase 2: 后端核心功能 (P0)

### Task 2.1: 创建批次转换服务

**Files:**
- Create: \`nexusarchive-java/src/main/java/com/nexusarchive/service/BatchToArchiveService.java\`
- Create: \`nexusarchive-java/src/main/java/com/nexusarchive/service/impl/BatchToArchiveServiceImpl.java\`
- Test: \`nexusarchive-java/src/test/java/com/nexusarchive/service/BatchToArchiveServiceTest.java\`

**Step 1: 创建服务接口**

\`\`\`java
// File: nexusarchive-java/src/main/java/com/nexusarchive/service/BatchToArchiveService.java
package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;

/**
 * 批次文件转换服务
 *
 * 负责将批量上传的文件转换为 acc_archive 档案记录
 * 符合 DA/T 94-2022 元数据同步捕获要求
 */
public interface BatchToArchiveService {

    /**
     * 从批次信息创建档案记录
     *
     * @param fileContent 文件内容
     * @param batch 批次信息
     * @return 创建的档案记录
     */
    Archive createArchiveFromBatch(ArcFileContent fileContent, CollectionBatch batch);

    /**
     * 批量完成时，标记所有文件的预归档状态
     *
     * @param batchId 批次ID
     */
    void markBatchAsPendingMetadata(Long batchId);

    /**
     * 根据文件ID获取关联的档案ID
     *
     * @param fileId 文件ID
     * @return 档案ID，不存在返回null
     */
    String getArchiveIdByFileId(String fileId);
}
\`\`\`

**Step 2: 创建服务实现**

\`\`\`java
// File: nexusarchive-java/src/main/java/com/nexusarchive/service/impl/BatchToArchiveServiceImpl.java
package com.nexusarchive.service.impl;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.service.ArchivalCodeGenerator;
import com.nexusarchive.service.BatchToArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 批次文件转换服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchToArchiveServiceImpl implements BatchToArchiveService {

    private final ArchiveMapper archiveMapper;
    private final CollectionBatchFileMapper batchFileMapper;
    private final ArchivalCodeGenerator archivalCodeGenerator;

    @Override
    @Transactional
    public Archive createArchiveFromBatch(ArcFileContent fileContent, CollectionBatch batch) {
        log.info("创建档案记录: fileId={}, batchNo={}", fileContent.getId(), batch.getBatchNo());

        // 生成档号
        String archiveCode = archivalCodeGenerator.generate(
            batch.getFondsCode(),
            batch.getFiscalYear(),
            batch.getArchivalCategory()
        );

        // 创建档案记录（最小必填字段）
        Archive archive = new Archive();
        archive.setId(UUID.randomUUID().toString().replace("-", ""));
        archive.setArchiveCode(archiveCode);
        archive.setFondsNo(batch.getFondsCode());
        archive.setCategoryCode("AC04"); // 固定为"其他会计资料"（原始凭证附件）
        archive.setFiscalYear(batch.getFiscalYear());
        archive.setFiscalPeriod(batch.getFiscalPeriod());
        archive.setTitle(fileContent.getFileName()); // 初始使用文件名
        archive.setRetentionPeriod("30年"); // 默认保管期限
        archive.setOrgName("上传单位"); // TODO: 从全宗信息获取
        archive.setStatus(PreArchiveStatus.PENDING_METADATA.getCode());

        // 存储文件内容关联
        archive.setStoragePath(fileContent.getStoragePath());
        archive.setFileHash(fileContent.getFileHash());

        archiveMapper.insert(archive);

        log.info("档案记录创建成功: archiveId={}, archiveCode={}", archive.getId(), archiveCode);

        return archive;
    }

    @Override
    @Transactional
    public void markBatchAsPendingMetadata(Long batchId) {
        log.info("标记批次文件状态为待补录: batchId={}", batchId);
        // 更新批次中所有文件的关联档案ID
    }

    @Override
    public String getArchiveIdByFileId(String fileId) {
        CollectionBatchFile batchFile = batchFileMapper.selectByFileId(fileId);
        return batchFile != null ? batchFile.getArchiveId() : null;
    }
}
\`\`\`

**Step 3: 编写测试**

\`\`\`java
// File: nexusarchive-java/src/test/java/com/nexusarchive/service/BatchToArchiveServiceTest.java
package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@DisplayName("批次转换服务测试")
class BatchToArchiveServiceTest {

    @BeforeEach
    void setUp() {
        // 测试数据初始化
    }

    @Test
    @DisplayName("应该创建档案记录并设置待补录状态")
    void createArchiveFromBatch_ShouldCreateArchiveWithPendingMetadataStatus() {
        // Given & When & Then 实现略
        // 验证创建的档案记录 categoryCode = AC04
        // 验证 status = PENDING_METADATA
    }
}
\`\`\`

**Step 4: 运行测试验证**

Run: \`mvn test -Dtest=BatchToArchiveServiceTest\`
Expected: 测试通过

**Step 5: 提交**

\`\`\`bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/BatchToArchiveService.java \\
        nexusarchive-java/src/main/java/com/nexusarchive/service/impl/BatchToArchiveServiceImpl.java \\
        nexusarchive-java/src/test/java/com/nexusarchive/service/BatchToArchiveServiceTest.java
git commit -m "feat(service): add BatchToArchiveService for compliance fix"
\`\`\`

---

### Task 2.2: 修改文件上传逻辑

**Files:**
- Modify: \`nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java\`

**Step 1: 在文件上传方法中添加创建档案记录逻辑**

\`\`\`java
// 在 uploadFile 方法中，保存文件后添加创建档案记录的逻辑

@Override
@Transactional
public FileUploadResult uploadFile(Long batchId, MultipartFile file, Long userId) {
    // 1. 保存文件
    ArcFileContent fileContent = saveToStorage(file);

    // 2. 创建批次文件记录
    CollectionBatchFile batchFile = createBatchFile(batchId, fileContent);
    batchFileMapper.insert(batchFile);

    // ===== 新增：创建档案记录 =====
    CollectionBatch batch = batchMapper.selectById(batchId);
    Archive archive = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

    // 3. 更新批次文件记录，关联档案ID
    batchFile.setArchiveId(archive.getId());
    batchFileMapper.updateById(batchFile);

    // 4. 异步触发智能解析
    smartParserService.parseAndIndex(List.of(fileContent));

    // ... 返回逻辑 ...
}
\`\`\`

**Step 2: 添加依赖注入**

\`\`\`java
// 在类顶部添加
private final BatchToArchiveService batchToArchiveService;
private final SmartParserService smartParserService;

// 修改构造函数注入依赖
\`\`\`

**Step 3: 运行测试验证**

Run: \`mvn test -Dtest=CollectionBatchServiceTest\`
Expected: 现有测试通过，新逻辑正确执行

**Step 4: 提交**

\`\`\`bash
git add nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java
git commit -m "feat(service): create archive record immediately after file upload"
\`\`\`

---

## Phase 3: 前端界面修改 (P0)

### Task 3.1: 添加合规提示组件

**Files:**
- Create: \`src/pages/collection/components/ComplianceAlert.tsx\`

**Step 1: 创建合规提示组件**

\`\`\`typescript
// File: src/pages/collection/components/ComplianceAlert.tsx
import { Alert } from 'antd';

/**
 * 批量上传合规提示组件
 *
 * 根据 DA/T 94-2022 要求，电子会计资料归档需同时捕获元数据。
 * 本功能用于上传原始凭证附件，需在凭证关联页面匹配到记账凭证。
 */
export const ComplianceAlert: React.FC = () => {
  return (
    <Alert
      type="warning"
      showIcon
      closable
      className="mb-4"
      message={
        <span>
          <strong>合规提示：</strong>上传的文件将作为<strong>原始凭证附件</strong>处理，
          需要在凭证关联页面匹配到对应记账凭证后方可归档。
          未关联的文件可手动归档。
        </span>
      }
    />
  );
};
\`\`\`

**Step 2: 提交**

\`\`\`bash
git add src/pages/collection/components/ComplianceAlert.tsx \\
        src/pages/collection/BatchUploadView.tsx
git commit -m "feat(frontend): add compliance alert for batch upload"
\`\`\`

---

### Task 3.2: 修改上传完成页面

**Files:**
- Modify: \`src/pages/collection/BatchUploadView.tsx\`

**Step 1: 添加跳转到凭证关联按钮**

\`\`\`typescript
// 在完成步骤中添加跳转按钮
{step === 'complete' && (
  <div className="text-center py-8">
    <CheckCircle2 className="text-green-500 mx-auto mb-4" size={64} />
    <h2>上传完成！</h2>

    <Button type="primary" size="large" onClick={() => navigate('/system/pre-archive/link')}>
      前往凭证关联
    </Button>
    <Button onClick={() => navigate('/system/collection/upload')}>
      继续上传
    </Button>

    <Alert type="info" message="已为您创建档案记录，请在凭证关联页面匹配原始凭证" />
  </div>
)}
\`\`\`

**Step 2: 提交**

\`\`\`bash
git add src/pages/collection/BatchUploadView.tsx
git commit -m "feat(frontend): add navigate to voucher linking page after upload"
\`\`\`

---

## Phase 4: 兜底机制 (P1)

### Task 4.1: 创建手动归档弹窗

**Files:**
- Create: \`src/pages/archives/components/ManualArchiveModal.tsx\`

**Step 1: 创建手动归档弹窗组件**

\`\`\`typescript
// File: src/pages/archives/components/ManualArchiveModal.tsx
import { useState } from 'react';
import { Modal, Form, Input, Button, message } from 'antd';

/**
 * 手动归档弹窗
 *
 * 用于处理长期未关联的文件，允许用户补录元数据后直接归档
 */
export const ManualArchiveModal: React.FC<ManualArchiveModalProps> = ({
  visible,
  file,
  onSuccess,
  onCancel,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    // 实现略
  };

  return (
    <Modal title="手动归档" open={visible} onCancel={onCancel}>
      <Form form={form} layout="vertical">
        <Form.Item label="题名" name="title" rules={[{ required: true }]}>
          <Input defaultValue={file?.fileName} />
        </Form.Item>
        <Form.Item label="凭证号" name="voucherNo">
          <Input placeholder="如：记-001" />
        </Form.Item>
        {/* 其他字段... */}
      </Form>
    </Modal>
  );
};
\`\`\`

**Step 2: 提交**

\`\`\`bash
git add src/pages/archives/components/ManualArchiveModal.tsx
git commit -m "feat(frontend): add manual archive modal for unlinked files"
\`\`\`

---

## Phase 5: 测试与文档 (P1)

### Task 5.1: 更新批量上传文档

**Files:**
- Modify: \`docs/features/batch-upload.md\`

**Step 1: 更新功能概述**

\`\`\`markdown
# 批量上传功能

## 功能定位

- ✅ **用于**：上传原始凭证附件（发票、合同、回单等）
- ❌ **不用于**：直接上传记账凭证（应通过ERP同步）
- ⚠️ **注意**：上传后需在凭证关联页面匹配后方可正式归档
\`\`\`

**Step 2: 提交**

\`\`\`bash
git add docs/features/batch-upload.md
git commit -m "docs: update batch upload feature description for compliance"
\`\`\`

---

## 验收标准

### 功能验收
- [ ] 上传文件后在 \`acc_archive\` 表中创建记录
- [ ] 新上传的文件在凭证关联页面可见
- [ ] 智能解析成功后元数据更新
- [ ] 未关联文件可手动归档
- [ ] 页面显示合规提示

### 测试验收
- [ ] 后端单元测试通过
- [ ] API 集成测试通过
- [ ] 前端类型检查通过

---

**总工作量估算**：P0 约 7.5 小时，P0+P1 约 11.5 小时
