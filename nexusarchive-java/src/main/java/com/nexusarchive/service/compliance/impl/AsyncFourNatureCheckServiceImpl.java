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
    private final com.nexusarchive.service.helper.FourNatureAsyncHelper helper;

    private final ThreadLocal<Map<String, byte[]>> fileContentCache = ThreadLocal.withInitial(ConcurrentHashMap::new);

    @Override
    public CompletableFuture<String> submitCheckTask(String archiveId, String archiveCode) {
        String taskId = UUID.randomUUID().toString();
        taskManager.createTask(taskId, archiveId, archiveCode);
        return performParallelCheck(taskId, archiveId).thenApply(result -> taskId)
                .exceptionally(ex -> {
                    taskManager.markTaskFailed(taskId, ex.getMessage());
                    throw new BusinessException("四性检测失败: " + ex.getMessage());
                });
    }

    @Override
    @Async("fourNatureCheckExecutor")
    public CompletableFuture<FourNatureReport> performParallelCheck(String taskId, String archiveId) {
        taskManager.markTaskRunning(taskId);
        try {
            Archive archive = archiveService.getArchiveById(archiveId);
            if (archive == null) throw new BusinessException("档案不存在: " + archiveId);
            List<ArcFileContent> files = archiveFileContentService.getFilesByItemId(archiveId, null);
            taskManager.updateTaskPhase(taskId, "正在执行检测", 20);

            CompletableFuture<CheckItem> authF = checkAuthenticityAsync(taskId, archive, files);
            CompletableFuture<CheckItem> intF = checkIntegrityAsync(taskId, archive, files);
            CompletableFuture<CheckItem> usaF = checkUsabilityAsync(taskId, archive, files);
            CompletableFuture<CheckItem> safF = checkSafetyAsync(taskId, archive, files);

            return CompletableFuture.allOf(authF, intF, usaF, safF).thenApply(v -> {
                try {
                    FourNatureReport report = helper.buildReport(archive, authF.join(), intF.join(), usaF.join(), safF.join());
                    taskManager.markTaskCompleted(taskId, report);
                    return report;
                } finally { fileContentCache.remove(); }
            });
        } catch (Exception e) {
            taskManager.markTaskFailed(taskId, e.getMessage());
            fileContentCache.remove();
            throw new BusinessException("四性检测失败: " + e.getMessage(), e);
        }
    }

    private CompletableFuture<CheckItem> checkAuthenticityAsync(String taskId, Archive archive, List<ArcFileContent> files) {
        return CompletableFuture.supplyAsync(() -> {
            CheckItem item = CheckItem.pass("真实性检测", "验证通过");
            List<String> details = new ArrayList<>();
            for (ArcFileContent f : files) {
                try {
                    byte[] content = getFileContent(f);
                    if (content == null) { item.addError("无法读取: " + f.getFileName()); continue; }
                    try (InputStream is = new ByteArrayInputStream(content)) {
                        CheckItem res = fourNatureCoreService.checkSingleFileAuthenticity(is, f.getFileName(), f.getOriginalHash(), f.getHashAlgorithm(), f.getFileType());
                        helper.persistVerificationLog(archive.getId(), f, res);
                        helper.merge(item, res, details, f.getFileName());
                    }
                } catch (Exception e) { item.addError("异常: " + f.getFileName()); }
            }
            if (!details.isEmpty()) item.setMessage(String.join("; ", details));
            return item;
        });
    }

    private CompletableFuture<CheckItem> checkIntegrityAsync(String taskId, Archive archive, List<ArcFileContent> files) {
        return CompletableFuture.supplyAsync(() -> {
            CheckItem item = CheckItem.pass("完整性检测", "完整");
            if (archive.getUniqueBizId() == null || archive.getAmount() == null) item.addError("关键字段缺失");
            if (files.isEmpty()) item.addError("无关联文件");
            return item;
        });
    }

    private CompletableFuture<CheckItem> checkUsabilityAsync(String taskId, Archive archive, List<ArcFileContent> files) {
        return CompletableFuture.supplyAsync(() -> {
            CheckItem item = CheckItem.pass("可用性检测", "可用");
            List<String> details = new ArrayList<>();
            for (ArcFileContent f : files) {
                try {
                    byte[] content = getFileContent(f);
                    if (content == null) { item.addError("无法读取: " + f.getFileName()); continue; }
                    try (InputStream is = new ByteArrayInputStream(content)) {
                        CheckItem res = fourNatureCoreService.checkSingleFileUsability(is, f.getFileName(), f.getFileType());
                        helper.merge(item, res, details, f.getFileName());
                    }
                } catch (Exception e) { item.addError("异常: " + f.getFileName()); }
            }
            if (!details.isEmpty()) item.setMessage(String.join("; ", details));
            return item;
        });
    }

    private CompletableFuture<CheckItem> checkSafetyAsync(String taskId, Archive archive, List<ArcFileContent> files) {
        return CompletableFuture.supplyAsync(() -> {
            CheckItem item = CheckItem.pass("安全性检测", "安全");
            for (ArcFileContent f : files) {
                try {
                    byte[] content = getFileContent(f);
                    if (content == null) continue;
                    try (InputStream is = new ByteArrayInputStream(content)) {
                        CheckItem res = fourNatureCoreService.checkSingleFileSafety(is, f.getFileName());
                        if (res.getStatus() != OverallStatus.PASS) {
                            item.setStatus(res.getStatus());
                            if (res.getErrors() != null) res.getErrors().forEach(item::addError);
                        }
                    }
                } catch (Exception e) { item.setStatus(OverallStatus.WARNING); }
            }
            return item;
        });
    }

    private byte[] getFileContent(ArcFileContent file) {
        Map<String, byte[]> cache = fileContentCache.get();
        byte[] content = cache.get(file.getId());
        if (content == null) {
            try { content = Files.readAllBytes(Path.of(file.getStoragePath())); cache.put(file.getId(), content); }
            catch (Exception e) { log.error("Read failed: {}", file.getFileName()); }
        }
        return content;
    }

    @Override public AsyncCheckTaskStatus getTaskStatus(String tid) { return taskManager.getTaskStatus(tid); }
    @Override public FourNatureReport getCheckResult(String tid) { return taskManager.getResult(tid); }

    @Override
    public CompletableFuture<FourNatureReport> getCheckResultAsync(String tid) {
        AsyncCheckTaskStatus s = taskManager.getTaskStatus(tid);
        if (s == null) return CompletableFuture.completedFuture(null);
        if (s.getStatus() == AsyncCheckTaskStatus.TaskStatus.COMPLETED) return CompletableFuture.completedFuture(taskManager.getResult(tid));
        if (s.getStatus() == AsyncCheckTaskStatus.TaskStatus.FAILED) return CompletableFuture.completedFuture(null);
        return CompletableFuture.supplyAsync(() -> {
            while (true) {
                AsyncCheckTaskStatus cur = taskManager.getTaskStatus(tid);
                if (cur == null || cur.getStatus() == AsyncCheckTaskStatus.TaskStatus.FAILED || cur.getStatus() == AsyncCheckTaskStatus.TaskStatus.CANCELLED) return null;
                if (cur.getStatus() == AsyncCheckTaskStatus.TaskStatus.COMPLETED) return taskManager.getResult(tid);
                try { Thread.sleep(100); } catch (InterruptedException e) { return null; }
            }
        });
    }

    @Override
    public boolean cancelTask(String tid) {
        AsyncCheckTaskStatus s = taskManager.getTaskStatus(tid);
        if (s != null && (s.getStatus() == AsyncCheckTaskStatus.TaskStatus.PENDING || s.getStatus() == AsyncCheckTaskStatus.TaskStatus.RUNNING)) {
            s.setStatus(AsyncCheckTaskStatus.TaskStatus.CANCELLED);
            return true;
        }
        return false;
    }
}
