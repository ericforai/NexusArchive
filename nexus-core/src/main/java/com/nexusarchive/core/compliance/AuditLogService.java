// Input: 审计日志
// Output: 哈希链审计服务
// Pos: NexusCore compliance/audit
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务
 * 
 * Roadmap 来源: 阶段二 - 实现 curr_hash = SM3(prev_hash + data)
 */
@Service
public class AuditLogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogService.class);
    private static final String GENESIS_HASH = "0".repeat(64);

    private final Sm3HashService hashService;
    
    // 内存存储 (实际应注入 AuditLogMapper)
    private final List<AuditLogEntry> logStore = new ArrayList<>();
    private final AtomicLong seqCounter = new AtomicLong(0);

    public AuditLogService(Sm3HashService hashService) {
        this.hashService = Objects.requireNonNull(hashService);
    }

    /**
     * 写入审计日志 (自动计算哈希链)
     */
    public void log(AuditLogEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        
        synchronized (logStore) {
            String prevHash = getLatestHash();
            
            // 先设置 actionTime，因为它会包含在 hashPayload 中
            entry.setActionTime(LocalDateTime.now());
            entry.setPrevHash(prevHash);
            entry.setChainSeq(seqCounter.incrementAndGet());
            
            String dataPayload = entry.toHashPayload();
            String currHash = hashService.hashSm3(prevHash + dataPayload);
            entry.setCurrHash(currHash);
            
            logStore.add(entry);
            LOGGER.info("审计日志写入: seq={}, action={}, hash={}", 
                    entry.getChainSeq(), entry.getAction(), currHash.substring(0, 16) + "...");
        }
    }

    /**
     * 验证审计链完整性
     * [P0-FIX] 整个验证过程在 synchronized 块内执行，确保并发一致性
     */
    public ChainVerifyResult verifyChain(LocalDateTime from, LocalDateTime to) {
        synchronized (logStore) {
            List<AuditLogEntry> entries = logStore.stream()
                    .filter(e -> !e.getActionTime().isBefore(from) && !e.getActionTime().isAfter(to))
                    .toList();
            
            if (entries.isEmpty()) {
                return ChainVerifyResult.success(0);
            }

            List<ChainBreak> breaks = new ArrayList<>();
            long verifiedCount = 0;

            for (int i = 0; i < entries.size(); i++) {
                AuditLogEntry entry = entries.get(i);
                String expectedPrevHash = (i == 0) ? GENESIS_HASH : entries.get(i - 1).getCurrHash();
                
                // 验证 prevHash 链接
                if (!expectedPrevHash.equals(entry.getPrevHash())) {
                    breaks.add(new ChainBreak(entry.getChainSeq(), expectedPrevHash, entry.getPrevHash()));
                    continue;
                }
                
                // 重新计算 currHash
                String recomputedHash = hashService.hashSm3(entry.getPrevHash() + entry.toHashPayload());
                if (!recomputedHash.equals(entry.getCurrHash())) {
                    breaks.add(new ChainBreak(entry.getChainSeq(), recomputedHash, entry.getCurrHash()));
                    continue;
                }
                
                verifiedCount++;
            }

            if (breaks.isEmpty()) {
                return ChainVerifyResult.success(entries.size());
            }
            return ChainVerifyResult.failure(entries.size(), verifiedCount, breaks);
        }
    }

    private String getLatestHash() {
        if (logStore.isEmpty()) {
            return GENESIS_HASH;
        }
        return logStore.get(logStore.size() - 1).getCurrHash();
    }
}
