// Input: Spring Framework、Lombok、Java 标准库、本地模块、签名日志持久化
// Output: AsyncFourNatureCheckServiceImpl 类（异步四性检测 + 验签结果留痕）
// Pos: 业务服务实现层 - 异步四性检测服务

package com.nexusarchive.service.compliance.impl;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.compliance.AsyncCheckTaskStatus;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcSignatureLog;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcSignatureLogMapper;
import com.nexusarchive.service.ArchiveFileContentService;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.FourNatureCheckService;
import com.nexusarchive.service.FourNatureCoreService;
import com.nexusarchive.service.compliance.AsyncCheckTaskManager;
import com.nexusarchive.service.compliance.AsyncFourNatureCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步四性检测服务实现
 * <p>
 * 将耗时的四性检测操作异步化，通过并行执行真实性、完整性、可用性、安全性检测
 * 来提升 API 响应速度
 * </p>
 *
 * @author System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFourNatureCheckServiceImpl implements AsyncFourNatureCheckService {

    private final AsyncCheckTaskManager taskManager;
    private final ArchiveService archiveService;
    private final ArchiveFileContentService archiveFileContentService;
    private final FourNatureCoreService fourNatureCoreService;
    private final FourNatureCheckService fourNatureCheckService;
    private final ArcFileContentMapper arcFileContentMapper;
    private final ArcSignatureLogMapper arcSignatureLogMapper;

    /**
     * 线程本地缓存，用于存储检测过程中的文件内容
     * 避免多次读取同一文件
     */
    private final ThreadLocal<Map<String, byte[]>> fileContentCache = ThreadLocal.withInitial(ConcurrentHashMap::new);

    @Override
    public CompletableFuture<String> submitCheckTask(String archiveId, String archiveCode) {
        String taskId = UUID.randomUUID().toString();

        // 创建任务状态
        taskManager.createTask(taskId, archiveId, archiveCode);

        // 异步执行检测
        return performParallelCheck(taskId, archiveId)
                .thenApply(result -> {
                    // 结果已由 performParallelCheck 设置到 taskManager
                    return taskId;
                })
                .exceptionally(ex -> {
                    taskManager.markTaskFailed(taskId, ex.getMessage());
                    log.error("四性检测任务执行失败: taskId={}, archiveId={}", taskId, archiveId, ex);
                    throw new BusinessException("四性检测失败: " + ex.getMessage());
                });
    }

    @Override
    @Async("fourNatureCheckExecutor")
    public CompletableFuture<FourNatureReport> performParallelCheck(String taskId, String archiveId) {
        log.info("开始异步四性检测: taskId={}, archiveId={}", taskId, archiveId);

        // 标记任务开始
        taskManager.markTaskRunning(taskId);

        try {
            // 获取档案信息
            Archive archive = archiveService.getArchiveById(archiveId);
            if (archive == null) {
                throw new BusinessException("档案不存在: " + archiveId);
            }

            // 获取关联文件
            List<ArcFileContent> files = archiveFileContentService.getFilesByItemId(archiveId, null);

            // 更新进度
            taskManager.updateTaskPhase(taskId, "准备检测数据", 20);

            // 并行执行四性检测
            CompletableFuture<CheckItem> authenticityFuture = checkAuthenticityAsync(taskId, archive, files);
            CompletableFuture<CheckItem> integrityFuture = checkIntegrityAsync(taskId, archive, files);
            CompletableFuture<CheckItem> usabilityFuture = checkUsabilityAsync(taskId, archive, files);
            CompletableFuture<CheckItem> safetyFuture = checkSafetyAsync(taskId, archive, files);

            // 等待所有检测完成
            return CompletableFuture.allOf(authenticityFuture, integrityFuture, usabilityFuture, safetyFuture)
                    .thenApply(v -> {
                        try {
                            CheckItem authenticity = authenticityFuture.join();
                            CheckItem integrity = integrityFuture.join();
                            CheckItem usability = usabilityFuture.join();
                            CheckItem safety = safetyFuture.join();

                            // 更新进度
                            taskManager.updateTaskPhase(taskId, "汇总检测结果", 90);

                            // 构建最终报告
                            FourNatureReport report = buildReport(archive, authenticity, integrity, usability, safety);

                            // 标记任务完成
                            taskManager.markTaskCompleted(taskId, report);

                            log.info("四性检测完成: taskId={}, archiveId={}, status={}",
                                    taskId, archiveId, report.getStatus());

                            return report;
                        } finally {
                            // 清理缓存
                            fileContentCache.remove();
                        }
                    });

        } catch (Exception e) {
            taskManager.markTaskFailed(taskId, e.getMessage());
            fileContentCache.remove();
            throw new BusinessException("四性检测失败: " + e.getMessage(), e);
        }
    }

    /**
     * 真实性检测（异步）
     * 包括：哈希校验、数字签名验证
     */
    private CompletableFuture<CheckItem> checkAuthenticityAsync(String taskId, Archive archive, List<ArcFileContent> files) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("开始真实性检测: taskId={}, archiveId={}", taskId, archive.getId());

            taskManager.updateTaskPhase(taskId, "真实性检测（哈希校验、签章验证）", 30);

            CheckItem combinedItem = CheckItem.pass("真实性检测", "所有文件验证通过");
            List<String> details = new ArrayList<>();

            if (files.isEmpty()) {
                combinedItem.addError("无关联文件");
                return combinedItem;
            }

            for (ArcFileContent file : files) {
                try {
                    Path path = Path.of(file.getStoragePath());
                    if (!Files.exists(path)) {
                        combinedItem.addError("文件不存在: " + file.getFileName());
                        continue;
                    }

                    // 读取文件内容（使用缓存避免重复读取）
                    byte[] content = getFileContent(file);
                    if (content == null) {
                        combinedItem.addError("无法读取文件: " + file.getFileName());
                        continue;
                    }

                    try (InputStream is = new ByteArrayInputStream(content)) {
                        // 调用核心服务进行真实性检测
                        CheckItem singleResult = fourNatureCoreService.checkSingleFileAuthenticity(
                                is,
                                file.getFileName(),
                                file.getOriginalHash(),
                                file.getHashAlgorithm(),
                                file.getFileType()
                        );

                        persistSignatureVerificationLog(archive.getId(), file, singleResult);
                        mergeResult(combinedItem, singleResult, details, file.getFileName());
                    }

                } catch (Exception e) {
                    log.error("真实性检测异常: file={}, error={}", file.getFileName(), e.getMessage());
                    combinedItem.addError("检测异常: " + file.getFileName() + " - " + e.getMessage());
                }
            }

            if (!details.isEmpty()) {
                combinedItem.setMessage(String.join("; ", details));
            }

            log.debug("真实性检测完成: taskId={}, status={}", taskId, combinedItem.getStatus());
            return combinedItem;
        });
    }

    /**
     * 完整性检测（异步）
     * 包括：元数据完整性、结构完整性、金额一致性
     */
    private CompletableFuture<CheckItem> checkIntegrityAsync(String taskId, Archive archive, List<ArcFileContent> files) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("开始完整性检测: taskId={}, archiveId={}", taskId, archive.getId());

            taskManager.updateTaskPhase(taskId, "完整性检测（元数据、结构）", 50);

            CheckItem item = CheckItem.pass("完整性检测", "元数据和结构完整");

            // 检查必填字段
            if (archive.getUniqueBizId() == null || archive.getUniqueBizId().isEmpty()) {
                item.addError("缺少唯一业务标识");
            }
            if (archive.getAmount() == null) {
                item.addError("缺少金额信息");
            }
            if (archive.getDocDate() == null) {
                item.addError("缺少业务日期");
            }

            // 检查文件数量一致性
            int expectedFileCount = files.size();
            // 可以根据 archive 中的 attachmentCount 字段校验
            // 这里简化处理
            if (expectedFileCount == 0) {
                item.addError("无关联文件");
            }

            // 检查元数据完整性
            if (archive.getStandardMetadata() == null || archive.getStandardMetadata().isEmpty()) {
                item.addError("缺少标准元数据");
            }

            log.debug("完整性检测完成: taskId={}, status={}", taskId, item.getStatus());
            return item;
        });
    }

    /**
     * 可用性检测（异步）
     * 包括：文件格式验证、可读性检查
     */
    private CompletableFuture<CheckItem> checkUsabilityAsync(String taskId, Archive archive, List<ArcFileContent> files) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("开始可用性检测: taskId={}, archiveId={}", taskId, archive.getId());

            taskManager.updateTaskPhase(taskId, "可用性检测（格式、可读性）", 70);

            CheckItem combinedItem = CheckItem.pass("可用性检测", "所有文件可用");
            List<String> details = new ArrayList<>();

            if (files.isEmpty()) {
                combinedItem.addError("无关联文件");
                return combinedItem;
            }

            for (ArcFileContent file : files) {
                try {
                    Path path = Path.of(file.getStoragePath());
                    if (!Files.exists(path)) {
                        combinedItem.addError("文件不存在: " + file.getFileName());
                        continue;
                    }

                    // 读取文件内容
                    byte[] content = getFileContent(file);
                    if (content == null) {
                        combinedItem.addError("无法读取文件: " + file.getFileName());
                        continue;
                    }

                    try (InputStream is = new ByteArrayInputStream(content)) {
                        CheckItem singleResult = fourNatureCoreService.checkSingleFileUsability(
                                is,
                                file.getFileName(),
                                file.getFileType()
                        );

                        mergeResult(combinedItem, singleResult, details, file.getFileName());
                    }

                } catch (Exception e) {
                    log.error("可用性检测异常: file={}, error={}", file.getFileName(), e.getMessage());
                    combinedItem.addError("检测异常: " + file.getFileName() + " - " + e.getMessage());
                }
            }

            if (!details.isEmpty()) {
                combinedItem.setMessage(String.join("; ", details));
            }

            log.debug("可用性检测完成: taskId={}, status={}", taskId, combinedItem.getStatus());
            return combinedItem;
        });
    }

    /**
     * 安全性检测（异步）
     * 包括：病毒扫描、恶意代码检测
     */
    private CompletableFuture<CheckItem> checkSafetyAsync(String taskId, Archive archive, List<ArcFileContent> files) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("开始安全性检测: taskId={}, archiveId={}", taskId, archive.getId());

            taskManager.updateTaskPhase(taskId, "安全性检测（病毒扫描）", 85);

            CheckItem combinedItem = CheckItem.pass("安全性检测", "未检测到安全威胁");

            if (files.isEmpty()) {
                combinedItem.addError("无关联文件");
                return combinedItem;
            }

            for (ArcFileContent file : files) {
                try {
                    Path path = Path.of(file.getStoragePath());
                    if (!Files.exists(path)) {
                        combinedItem.addError("文件不存在: " + file.getFileName());
                        continue;
                    }

                    // 读取文件内容
                    byte[] content = getFileContent(file);
                    if (content == null) {
                        combinedItem.addError("无法读取文件: " + file.getFileName());
                        continue;
                    }

                    try (InputStream is = new ByteArrayInputStream(content)) {
                        CheckItem singleResult = fourNatureCoreService.checkSingleFileSafety(is, file.getFileName());

                        if (singleResult.getStatus() == OverallStatus.FAIL
                                || singleResult.getStatus() == OverallStatus.WARNING) {
                            combinedItem.setStatus(singleResult.getStatus());
                            if (singleResult.getErrors() != null) {
                                singleResult.getErrors().forEach(combinedItem::addError);
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("安全性检测异常: file={}, error={}", file.getFileName(), e.getMessage());
                    combinedItem.setStatus(OverallStatus.WARNING);
                    combinedItem.addError("安全检测异常: " + file.getFileName() + " - " + e.getMessage());
                }
            }

            log.debug("安全性检测完成: taskId={}, status={}", taskId, combinedItem.getStatus());
            return combinedItem;
        });
    }

    /**
     * 构建最终报告
     */
    private FourNatureReport buildReport(Archive archive,
                                         CheckItem authenticity,
                                         CheckItem integrity,
                                         CheckItem usability,
                                         CheckItem safety) {
        String checkId = UUID.randomUUID().toString();

        // 确定总体状态
        OverallStatus overallStatus = OverallStatus.PASS;
        if (authenticity.getStatus() == OverallStatus.FAIL
                || integrity.getStatus() == OverallStatus.FAIL
                || usability.getStatus() == OverallStatus.FAIL
                || safety.getStatus() == OverallStatus.FAIL) {
            overallStatus = OverallStatus.FAIL;
        } else if (authenticity.getStatus() == OverallStatus.WARNING
                || integrity.getStatus() == OverallStatus.WARNING
                || usability.getStatus() == OverallStatus.WARNING
                || safety.getStatus() == OverallStatus.WARNING) {
            overallStatus = OverallStatus.WARNING;
        }

        return FourNatureReport.builder()
                .checkId(checkId)
                .checkTime(LocalDateTime.now())
                .archivalCode(archive.getArchiveCode())
                .status(overallStatus)
                .authenticity(authenticity)
                .integrity(integrity)
                .usability(usability)
                .safety(safety)
                .build();
    }

    /**
     * 合并检测结果
     */
    private void mergeResult(CheckItem target, CheckItem source, List<String> detailsCollector, String fileName) {
        if (source.getStatus() == OverallStatus.FAIL) {
            target.setStatus(OverallStatus.FAIL);
            target.addError(fileName + ": " + source.getMessage());
        } else if (source.getStatus() == OverallStatus.WARNING) {
            if (target.getStatus() != OverallStatus.FAIL) {
                target.setStatus(OverallStatus.WARNING);
            }
            detailsCollector.add(fileName + ": " + source.getMessage());
        } else {
            if (source.getMessage() != null && !source.getMessage().isEmpty()) {
                detailsCollector.add(fileName + ": " + source.getMessage());
            }
        }
    }

    private void persistSignatureVerificationLog(String archiveId, ArcFileContent file, CheckItem result) {
        if (!supportsSignatureVerification(file.getFileType())) {
            return;
        }

        String verifyMessage = resolveVerifyMessage(result);
        ArcSignatureLog logEntry = new ArcSignatureLog();
        logEntry.setArchiveId(archiveId);
        logEntry.setFileId(file.getId());
        logEntry.setVerifyResult(resolveVerifyResult(result, verifyMessage));
        logEntry.setVerifyTime(LocalDateTime.now());
        logEntry.setVerifyMessage(verifyMessage);

        try {
            arcSignatureLogMapper.insert(logEntry);
        } catch (Exception ex) {
            log.warn("签名验证结果持久化失败: archiveId={}, fileId={}, error={}",
                    archiveId, file.getId(), ex.getMessage());
        }
    }

    private boolean supportsSignatureVerification(String fileType) {
        return "PDF".equalsIgnoreCase(fileType) || "OFD".equalsIgnoreCase(fileType);
    }

    private String resolveVerifyMessage(CheckItem result) {
        if (result == null) {
            return "签名验证结果不可用";
        }
        String message = result.getMessage();
        String errors = null;
        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
            errors = String.join("; ", result.getErrors());
        }
        if (result.getStatus() == OverallStatus.FAIL) {
            if (message != null && !message.isBlank() && errors != null && !errors.isBlank()) {
                return message + "; " + errors;
            }
            if (message != null && !message.isBlank()) {
                return message;
            }
            if (errors != null && !errors.isBlank()) {
                return errors;
            }
        }
        if (message != null && !message.isBlank()) {
            return message;
        }
        if (errors != null && !errors.isBlank()) {
            return errors;
        }
        return "签名验证结果为空";
    }

    private String resolveVerifyResult(CheckItem result, String verifyMessage) {
        if (result == null) {
            return "UNKNOWN";
        }
        if (result.getStatus() == OverallStatus.PASS) {
            return "VALID";
        }
        if (result.getStatus() == OverallStatus.WARNING) {
            return "UNKNOWN";
        }
        if (result.getStatus() == OverallStatus.FAIL) {
            return "INVALID";
        }
        return "UNKNOWN";
    }

    /**
     * 获取文件内容（带缓存）
     */
    private byte[] getFileContent(ArcFileContent file) {
        Map<String, byte[]> cache = fileContentCache.get();
        String cacheKey = file.getId();

        byte[] content = cache.get(cacheKey);
        if (content == null) {
            try {
                Path path = Path.of(file.getStoragePath());
                content = Files.readAllBytes(path);
                cache.put(cacheKey, content);
            } catch (Exception e) {
                log.error("读取文件失败: file={}, error={}", file.getFileName(), e.getMessage());
            }
        }
        return content;
    }

    @Override
    public AsyncCheckTaskStatus getTaskStatus(String taskId) {
        return taskManager.getTaskStatus(taskId);
    }

    @Override
    public FourNatureReport getCheckResult(String taskId) {
        return taskManager.getResult(taskId);
    }

    @Override
    public CompletableFuture<FourNatureReport> getCheckResultAsync(String taskId) {
        AsyncCheckTaskStatus status = taskManager.getTaskStatus(taskId);
        if (status == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (status.getStatus() == AsyncCheckTaskStatus.TaskStatus.COMPLETED) {
            return CompletableFuture.completedFuture(taskManager.getResult(taskId));
        }

        if (status.getStatus() == AsyncCheckTaskStatus.TaskStatus.FAILED) {
            return CompletableFuture.completedFuture(null);
        }

        // 任务仍在执行中，返回未完成的 Future
        return CompletableFuture.supplyAsync(() -> {
            while (true) {
                AsyncCheckTaskStatus currentStatus = taskManager.getTaskStatus(taskId);
                if (currentStatus == null) {
                    return null;
                }
                if (currentStatus.getStatus() == AsyncCheckTaskStatus.TaskStatus.COMPLETED) {
                    return taskManager.getResult(taskId);
                }
                if (currentStatus.getStatus() == AsyncCheckTaskStatus.TaskStatus.FAILED
                        || currentStatus.getStatus() == AsyncCheckTaskStatus.TaskStatus.CANCELLED) {
                    return null;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        });
    }

    @Override
    public boolean cancelTask(String taskId) {
        AsyncCheckTaskStatus status = taskManager.getTaskStatus(taskId);
        if (status == null) {
            return false;
        }

        if (status.getStatus() == AsyncCheckTaskStatus.TaskStatus.PENDING
                || status.getStatus() == AsyncCheckTaskStatus.TaskStatus.RUNNING) {
            status.setStatus(AsyncCheckTaskStatus.TaskStatus.CANCELLED);
            log.info("任务已取消: taskId={}", taskId);
            return true;
        }

        return false;
    }
}
