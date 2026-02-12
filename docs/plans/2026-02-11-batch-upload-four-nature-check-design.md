# 批量上传四性检测集成设计文档

**日期**: 2026-02-11
**作者**: Claude + 用户
**状态**: 待实施

---

## 背景

### 现状问题

1. **批量上传后没有触发四性检测**
   - 文件上传后状态为 `PENDING_CHECK`
   - 需要用户手动触发检测，或点击"完成上传"后直接变为 `READY_TO_ARCHIVE`（跳过检测）

2. **归档审批提交缺少控制**
   - 任何状态的文件都可以尝试提交归档审批
   - 后端会拒绝非 `READY_TO_ARCHIVE` 状态的提交
   - 前端没有给用户明确提示

### 用户诉求

- 批量上传完成后自动触发四性检测
- 只有通过检测的文件才能提交归档审批
- 界面给用户明确的状态提示和操作引导

---

## 设计目标

1. **自动化检测**：批次完成后自动触发四性检测，无需手动操作
2. **状态引导**：根据文件状态显示对应的操作按钮
3. **友好提示**：检测失败时在界面上显示失败原因
4. **流程闭环**：从上传到检测到归档的完整流程

---

## 状态流转设计

```
┌─────────────────────────────────────────────────────────────────┐
│                         批量上传流程                            │
└─────────────────────────────────────────────────────────────────┘

文件上传
    ↓
PENDING_CHECK (待检测)
    ↓
[用户点击"完成上传"]
    ↓
执行四性检测 (同步等待)
    ↓
    ├─────────────────┬─────────────────┐
    ↓                 ↓                 ↓
检测通过            检测失败          文件不存在
    ↓                 ↓                 ↓
READY_TO_ARCHIVE   NEEDS_ACTION     (错误处理)
(可归档)           (待处理)
    ↓                 ↓
可提交归档审批      显示失败原因
```

---

## 后端改造

### 1. CollectionBatchServiceImpl.completeBatch()

**位置**: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java`

**改造内容**：

```java
@Override
@Transactional
public BatchCompleteResult completeBatch(Long batchId, String userId) {
    log.info("完成批次: batchId={}, userId={}", batchId, userId);

    // 1. 验证批次
    CollectionBatch batch = batchMapper.selectById(batchId);
    if (batch == null) {
        throw new IllegalArgumentException("批次不存在");
    }

    // 全宗校验
    if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) {
        throw new BusinessException(403, "越权操作：非当前全宗数据");
    }

    // 2. 更新批次状态
    batch.setStatus(CollectionBatch.STATUS_UPLOADED);
    batch.setCompletedTime(LocalDateTime.now());
    batch.setLastModifiedTime(LocalDateTime.now());
    batchMapper.updateById(batch);

    // 3. 获取批次中所有文件
    List<CollectionBatchFile> batchFiles = batchFileMapper.selectList(
        new LambdaQueryWrapper<CollectionBatchFile>()
            .eq(CollectionBatchFile::getBatchId, batchId)
            .isNotNull(CollectionBatchFile::getFileId)
    );

    // 4. 执行四性检测（同步）
    BatchCheckResult checkResult = executeBatchCheck(batchFiles);

    // 5. 记录审计日志
    auditLogService.log(
        userId, userId, "COMPLETE_BATCH", "COLLECTION_BATCH",
        String.valueOf(batchId), "SUCCESS",
        String.format("完成批次上传: %s, 检测通过: %d, 失败: %d",
            batch.getBatchNo(), checkResult.getPassedCount(), checkResult.getFailedCount()),
        null
    );

    // 6. 返回结果（包含检测统计）
    return new BatchCompleteResult(
        batch.getId(),
        batch.getBatchNo(),
        batch.getStatus(),
        batch.getTotalFiles(),
        batch.getUploadedFiles(),
        checkResult.getPassedCount(),    // 新增
        checkResult.getFailedCount(),     // 新增
        checkResult.getFailedFiles()      // 新增
    );
}

/**
 * 批量执行四性检测
 */
private BatchCheckResult executeBatchCheck(List<CollectionBatchFile> batchFiles) {
    BatchCheckResult result = new BatchCheckResult();

    for (CollectionBatchFile batchFile : batchFiles) {
        try {
            ArcFileContent file = arcFileContentMapper.selectById(batchFile.getFileId());
            if (file == null) {
                continue;
            }

            // 调用四性检测服务
            FourNatureReport report = preArchiveCheckService.checkSingleFile(file.getId());

            // 根据检测结果更新状态
            String newStatus = (report.getStatus() == OverallStatus.PASS)
                ? PreArchiveStatus.READY_TO_ARCHIVE.getCode()
                : PreArchiveStatus.NEEDS_ACTION.getCode();

            file.setPreArchiveStatus(newStatus);
            file.setCheckedTime(LocalDateTime.now());
            arcFileContentMapper.updateById(file);

            if (report.getStatus() == OverallStatus.PASS) {
                result.addPassed(file.getId(), file.getFileName());
            } else {
                result.addFailed(file.getId(), file.getFileName(), getFailureReason(report));
            }

        } catch (Exception e) {
            log.error("检测文件失败: fileId={}, error={}", batchFile.getFileId(), e.getMessage());
            result.addFailed(batchFile.getFileId(), "未知", "检测异常: " + e.getMessage());
        }
    }

    return result;
}

/**
 * 从检测报告中提取失败原因（用于界面显示）
 */
private String getFailureReason(FourNatureReport report) {
    // 简化失败原因，用于前端显示
    if (report.getAuthenticity() != null &&
        report.getAuthenticity().getStatus() == OverallStatus.FAIL) {
        return "真实性检测失败";
    }
    if (report.getIntegrity() != null &&
        report.getIntegrity().getStatus() == OverallStatus.FAIL) {
        return "完整性检测失败";
    }
    if (report.getUsability() != null &&
        report.getUsability().getStatus() == OverallStatus.FAIL) {
        return "可用性检测失败";
    }
    if (report.getSafety() != null &&
        report.getSafety().getStatus() == OverallStatus.FAIL) {
        return "安全性检测失败";
    }
    return "检测未通过";
}
```

### 2. BatchCompleteResult DTO 扩展

**位置**: `nexusarchive-java/src/main/java/com/nexusarchive/dto/collection/BatchCompleteResult.java`

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchCompleteResult {
    private Long batchId;
    private String batchNo;
    private String status;
    private Integer totalFiles;
    private Integer uploadedFiles;
    private Integer passedFiles;    // 新增：检测通过数
    private Integer failedFiles;     // 新增：检测失败数
    private List<FailedFileInfo> failedFileList;  // 新增：失败文件详情
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FailedFileInfo {
    private String fileId;
    private String fileName;
    private String reason;
}
```

### 3. 新增检测统计 API

**位置**: `nexusarchive-java/src/main/java/com/nexusarchive/controller/PoolController.java`

```java
/**
 * 获取检测失败原因统计
 */
@GetMapping("/fail-reasons")
@PreAuthorize("hasAnyAuthority('archive:view','nav:all')")
public Result<Map<String, Long>> getFailureReasons() {
    // 统计各种失败原因的数量
    // 返回格式: { "真实性检测失败": 5, "完整性检测失败": 2, ... }
}
```

---

## 前端改造

### 1. 批量上传页面 (BatchUploadView.tsx)

**改造点**：

1. **完成上传按钮交互**
```tsx
const completeBatchMutation = useMutation({
  mutationFn: () => batchUploadApi.completeBatch(batchInfo!.batchId),
  onSuccess: (data) => {
    setStep('complete');
    setCheckResult({
      passed: data.passedFiles || 0,
      failed: data.failedFiles || 0,
      failedList: data.failedFileList || []
    });

    if (data.failedFiles > 0) {
      message.warning(`批次完成：${data.passedFiles} 个文件检测通过，${data.failedFiles} 个文件检测失败`);
    } else {
      message.success(`批次完成：所有 ${data.passedFiles} 个文件检测通过`);
    }
  },
  onError: (error) => {
    message.error('批次完成失败：' + error.message);
  }
});
```

2. **加载状态文本**
```tsx
<Button
  type="primary"
  onClick={handleCompleteBatch}
  disabled={stats.uploaded === 0}
  loading={completeBatchMutation.isPending}
>
  {completeBatchMutation.isPending ? '正在执行四性检测...' : '完成上传'}
</Button>
```

3. **完成页面显示检测摘要**
```tsx
{step === 'complete' && (
  <div className="text-center">
    <Result
      status={checkResult.failed > 0 ? 'warning' : 'success'}
      title="批次上传完成"
      subTitle={
        checkResult.failed > 0
          ? `${checkResult.passed} 个文件检测通过，${checkResult.failed} 个文件检测失败`
          : `所有 ${checkResult.passed} 个文件检测通过`
      }
    />
    {checkResult.failed > 0 && (
      <div className="mt-4">
        <h3>检测失败文件：</h3>
        <List
          dataSource={checkResult.failedList}
          renderItem={(item) => (
            <List.Item>
              <span>{item.fileName}</span>
              <Tag color="error">{item.reason}</Tag>
            </List.Item>
          )}
        />
      </div>
    )}
  </div>
)}
```

### 2. 预归档库仪表板 (PoolDashboard.tsx)

**改造点**：

1. **按钮显示逻辑**
```tsx
if (showActions && count > 0) {
  if (status === SimplifiedPreArchiveStatus.READY_TO_ARCHIVE) {
    currentShowAction = true;
    currentActionLabel = `批量归档 (${count})`;
  } else if (status === SimplifiedPreArchiveStatus.NEEDS_ACTION) {
    currentShowAction = true;
    currentActionLabel = `查看详情 (${count})`;
  } else if (status === SimplifiedPreArchiveStatus.PENDING_CHECK) {
    currentShowAction = true;
    currentActionLabel = `开始检测 (${count})`;
  }
}
// READY_TO_MATCH, SUBMITTED, COMPLETED 不显示按钮
```

2. **顶部提示条**
```tsx
<div className="pool-dashboard">
  {/* 状态提示条 */}
  {(stats.NEEDS_ACTION > 0 || stats.READY_TO_ARCHIVE > 0) && (
    <Alert
      type={stats.NEEDS_ACTION > 0 ? 'warning' : 'info'}
      message={
        stats.NEEDS_ACTION > 0
          ? `有 ${stats.NEEDS_ACTION} 个文件检测失败，请处理后重新提交`
          : `有 ${stats.READY_TO_ARCHIVE} 个文件可以提交归档`
      }
      showIcon
      className="mb-4"
    />
  )}

  {/* 默认提示 */}
  {stats.NEEDS_ACTION === 0 && stats.READY_TO_ARCHIVE === 0 && (
    <Alert
      type="info"
      message="文件需通过四性检测后才能归档"
      showIcon
      className="mb-4"
    />
  )}

  {/* 状态卡片 */}
  <div className="pool-dashboard__cards">
    ...
  </div>
</div>
```

3. **NEEDS_ACTION 卡片增强**
```tsx
// 在 DashboardCard 中添加失败原因标签
{status === SimplifiedPreArchiveStatus.NEEDS_ACTION && failureReason && (
  <Tag color="error" className="mt-2">{failureReason}</Tag>
)}
```

### 3. API 客户端更新 (pool.ts)

**新增**：获取失败原因统计
```tsx
getFailureReasons: async (): Promise<Record<string, number>> => {
  const response = await client.get<ApiResponse<Record<string, number>>>('/pool/fail-reasons');
  if (response.data.code === 200) {
    return response.data.data;
  }
  return {};
}
```

---

## 测试计划

### 单元测试

1. **CollectionBatchServiceImplTest**
   - 测试批次完成后正确触发检测
   - 测试检测通过/失败的状态更新
   - 测试返回值的完整性

2. **PreArchiveCheckServiceTest**
   - 测试四性检测各种失败场景

### 集成测试

1. 完整的上传-检测-归档流程
2. 检测失败后的重新检测流程
3. 批量上传大量文件的性能测试

### E2E 测试

1. 批量上传文件 → 点击完成上传 → 验证检测结果
2. 检测失败 → 查看失败原因 → 重新检测
3. 检测通过 → 提交归档审批 → 验证审批记录

---

## 实施检查清单

### 后端
- [ ] 扩展 `BatchCompleteResult` DTO
- [ ] 改造 `CollectionBatchServiceImpl.completeBatch()`
- [ ] 添加 `executeBatchCheck()` 方法
- [ ] 添加 `getFailureReason()` 方法
- [ ] 新增 `/pool/fail-reasons` API
- [ ] 编写单元测试

### 前端
- [ ] 更新 `BatchUploadView.tsx` 完成页面
- [ ] 更新 `PoolDashboard.tsx` 按钮逻辑
- [ ] 添加状态提示条
- [ ] 更新 `pool.ts` API 客户端
- [ ] 编写组件测试

### 文档
- [ ] 更新用户手册（批量上传流程）
- [ ] 更新 API 文档

---

## 变更影响

### 兼容性

- **向后兼容**：是的，API 只是扩展返回值
- **数据库变更**：无
- **配置变更**：无

### 性能

- **上传时间增加**：批次完成时需要等待检测完成
- **建议**：对于大批次（>100文件），考虑改为异步处理（未来优化）

---

## 后续优化方向

1. **异步检测**：大批次使用异步检测 + WebSocket 进度推送
2. **检测模板化**：支持自定义检测规则
3. **批量重试**：一键重新检测所有失败文件
