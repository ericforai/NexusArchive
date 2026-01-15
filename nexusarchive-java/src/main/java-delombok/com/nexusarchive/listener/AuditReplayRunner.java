// Input: SLF4J、Spring Framework、Java 标准库、本地模块
// Output: AuditReplayRunner 类
// Pos: 监听器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.listener;

import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.LocalAuditBuffer;
import com.nexusarchive.service.LocalAuditBuffer.PendingAuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 审计日志重放启动器
 * 
 * 应用启动时自动检查本地缓冲目录，将未成功写入数据库的审计日志重放。
 * 这是 P0 阻断项修复的关键组件，确保审计日志的持久化保障。
 * 
 * 执行顺序：在 Flyway 迁移之后执行（Order 100）
 * 
 * 合规要求：
 * - DA/T 94-2022: 审计日志必须具备"不可抵赖性"
 * 
 * @author Agent - 生产上线阻断项修复
 */
@Slf4j
@Component
@Order(100) // 在数据库初始化之后执行
@RequiredArgsConstructor
public class AuditReplayRunner implements ApplicationRunner {

    private final LocalAuditBuffer localAuditBuffer;
    private final AuditLogService auditLogService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 审计日志重放检查开始 ==========");
        
        int pendingCount = localAuditBuffer.getPendingCount();
        if (pendingCount == 0) {
            log.info("本地缓冲区为空，无需重放审计日志");
            return;
        }

        log.warn("[IMPORTANT] 发现 {} 条待重放的审计日志，开始重放...", pendingCount);

        List<PendingAuditLog> pendingLogs = localAuditBuffer.readPending();
        int successCount = 0;
        int failCount = 0;

        for (PendingAuditLog pending : pendingLogs) {
            try {
                SysAuditLog auditLog = pending.auditLog();
                
                // 使用同步方法确保写入成功
                auditLogService.saveAuditLogWithHash(auditLog);
                
                // 写入成功，删除缓冲文件
                localAuditBuffer.markReplayed(pending.filename());
                successCount++;
                
                log.info("审计日志重放成功: {} (操作: {}, 用户: {})", 
                        pending.filename(), auditLog.getAction(), auditLog.getUserId());
                
            } catch (Exception e) {
                failCount++;
                log.error("审计日志重放失败: {}，将保留缓冲文件待下次启动重试", 
                        pending.filename(), e);
            }
        }

        log.info("========== 审计日志重放完成: 成功={}, 失败={} ==========", 
                successCount, failCount);

        if (failCount > 0) {
            log.warn("[WARNING] 仍有 {} 条审计日志重放失败，请检查数据库连接状态", failCount);
        }
    }
}
