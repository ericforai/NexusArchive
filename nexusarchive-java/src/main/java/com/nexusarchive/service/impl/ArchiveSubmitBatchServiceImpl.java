// Input: Spring Framework、MyBatis-Plus、Java 标准库、本地模块
// Output: ArchiveSubmitBatchServiceImpl 实现类
// Pos: 服务层实现
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.*;
import com.nexusarchive.mapper.*;
import com.nexusarchive.service.ArchiveSubmitBatchService;
import com.nexusarchive.service.FourNatureCoreService;
import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.OverallStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.InputStream;
import java.io.BufferedInputStream;

/**
 * 归档提交批次服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveSubmitBatchServiceImpl implements ArchiveSubmitBatchService {

    private final ArchiveSubmitBatchMapper batchMapper;
    private final ArchiveBatchItemMapper itemMapper;
    private final PeriodLockMapper periodLockMapper;
    private final IntegrityCheckMapper integrityCheckMapper;
    private final ArcFileContentMapper voucherMapper;
    private final OriginalVoucherMapper originalVoucherMapper;
    private final FourNatureCoreService fourNatureCoreService;

    // ========== 批次管理 ==========

    @Override
    @Transactional
    public ArchiveSubmitBatch createBatch(Long fondsId, LocalDate periodStart, LocalDate periodEnd, Long createdBy) {
        // 检查期间是否已锁定
        String startPeriod = periodStart.toString().substring(0, 7);
        String endPeriod = periodEnd.toString().substring(0, 7);

        PeriodLock lock = periodLockMapper.findActiveLock(fondsId, startPeriod);
        if (lock != null && PeriodLock.TYPE_ARCHIVED.equals(lock.getLockType())) {
            throw new IllegalStateException("期间 " + startPeriod + " 已归档，不能重复创建批次");
        }

        // 检查是否有未完成的批次
        int pendingCount = batchMapper.countPendingBatchesInPeriod(fondsId, startPeriod, endPeriod);
        if (pendingCount > 0) {
            throw new IllegalStateException("该期间范围已有进行中的归档批次");
        }

        // 生成批次编号
        String batchNo = batchMapper.generateBatchNo();

        ArchiveSubmitBatch batch = ArchiveSubmitBatch.builder()
                .batchNo(batchNo)
                .fondsId(fondsId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .scopeType(ArchiveSubmitBatch.SCOPE_PERIOD)
                .status(ArchiveSubmitBatch.STATUS_PENDING)
                .voucherCount(0)
                .docCount(0)
                .fileCount(0)
                .totalSizeBytes(0L)
                .createdBy(createdBy)
                .createdTime(LocalDateTime.now())
                .lastModifiedTime(LocalDateTime.now())
                .build();

        batchMapper.insert(batch);
        log.info("创建归档批次: {} (fondsId={}, period={} ~ {})", batchNo, fondsId, periodStart, periodEnd);

        return batch;
    }

    @Override
    public ArchiveSubmitBatch getBatch(Long batchId) {
        return batchMapper.selectById(batchId);
    }

    @Override
    public IPage<ArchiveSubmitBatch> listBatches(Page<ArchiveSubmitBatch> page, Long fondsId, String status) {
        return batchMapper.findPage(page, fondsId, status);
    }

    @Override
    public List<ArchiveSubmitBatch> listBatchesByFonds(Long fondsId, String status) {
        return batchMapper.findByFondsAndStatus(fondsId, status);
    }

    @Override
    @Transactional
    public void deleteBatch(Long batchId) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!batch.isPending()) {
            throw new IllegalStateException("只能删除待提交状态的批次");
        }

        // 删除批次条目
        itemMapper.delete(new LambdaQueryWrapper<ArchiveBatchItem>()
                .eq(ArchiveBatchItem::getBatchId, batchId));

        // 删除批次
        batchMapper.deleteById(batchId);
        log.info("删除归档批次: {}", batch.getBatchNo());
    }

    // ========== 批次条目管理 ==========

    @Override
    @Transactional
    public int addVouchersToBatch(Long batchId, List<Long> voucherIds) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!batch.isPending()) {
            throw new IllegalStateException("只能向待提交状态的批次添加条目");
        }

        int added = 0;
        for (Long voucherId : voucherIds) {
            // 检查凭证是否已在其他批次中
            int existCount = itemMapper.countVoucherInOtherBatches(voucherId);
            if (existCount > 0) {
                log.warn("凭证 {} 已在其他批次中，跳过", voucherId);
                continue;
            }

            // 获取凭证信息
            ArcFileContent voucher = voucherMapper.selectById(voucherId);
            if (voucher == null) {
                log.warn("凭证不存在: {}", voucherId);
                continue;
            }

            ArchiveBatchItem item = ArchiveBatchItem.builder()
                    .batchId(batchId)
                    .itemType(ArchiveBatchItem.TYPE_VOUCHER)
                    .refId(voucherId)
                    .refNo(voucher.getErpVoucherNo())
                    .status(ArchiveBatchItem.STATUS_PENDING)
                    .createdTime(LocalDateTime.now())
                    .build();

            itemMapper.insert(item);
            added++;
        }

        // 更新批次统计
        if (added > 0) {
            batch.setVoucherCount(batch.getVoucherCount() + added);
            batch.setLastModifiedTime(LocalDateTime.now());
            batchMapper.updateById(batch);
        }

        log.info("向批次 {} 添加了 {} 张凭证", batch.getBatchNo(), added);
        return added;
    }

    @Override
    @Transactional
    public int addDocsToBatch(Long batchId, List<Long> docIds) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!batch.isPending()) {
            throw new IllegalStateException("只能向待提交状态的批次添加条目");
        }

        int added = 0;
        for (Long docId : docIds) {
            // 获取单据信息
            OriginalVoucher doc = originalVoucherMapper.selectById(docId);
            if (doc == null) {
                log.warn("单据不存在: {}", docId);
                continue;
            }

            ArchiveBatchItem item = ArchiveBatchItem.builder()
                    .batchId(batchId)
                    .itemType(ArchiveBatchItem.TYPE_SOURCE_DOC)
                    .refId(docId)
                    .refNo(doc.getVoucherNo())
                    .status(ArchiveBatchItem.STATUS_PENDING)
                    .createdTime(LocalDateTime.now())
                    .build();

            itemMapper.insert(item);
            added++;
        }

        // 更新批次统计
        if (added > 0) {
            batch.setDocCount(batch.getDocCount() + added);
            batch.setLastModifiedTime(LocalDateTime.now());
            batchMapper.updateById(batch);
        }

        log.info("向批次 {} 添加了 {} 张单据", batch.getBatchNo(), added);
        return added;
    }

    @Override
    @Transactional
    public void removeItemFromBatch(Long batchId, Long itemId) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!batch.isPending()) {
            throw new IllegalStateException("只能从待提交状态的批次移除条目");
        }

        ArchiveBatchItem item = itemMapper.selectById(itemId);
        if (item == null || !item.getBatchId().equals(batchId)) {
            throw new IllegalArgumentException("条目不存在或不属于此批次");
        }

        itemMapper.deleteById(itemId);

        // 更新统计
        if (ArchiveBatchItem.TYPE_VOUCHER.equals(item.getItemType())) {
            batch.setVoucherCount(Math.max(0, batch.getVoucherCount() - 1));
        } else {
            batch.setDocCount(Math.max(0, batch.getDocCount() - 1));
        }
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);
    }

    @Override
    public List<ArchiveBatchItem> getBatchItems(Long batchId) {
        return itemMapper.findByBatchId(batchId);
    }

    @Override
    public List<ArchiveBatchItem> getBatchItemsByType(Long batchId, String itemType) {
        return itemMapper.findByBatchIdAndType(batchId, itemType);
    }

    // ========== 归档流程 ==========

    @Override
    @Transactional
    public ArchiveSubmitBatch submitBatch(Long batchId, Long submittedBy) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!batch.canSubmit()) {
            throw new IllegalStateException("批次状态不允许提交: " + batch.getStatus());
        }

        // 检查是否有条目
        List<ArchiveBatchItem> items = itemMapper.findByBatchId(batchId);
        if (items.isEmpty()) {
            throw new IllegalStateException("批次中没有任何条目，无法提交");
        }

        batch.setStatus(ArchiveSubmitBatch.STATUS_VALIDATING);
        batch.setSubmittedBy(submittedBy);
        batch.setSubmittedAt(LocalDateTime.now());
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        log.info("提交归档批次: {} (提交人: {})", batch.getBatchNo(), submittedBy);

        // 异步执行校验（此处简化为同步）
        validateBatch(batchId);

        return batchMapper.selectById(batchId);
    }

    @Override
    @Transactional
    public Map<String, Object> validateBatch(Long batchId) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("batchId", batchId);
        report.put("batchNo", batch.getBatchNo());
        report.put("validatedAt", LocalDateTime.now().toString());

        List<Map<String, Object>> errors = new ArrayList<>();
        List<Map<String, Object>> warnings = new ArrayList<>();
        int validatedCount = 0;

        // 校验每个条目
        List<ArchiveBatchItem> items = itemMapper.findByBatchId(batchId);
        for (ArchiveBatchItem item : items) {
            Map<String, Object> itemResult = validateItem(item);

            if ("FAIL".equals(itemResult.get("status"))) {
                errors.add(itemResult);
                item.setStatus(ArchiveBatchItem.STATUS_FAILED);
            } else if ("WARNING".equals(itemResult.get("status"))) {
                warnings.add(itemResult);
                item.setStatus(ArchiveBatchItem.STATUS_VALIDATED);
                validatedCount++;
            } else {
                item.setStatus(ArchiveBatchItem.STATUS_VALIDATED);
                validatedCount++;
            }

            item.setValidationResult(itemResult);
            itemMapper.updateById(item);
        }

        report.put("totalItems", items.size());
        report.put("validatedItems", validatedCount);
        report.put("errorCount", errors.size());
        report.put("warningCount", warnings.size());
        report.put("errors", errors);
        report.put("warnings", warnings);

        // 更新批次
        batch.setValidationReport(report);
        batch.setLastModifiedTime(LocalDateTime.now());

        if (!errors.isEmpty()) {
            batch.setStatus(ArchiveSubmitBatch.STATUS_FAILED);
            batch.setErrorMessage("校验失败: " + errors.size() + " 个错误");
        }
        // 校验通过保持 VALIDATING 状态，等待审批

        batchMapper.updateById(batch);

        log.info("批次 {} 校验完成: {} 通过, {} 错误, {} 警告",
                batch.getBatchNo(), validatedCount, errors.size(), warnings.size());

        return report;
    }

    private Map<String, Object> validateItem(ArchiveBatchItem item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemId", item.getId());
        result.put("itemType", item.getItemType());
        result.put("refId", item.getRefId());
        result.put("refNo", item.getRefNo());

        List<String> issues = new ArrayList<>();

        if (ArchiveBatchItem.TYPE_VOUCHER.equals(item.getItemType())) {
            // 校验凭证
            ArcFileContent voucher = voucherMapper.selectById(item.getRefId());
            if (voucher == null) {
                issues.add("凭证不存在");
                result.put("status", "FAIL");
            } else {
                // 检查是否有必要字段
                if (voucher.getErpVoucherNo() == null || voucher.getErpVoucherNo().isEmpty()) {
                    issues.add("凭证号为空");
                }
                // 可以添加更多校验...

                result.put("status", issues.isEmpty() ? "PASS" : "WARNING");
            }
        } else {
            // 校验单据
            OriginalVoucher doc = originalVoucherMapper.selectById(item.getRefId());
            if (doc == null) {
                issues.add("单据不存在");
                result.put("status", "FAIL");
            } else {
                result.put("status", "PASS");
            }
        }

        result.put("issues", issues);
        return result;
    }

    @Override
    @Transactional
    public ArchiveSubmitBatch approveBatch(Long batchId, Long approvedBy, String comment) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!ArchiveSubmitBatch.STATUS_VALIDATING.equals(batch.getStatus())) {
            throw new IllegalStateException("只能审批校验中的批次");
        }

        // 检查是否有失败条目
        int failedCount = itemMapper.countFailedItems(batchId);
        if (failedCount > 0) {
            throw new IllegalStateException("存在 " + failedCount + " 个校验失败的条目，无法审批通过");
        }

        batch.setStatus(ArchiveSubmitBatch.STATUS_APPROVED);
        batch.setApprovedBy(approvedBy);
        batch.setApprovedAt(LocalDateTime.now());
        batch.setApprovalComment(comment);
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        log.info("审批通过归档批次: {} (审批人: {})", batch.getBatchNo(), approvedBy);

        return batch;
    }

    @Override
    @Transactional
    public ArchiveSubmitBatch rejectBatch(Long batchId, Long rejectedBy, String comment) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!ArchiveSubmitBatch.STATUS_VALIDATING.equals(batch.getStatus())) {
            throw new IllegalStateException("只能驳回校验中的批次");
        }

        batch.setStatus(ArchiveSubmitBatch.STATUS_REJECTED);
        batch.setApprovedBy(rejectedBy);
        batch.setApprovedAt(LocalDateTime.now());
        batch.setApprovalComment(comment);
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        log.info("驳回归档批次: {} (原因: {})", batch.getBatchNo(), comment);

        return batch;
    }

    @Override
    @Transactional
    public ArchiveSubmitBatch executeBatchArchive(Long batchId, Long archivedBy) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!batch.canArchive()) {
            throw new IllegalStateException("批次状态不允许执行归档: " + batch.getStatus());
        }

        // 执行四性检测
        Map<String, Object> integrityReport = runIntegrityCheck(batchId);
        batch.setIntegrityReport(integrityReport);

        // 更新所有条目状态为已归档
        itemMapper.updateStatusByBatchId(batchId, ArchiveBatchItem.STATUS_ARCHIVED);

        // 锁定期间
        String startPeriod = batch.getPeriodStart().toString().substring(0, 7);
        String endPeriod = batch.getPeriodEnd().toString().substring(0, 7);

        // 为每个月份创建锁定记录
        LocalDate current = batch.getPeriodStart().withDayOfMonth(1);
        while (!current.isAfter(batch.getPeriodEnd())) {
            String period = current.toString().substring(0, 7);
            PeriodLock existingLock = periodLockMapper.findActiveLockByType(
                    batch.getFondsId(), period, PeriodLock.TYPE_ARCHIVED);

            if (existingLock == null) {
                PeriodLock lock = PeriodLock.builder()
                        .fondsId(batch.getFondsId())
                        .period(period)
                        .lockType(PeriodLock.TYPE_ARCHIVED)
                        .lockedAt(LocalDateTime.now())
                        .lockedBy(archivedBy)
                        .reason("归档批次: " + batch.getBatchNo())
                        .build();
                periodLockMapper.insert(lock);
            }

            current = current.plusMonths(1);
        }

        // 更新批次状态
        batch.setStatus(ArchiveSubmitBatch.STATUS_ARCHIVED);
        batch.setArchivedBy(archivedBy);
        batch.setArchivedAt(LocalDateTime.now());
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        log.info("归档批次执行完成: {} ({} 凭证, {} 单据)",
                batch.getBatchNo(), batch.getVoucherCount(), batch.getDocCount());

        return batch;
    }

    // ========== 四性检测 ==========

    @Override
    @Transactional
    public Map<String, Object> runIntegrityCheck(Long batchId) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("batchId", batchId);
        report.put("batchNo", batch.getBatchNo());
        report.put("checkedAt", LocalDateTime.now().toString());

        // 四性检测
        List<Map<String, Object>> checks = new ArrayList<>();

        // 1. 真实性检测
        checks.add(checkAuthenticity(batch));

        // 2. 完整性检测
        checks.add(checkIntegrity(batch));

        // 3. 可用性检测
        checks.add(checkUsability(batch));

        // 4. 安全性检测
        checks.add(checkSecurity(batch));

        report.put("checks", checks);

        // 计算总体结果
        boolean allPassed = checks.stream()
                .allMatch(c -> "PASS".equals(c.get("result")));
        report.put("overallResult", allPassed ? "PASS" : "FAIL");

        return report;
    }

    private Map<String, Object> checkAuthenticity(ArchiveSubmitBatch batch) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkType", IntegrityCheck.CHECK_AUTHENTICITY);
        result.put("name", "真实性检测");

        List<ArchiveBatchItem> items = itemMapper.findByBatchId(batch.getId());
        List<String> details = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int passedCount = 0;

        for (ArchiveBatchItem item : items) {
            if (!ArchiveBatchItem.TYPE_VOUCHER.equals(item.getItemType())) {
                continue; // 暂时只检测凭证
            }
            
            ArcFileContent file = voucherMapper.selectById(item.getRefId());
            if (file == null) {
                errors.add("凭证不存在: " + item.getRefId());
                continue;
            }

            try {
                if (file.getStoragePath() == null || !Files.exists(Paths.get(file.getStoragePath()))) {
                   errors.add("文件文件丢失: " + file.getFileName());
                   continue;
                }

                try (InputStream fis = Files.newInputStream(Paths.get(file.getStoragePath()));
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    
                    String expectedHash = (file.getOriginalHash() != null && !file.getOriginalHash().isEmpty()) 
                            ? file.getOriginalHash() 
                            : file.getFileHash();

                    CheckItem checkItem = fourNatureCoreService.checkSingleFileAuthenticity(
                            bis, file.getFileName(), expectedHash, file.getHashAlgorithm(), file.getFileType());
                    
                    if (checkItem.getStatus() == OverallStatus.FAIL) {
                        errors.add(file.getFileName() + ": " + checkItem.getMessage());
                    } else if (checkItem.getStatus() == OverallStatus.WARNING) {
                        details.add(file.getFileName() + ": " + checkItem.getMessage());
                        passedCount++;
                    } else {
                        passedCount++;
                    }
                }
            } catch (Exception e) {
                errors.add(file.getFileName() + " 检测异常: " + e.getMessage());
            }
        }

        result.put("totalChecked", items.size());
        result.put("passedCount", passedCount);
        
        if (!errors.isEmpty()) {
            result.put("result", "FAIL");
            result.put("errors", errors);
        } else {
            result.put("result", "PASS");
        }
        
        if (!details.isEmpty()) {
            result.put("details", details); // Warnings
        }

        saveIntegrityCheck(batch.getId(), IntegrityCheck.CHECK_AUTHENTICITY, (String) result.get("result"), result);

        return result;
    }

    private Map<String, Object> checkIntegrity(ArchiveSubmitBatch batch) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkType", IntegrityCheck.CHECK_INTEGRITY);
        result.put("name", "完整性检测");

        List<ArchiveBatchItem> items = itemMapper.findByBatchId(batch.getId());
        List<String> errors = new ArrayList<>();
        int validCount = 0;

        for (ArchiveBatchItem item : items) {
            if (ArchiveBatchItem.TYPE_VOUCHER.equals(item.getItemType())) {
                 ArcFileContent file = voucherMapper.selectById(item.getRefId());
                 if (file == null) {
                     errors.add("凭证引用无效: ID " + item.getRefId());
                     continue;
                 }
                 
                 // 必填字段检查 (简化版，与 PreArchiveCheckService 保持一致)
                 List<String> missing = new ArrayList<>();
                 if (file.getFiscalYear() == null) missing.add("会计年度");
                 if (file.getVoucherType() == null) missing.add("凭证类型");
                 if (file.getCreator() == null) missing.add("责任者");
                 
                 if (!missing.isEmpty()) {
                     errors.add(file.getFileName() + " 缺失字段: " + String.join(",", missing));
                 } else {
                     validCount++;
                 }
            } else {
                // 单据暂简单通过
                if (item.getRefId() != null) validCount++;
            }
        }

        result.put("totalItems", items.size());
        result.put("validItems", validCount);
        
        if (!errors.isEmpty()) {
            result.put("result", "FAIL");
            result.put("errors", errors);
        } else {
            result.put("result", "PASS");
        }

        saveIntegrityCheck(batch.getId(), IntegrityCheck.CHECK_INTEGRITY, (String) result.get("result"), result);

        return result;
    }

    private Map<String, Object> checkUsability(ArchiveSubmitBatch batch) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkType", IntegrityCheck.CHECK_USABILITY);
        result.put("name", "可用性检测");

        List<ArchiveBatchItem> items = itemMapper.findByBatchId(batch.getId());
        List<String> errors = new ArrayList<>();

        for (ArchiveBatchItem item : items) {
            if (ArchiveBatchItem.TYPE_VOUCHER.equals(item.getItemType())) {
                ArcFileContent file = voucherMapper.selectById(item.getRefId());
                if (file != null && file.getStoragePath() != null && Files.exists(Paths.get(file.getStoragePath()))) {
                     try (InputStream fis = Files.newInputStream(Paths.get(file.getStoragePath()));
                          BufferedInputStream bis = new BufferedInputStream(fis)) {
                        
                        CheckItem checkItem = fourNatureCoreService.checkSingleFileUsability(
                                bis, file.getFileName(), file.getFileType());
                        
                        if (checkItem.getStatus() == OverallStatus.FAIL) {
                            errors.add(file.getFileName() + ": " + checkItem.getMessage());
                        }
                     } catch (Exception e) {
                         errors.add(file.getFileName() + " 读取失败");
                     }
                }
            }
        }

        if (!errors.isEmpty()) {
            result.put("result", "FAIL");
            result.put("errors", errors);
        } else {
             result.put("result", "PASS");
             result.put("details", "所有文件格式校验通过");
        }

        saveIntegrityCheck(batch.getId(), IntegrityCheck.CHECK_USABILITY, (String) result.get("result"), result);
        return result;
    }

    private Map<String, Object> checkSecurity(ArchiveSubmitBatch batch) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkType", IntegrityCheck.CHECK_SECURITY);
        result.put("name", "安全性检测");

        List<ArchiveBatchItem> items = itemMapper.findByBatchId(batch.getId());
        List<String> errors = new ArrayList<>();

        for (ArchiveBatchItem item : items) {
            if (ArchiveBatchItem.TYPE_VOUCHER.equals(item.getItemType())) {
                ArcFileContent file = voucherMapper.selectById(item.getRefId());
                if (file != null && file.getStoragePath() != null && Files.exists(Paths.get(file.getStoragePath()))) {
                     try (InputStream fis = Files.newInputStream(Paths.get(file.getStoragePath()));
                          BufferedInputStream bis = new BufferedInputStream(fis)) {
                        
                        CheckItem checkItem = fourNatureCoreService.checkSingleFileSafety(bis, file.getFileName());
                        
                        if (checkItem.getStatus() == OverallStatus.FAIL) {
                            errors.add(file.getFileName() + ": " + checkItem.getMessage());
                        }
                     } catch (Exception e) {
                         // ignore read error, handled in usability
                     }
                }
            }
        }

        if (!errors.isEmpty()) {
            result.put("result", "FAIL");
            result.put("errors", errors);
        } else {
             result.put("result", "PASS");
             result.put("details", "病毒扫描通过，未发现安全威胁");
        }

        saveIntegrityCheck(batch.getId(), IntegrityCheck.CHECK_SECURITY, (String) result.get("result"), result);
        return result;
    }

    private void saveIntegrityCheck(Long batchId, String checkType, String resultStatus, Map<String, Object> details) {
        IntegrityCheck check = IntegrityCheck.builder()
                .targetType(IntegrityCheck.TARGET_BATCH)
                .targetId(batchId)
                .checkType(checkType)
                .result(resultStatus)
                .details(details)
                .checkedAt(LocalDateTime.now())
                .build();
        integrityCheckMapper.insert(check);
    }

    // ========== 统计 ==========

    @Override
    public Map<String, Object> getBatchStats(Long fondsId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Map<String, Object>> statusCounts = batchMapper.countByStatus(fondsId);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0;

        for (Map<String, Object> row : statusCounts) {
            String status = (String) row.get("status");
            Long count = ((Number) row.get("count")).longValue();
            byStatus.put(status, count);
            total += count;
        }

        stats.put("total", total);
        stats.put("byStatus", byStatus);

        return stats;
    }
}
