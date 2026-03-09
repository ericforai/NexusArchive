// Input: JUnit 5、Mockito、本地模块
// Output: AuditLogServiceTest 测试类（校验哈希链写入与验真）
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.util.SM3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuditLogServiceTest {

    @Mock
    private SysAuditLogMapper auditLogMapper;

    @Mock
    private SM3Utils sm3Utils;

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(auditLogService, "auditLogHmacKey", "test-hmac-key");
    }

    @Test
    @DisplayName("首条日志写入时会生成首节点哈希并补齐默认上下文")
    void saveAuditLogWithHash_firstLog_persistsGenesisHash() {
        SysAuditLog auditLog = createLog("log-100", null, null, LocalDateTime.of(2026, 3, 9, 9, 30));
        auditLog.setUserId("user-100");
        auditLog.setAction("CREATE");
        auditLog.setObjectDigest("digest-100");

        when(auditLogMapper.getLatestLogHash()).thenReturn(null);
        when(sm3Utils.hmac("test-hmac-key", "user-100|CREATE|digest-100|2026-03-09 09:30:00.000|"))
                .thenReturn("genesis-hash");

        auditLogService.saveAuditLogWithHash(auditLog);

        verify(sm3Utils).hmac("test-hmac-key", "user-100|CREATE|digest-100|2026-03-09 09:30:00.000|");
        verify(auditLogMapper).insert(auditLog);
        assertThat(auditLog.getPrevLogHash()).isNull();
        assertThat(auditLog.getLogHash()).isEqualTo("genesis-hash");
        assertThat(auditLog.getMacAddress()).isEqualTo("UNKNOWN");
        assertThat(auditLog.getClientIp()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("连续写入时后续日志会挂接上一条哈希")
    void saveAuditLogWithHash_consecutiveWrites_linkToPreviousHash() {
        SysAuditLog firstLog = createLog("log-101", null, null, LocalDateTime.of(2026, 3, 9, 10, 0));
        firstLog.setUserId("user-101");
        firstLog.setAction("CREATE");
        firstLog.setObjectDigest("digest-101");

        SysAuditLog secondLog = createLog("log-102", null, null, LocalDateTime.of(2026, 3, 9, 10, 5));
        secondLog.setUserId("user-101");
        secondLog.setAction("UPDATE");
        secondLog.setObjectDigest("digest-102");

        when(auditLogMapper.getLatestLogHash()).thenReturn(null, "hash-001");
        when(sm3Utils.hmac(anyString(), anyString())).thenReturn("hash-001", "hash-002");

        auditLogService.saveAuditLogWithHash(firstLog);
        auditLogService.saveAuditLogWithHash(secondLog);

        verify(auditLogMapper, times(2)).insert(any(SysAuditLog.class));
        assertThat(firstLog.getPrevLogHash()).isNull();
        assertThat(firstLog.getLogHash()).isEqualTo("hash-001");
        assertThat(secondLog.getPrevLogHash()).isEqualTo("hash-001");
        assertThat(secondLog.getLogHash()).isEqualTo("hash-002");
    }

    @Test
    @DisplayName("哈希链查询失败时仍会写入日志")
    void saveAuditLogWithHash_whenPreviousHashLookupFails_stillInserts() {
        SysAuditLog auditLog = createLog("log-103", null, null, LocalDateTime.of(2026, 3, 9, 12, 0));
        auditLog.setUserId("user-103");
        auditLog.setAction("UPDATE");

        when(auditLogMapper.getLatestLogHash()).thenThrow(new RuntimeException("db unavailable"));

        auditLogService.saveAuditLogWithHash(auditLog);

        verify(auditLogMapper).insert(auditLog);
        assertThat(auditLog.getLogHash()).isNull();
    }

    @Test
    @DisplayName("空时间范围验真会返回空结果而不是误报失败")
    void verifyLogChain_emptyRange_returnsValidEmptyResult() {
        when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 9)
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getTotalLogs()).isZero();
        assertThat(result.getVerifiedLogs()).isZero();
        assertThat(result.getMessage()).contains("无日志");
    }

    @Test
    @DisplayName("日志链断链时会被稳定识别")
    void verifyLogChain_brokenLink_detectsMismatch() {
        SysAuditLog log1 = createLog("log-001", null, "hash-001", LocalDateTime.of(2026, 3, 9, 12, 0));
        SysAuditLog log2 = createLog("log-002", "wrong-prev-hash", "hash-002", LocalDateTime.of(2026, 3, 9, 12, 5));

        when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(log1, log2));
        when(sm3Utils.hmac(anyString(), anyString()))
                .thenReturn("hash-001", "hash-002");

        AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 9)
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getVerifiedLogs()).isEqualTo(2);
        assertThat(result.getMessage()).contains("链条断裂");
    }

    @Test
    @DisplayName("日志内容被篡改时会被稳定识别")
    void verifyLogChain_tamperedLog_detectsHashMismatch() {
        SysAuditLog tamperedLog = createLog("log-003", null, "persisted-hash", LocalDateTime.of(2026, 3, 9, 12, 0));
        tamperedLog.setObjectDigest("digest-003");

        when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(tamperedLog));
        when(sm3Utils.hmac(anyString(), anyString())).thenReturn("recalculated-hash");

        AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 9)
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getVerifiedLogs()).isZero();
        assertThat(result.getMessage()).contains("日志篡改");
    }

    private SysAuditLog createLog(String id, String prevHash, String logHash, LocalDateTime createdTime) {
        SysAuditLog log = new SysAuditLog();
        log.setId(id);
        log.setUserId("user-001");
        log.setAction("TEST");
        log.setCreatedTime(createdTime);
        log.setObjectDigest("digest-" + id);
        log.setPrevLogHash(prevHash);
        log.setLogHash(logHash);
        return log;
    }
}
