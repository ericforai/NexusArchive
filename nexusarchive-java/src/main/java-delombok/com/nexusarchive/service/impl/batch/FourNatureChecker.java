// Input: MyBatis-Plus、Lombok、Spring Framework
// Output: FourNatureChecker 类
// Pos: 归档批次服务 - 四性检测层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.batch;

import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArchiveBatchItem;
import com.nexusarchive.entity.ArchiveSubmitBatch;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.IntegrityCheck;
import com.nexusarchive.mapper.ArchiveBatchItemMapper;
import com.nexusarchive.mapper.ArchiveSubmitBatchMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.IntegrityCheckMapper;
import com.nexusarchive.service.FourNatureCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 四性检测器
 * <p>
 * 负责归档批次的四性检测：真实性、完整性、可用性、安全性
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FourNatureChecker {

    private final ArchiveSubmitBatchMapper batchMapper;
    private final ArchiveBatchItemMapper itemMapper;
    private final ArcFileContentMapper voucherMapper;
    private final IntegrityCheckMapper integrityCheckMapper;
    private final FourNatureCoreService fourNatureCoreService;

    /**
     * 验证批次
     */
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

                result.put("status", issues.isEmpty() ? "PASS" : "WARNING");
            }
        } else {
            // 校验单据
            result.put("status", "PASS");
        }

        result.put("issues", issues);
        return result;
    }

    /**
     * 执行四性检测
     */
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
                continue;
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
            result.put("details", details);
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

                // 必填字段检查
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
}
