// Input: Jackson、SLF4J、Spring Framework、Java 标准库、本地模块
// Output: LocalAuditBuffer 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusarchive.entity.SysAuditLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * 审计日志本地缓冲服务
 * 
 * 当数据库写入失败时，将审计日志持久化到本地文件系统，
 * 确保审计记录不会因数据库故障而丢失。
 * 
 * 合规要求：
 * - DA/T 94-2022: 审计日志必须具备"不可抵赖性"
 * - GB/T 39784-2021: 审计日志必须防篡改
 * 
 * @author Agent - 生产上线阻断项修复
 */
@Slf4j
@Service
public class LocalAuditBuffer {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    
    private final ObjectMapper objectMapper;
    private Path bufferDirectory;

    @Value("${audit.buffer.path:logs/audit-buffer}")
    private String bufferPath;

    public LocalAuditBuffer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() {
        this.bufferDirectory = Paths.get(bufferPath).toAbsolutePath();
        try {
            Files.createDirectories(bufferDirectory);
            log.info("审计日志本地缓冲目录已初始化: {}", bufferDirectory);
        } catch (IOException e) {
            log.error("无法创建审计日志缓冲目录: {}", bufferDirectory, e);
        }
    }

    /**
     * 持久化失败的审计日志到本地文件
     * 
     * 文件命名格式: {timestamp}_{logId}.json
     * 
     * @param auditLog 需要持久化的审计日志
     */
    public void persist(SysAuditLog auditLog) {
        if (auditLog == null) {
            log.warn("尝试持久化空的审计日志，已跳过");
            return;
        }

        String timestamp = LocalDateTime.now().format(FILE_TIME_FORMATTER);
        String logId = auditLog.getId() != null ? auditLog.getId() : UUID.randomUUID().toString();
        String filename = String.format("%s_%s.json", timestamp, logId);
        Path filePath = bufferDirectory.resolve(filename);

        try {
            String json = objectMapper.writeValueAsString(auditLog);
            Files.writeString(filePath, json);
            log.warn("[AUDIT_BUFFER] 审计日志已写入本地缓冲: {} (操作: {}, 用户: {})", 
                    filename, auditLog.getAction(), auditLog.getUserId());
        } catch (IOException e) {
            // 本地写入也失败，这是最后一道防线，必须尝试其他方式
            log.error("[CRITICAL] 审计日志本地缓冲写入失败! 日志可能丢失: {}", auditLog, e);
            // 尝试写入标准错误输出作为最后保障
            System.err.println("[AUDIT_CRITICAL] " + auditLog);
        }
    }

    /**
     * 读取所有待重放的审计日志
     * 
     * @return 待重放的审计日志列表（按文件名排序，保证时间顺序）
     */
    public List<PendingAuditLog> readPending() {
        List<PendingAuditLog> pendingLogs = new ArrayList<>();

        if (!Files.exists(bufferDirectory)) {
            return pendingLogs;
        }

        try (Stream<Path> files = Files.list(bufferDirectory)) {
            files.filter(path -> path.toString().endsWith(".json"))
                 .sorted() // 按文件名排序（时间戳在前）
                 .forEach(path -> {
                     try {
                         String json = Files.readString(path);
                         SysAuditLog auditLog = objectMapper.readValue(json, SysAuditLog.class);
                         pendingLogs.add(new PendingAuditLog(path.getFileName().toString(), auditLog));
                     } catch (IOException e) {
                         log.error("读取缓冲文件失败: {}", path, e);
                     }
                 });
        } catch (IOException e) {
            log.error("遍历缓冲目录失败: {}", bufferDirectory, e);
        }

        log.info("发现 {} 条待重放的审计日志", pendingLogs.size());
        return pendingLogs;
    }

    /**
     * 重放成功后删除缓冲文件
     * 
     * @param filename 要删除的文件名
     */
    public void markReplayed(String filename) {
        if (filename == null || filename.isEmpty()) {
            return;
        }

        Path filePath = bufferDirectory.resolve(filename);
        try {
            if (Files.deleteIfExists(filePath)) {
                log.info("审计日志已重放成功，删除缓冲文件: {}", filename);
            }
        } catch (IOException e) {
            log.error("删除缓冲文件失败: {}", filename, e);
        }
    }

    /**
     * 获取待重放日志数量
     */
    public int getPendingCount() {
        if (!Files.exists(bufferDirectory)) {
            return 0;
        }
        
        try (Stream<Path> files = Files.list(bufferDirectory)) {
            return (int) files.filter(path -> path.toString().endsWith(".json")).count();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 待重放审计日志记录
     */
    public record PendingAuditLog(String filename, SysAuditLog auditLog) {}
}
