package com.nexusarchive.service;

import com.nexusarchive.dto.ChainVerificationResult;
import com.nexusarchive.dto.VerificationResult;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.service.impl.AuditLogVerificationServiceImpl;
import com.nexusarchive.util.SM3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuditLogVerificationServiceImplTest {

    @Mock
    private SysAuditLogMapper auditLogMapper;

    @Mock
    private SM3Utils sm3Utils;

    @InjectMocks
    private AuditLogVerificationServiceImpl verificationService;

    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.of(2026, 3, 1, 10, 0, 0);
    }

    @Test
    @DisplayName("单条验真在日志不存在时返回缺失结果")
    void verifySingleLog_WhenLogMissing_ReturnsMissingLogResult() {
        when(auditLogMapper.selectById("missing-log")).thenReturn(null);

        VerificationResult result = verificationService.verifySingleLog("missing-log");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getLogId()).isEqualTo("missing-log");
        assertThat(result.getIssueType()).isEqualTo("MISSING_LOG");
        assertThat(result.getReason()).contains("不存在");
    }

    @Test
    @DisplayName("单条验真在哈希不匹配时返回篡改结果")
    void verifySingleLog_WhenHashMismatched_ReturnsHashMismatchResult() {
        SysAuditLog log = createLog("log-001", null, "stored-hash");
        when(auditLogMapper.selectById("log-001")).thenReturn(log);
        when(sm3Utils.hmac(any(), anyString())).thenReturn("recalculated-hash");

        VerificationResult result = verificationService.verifySingleLog("log-001");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getIssueType()).isEqualTo("HASH_MISMATCH");
        assertThat(result.getExpectedHash()).isEqualTo("stored-hash");
        assertThat(result.getActualHash()).isEqualTo("recalculated-hash");
    }

    @Test
    @DisplayName("单条验真在前序日志缺失时返回缺失结果")
    void verifySingleLog_WhenPreviousLogMissing_ReturnsMissingPreviousResult() {
        SysAuditLog log = createLog("log-001", "missing-prev-hash", "stored-hash");
        when(auditLogMapper.selectById("log-001")).thenReturn(log);
        when(sm3Utils.hmac(any(), anyString())).thenReturn("stored-hash");
        when(auditLogMapper.selectOne(argThat(wrapper -> wrapper != null))).thenReturn(null);

        VerificationResult result = verificationService.verifySingleLog("log-001");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getIssueType()).isEqualTo("MISSING_LOG");
        assertThat(result.getExpectedHash()).isEqualTo("missing-prev-hash");
        assertThat(result.getReason()).contains("前序审计日志缺失");
    }

    @Test
    @DisplayName("单条验真按 prevLogHash 查找前序日志并通过链校验")
    void verifySingleLog_WhenPreviousLogResolvedByPrevHash_ReturnsValidResult() {
        SysAuditLog previous = createLog("log-000", null, "prev-hash");
        previous.setCreatedTime(baseTime.plusMinutes(5));
        SysAuditLog current = createLog("log-001", "prev-hash", "current-hash");
        current.setCreatedTime(baseTime);

        when(auditLogMapper.selectById("log-001")).thenReturn(current);
        when(auditLogMapper.selectOne(argThat(wrapper -> wrapper != null))).thenReturn(previous);
        when(sm3Utils.hmac(any(), anyString())).thenReturn("current-hash");

        VerificationResult result = verificationService.verifySingleLog("log-001");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getIssueType()).isEqualTo("OK");
        assertThat(result.getExpectedHash()).isEqualTo("current-hash");
    }

    @Test
    @DisplayName("按ID验真可识别缺失日志")
    void verifyChainByLogIds_WhenRequestedLogMissing_ReturnsMissingEntry() {
        SysAuditLog existing = createLog("log-001", null, "hash-001");
        when(auditLogMapper.findByIdsInOrder(List.of("log-001", "log-002"))).thenReturn(List.of(existing));
        when(sm3Utils.hmac(any(), anyString())).thenReturn("hash-001");

        ChainVerificationResult result = verificationService.verifyChainByLogIds(List.of("log-001", "log-002"));

        assertThat(result.isChainIntact()).isFalse();
        assertThat(result.getTotalLogs()).isEqualTo(2);
        assertThat(result.getInvalidLogs()).isEqualTo(1);
        assertThat(result.getInvalidResults())
                .extracting(VerificationResult::getIssueType)
                .containsExactly("MISSING_LOG");
    }

    @Test
    @DisplayName("按ID验真可识别断链")
    void verifyChainByLogIds_WhenPrevHashBroken_ReturnsBrokenChainEntry() {
        SysAuditLog first = createLog("log-001", null, "hash-001");
        SysAuditLog second = createLog("log-002", "wrong-prev-hash", "hash-002");
        when(auditLogMapper.findByIdsInOrder(List.of("log-001", "log-002")))
                .thenReturn(List.of(first, second));
        when(sm3Utils.hmac(any(), anyString())).thenReturn("hash-001", "hash-002");

        ChainVerificationResult result = verificationService.verifyChainByLogIds(List.of("log-001", "log-002"));

        assertThat(result.isChainIntact()).isFalse();
        assertThat(result.getInvalidResults())
                .extracting(VerificationResult::getIssueType)
                .contains("BROKEN_CHAIN");
    }

    @Test
    @DisplayName("按时间范围和全宗过滤验真仅验证过滤后的链")
    void verifyChain_WhenFilteredByDateAndFonds_ReturnsFilteredChainResult() {
        SysAuditLog first = createLog("log-101", null, "hash-101");
        first.setSourceFonds("F001");
        SysAuditLog second = createLog("log-102", "hash-101", "hash-102");
        second.setSourceFonds("F001");

        when(auditLogMapper.findByDateRangeAndFondsNo(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 2),
                "F001"))
                .thenReturn(List.of(first, second));
        when(sm3Utils.hmac(any(), anyString())).thenReturn("hash-101", "hash-102");

        ChainVerificationResult result = verificationService.verifyChain(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 2),
                "F001");

        assertThat(result.isChainIntact()).isTrue();
        assertThat(result.getTotalLogs()).isEqualTo(2);
        assertThat(result.getValidLogs()).isEqualTo(2);
        assertThat(result.getInvalidResults()).isEmpty();
    }

    private SysAuditLog createLog(String id, String prevHash, String hash) {
        SysAuditLog log = new SysAuditLog();
        log.setId(id);
        log.setUserId("user-001");
        log.setAction("ARCHIVE_CREATE");
        log.setObjectDigest("digest-001");
        log.setCreatedTime(baseTime.plusSeconds(id.hashCode() & 15));
        log.setPrevLogHash(prevHash);
        log.setLogHash(hash);
        return log;
    }
}
