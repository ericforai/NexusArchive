// Input: JUnit + AuditLogService
// Output: 审计哈希链测试
// Pos: NexusCore tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogServiceTests {
    private AuditLogService auditLogService;
    private Sm3HashService hashService;

    @BeforeEach
    void setUp() {
        hashService = new Sm3HashService();
        auditLogService = new AuditLogService(hashService);
    }

    @Test
    void shouldWriteAuditLogWithHashChain() {
        AuditLogEntry entry1 = createEntry("user1", "LOGIN", "session-1");
        AuditLogEntry entry2 = createEntry("user1", "VIEW", "archive-123");
        AuditLogEntry entry3 = createEntry("user1", "DOWNLOAD", "file-456");

        auditLogService.log(entry1);
        auditLogService.log(entry2);
        auditLogService.log(entry3);

        // 验证链式哈希
        assertEquals(1L, entry1.getChainSeq());
        assertEquals(2L, entry2.getChainSeq());
        assertEquals(3L, entry3.getChainSeq());

        // entry2 的 prevHash 应该等于 entry1 的 currHash
        assertEquals(entry1.getCurrHash(), entry2.getPrevHash());
        assertEquals(entry2.getCurrHash(), entry3.getPrevHash());
    }

    @Test
    void shouldVerifyChainIntegrity() {
        for (int i = 0; i < 5; i++) {
            auditLogService.log(createEntry("user" + i, "ACTION" + i, "target" + i));
        }

        LocalDateTime from = LocalDateTime.now().minusMinutes(1);
        LocalDateTime to = LocalDateTime.now().plusMinutes(1);
        ChainVerifyResult result = auditLogService.verifyChain(from, to);

        assertTrue(result.valid());
        assertEquals(5, result.totalRecords());
        assertEquals(5, result.verifiedRecords());
        assertTrue(result.breaks().isEmpty());
    }

    private AuditLogEntry createEntry(String operator, String action, String target) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setTraceId("trace-" + System.nanoTime());
        entry.setOperator(operator);
        entry.setOperatorName(operator);
        entry.setAction(action);
        entry.setTarget(target);
        entry.setDataSnapshot("{}");
        return entry;
    }
}
