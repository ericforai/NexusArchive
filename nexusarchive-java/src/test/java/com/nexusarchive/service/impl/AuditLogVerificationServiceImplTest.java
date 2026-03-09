// Input: JUnit 5、Mockito、本地模块
// Output: AuditLogVerificationServiceImplTest 测试类（校验验真服务与哈希链服务协作）
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.ChainVerificationResult;
import com.nexusarchive.dto.VerificationResult;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.util.SM3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuditLogVerificationServiceImplTest {

    @Mock
    private SysAuditLogMapper auditLogMapper;

    @Mock
    private SM3Utils sm3Utils;

    private AuditLogService auditLogService;
    private AuditLogVerificationServiceImpl verificationService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogMapper, sm3Utils);
        verificationService = new AuditLogVerificationServiceImpl(auditLogService, auditLogMapper, sm3Utils);
        ReflectionTestUtils.setField(auditLogService, "auditLogHmacKey", "test-hmac-key");
        ReflectionTestUtils.setField(verificationService, "auditLogHmacKey", "test-hmac-key");
    }

    @Test
    @DisplayName("按日期范围验真时会透传真实链路错误并统计无效日志")
    void verifyChain_detectsBrokenLinkFromAuditLogService() {
        SysAuditLog firstLog = createLog("log-201", null, "hash-201", LocalDateTime.of(2026, 3, 9, 10, 0));
        SysAuditLog secondLog = createLog("log-202", "wrong-prev-hash", "hash-202", LocalDateTime.of(2026, 3, 9, 10, 5));

        when(auditLogMapper.findByDateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 9)))
                .thenReturn(List.of(firstLog, secondLog));
        when(sm3Utils.hmac(anyString(), anyString())).thenReturn("hash-201", "hash-202");

        ChainVerificationResult result = verificationService.verifyChain(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 9),
                null
        );

        assertThat(result.isChainIntact()).isFalse();
        assertThat(result.getTotalLogs()).isEqualTo(2);
        assertThat(result.getValidLogs()).isEqualTo(1);
        assertThat(result.getInvalidLogs()).isEqualTo(1);
        assertThat(result.getInvalidResults()).singleElement()
                .extracting(VerificationResult::getReason)
                .asString()
                .contains("链条断裂");
    }

    @Test
    @DisplayName("按日志 ID 验真时会识别被篡改的日志")
    void verifyChainByLogIds_detectsTamperedLog() {
        SysAuditLog tamperedLog = createLog("log-301", null, "persisted-hash", LocalDateTime.of(2026, 3, 9, 11, 0));

        when(auditLogMapper.selectBatchIds(List.of("log-301"))).thenReturn(List.of(tamperedLog));
        when(auditLogMapper.selectById("log-301")).thenReturn(tamperedLog);
        when(sm3Utils.hmac(anyString(), anyString())).thenReturn("recalculated-hash");

        ChainVerificationResult result = verificationService.verifyChainByLogIds(List.of("log-301"));

        assertThat(result.isChainIntact()).isFalse();
        assertThat(result.getTotalLogs()).isEqualTo(1);
        assertThat(result.getValidLogs()).isZero();
        assertThat(result.getInvalidLogs()).isEqualTo(1);
        assertThat(result.getInvalidResults()).singleElement()
                .extracting(VerificationResult::getReason)
                .asString()
                .contains("哈希值不匹配");
    }

    @Test
    @DisplayName("空日志 ID 列表验真时返回空结果")
    void verifyChainByLogIds_emptyInput_returnsEmptyResult() {
        ChainVerificationResult result = verificationService.verifyChainByLogIds(List.of());

        assertThat(result.isChainIntact()).isTrue();
        assertThat(result.getTotalLogs()).isZero();
        assertThat(result.getValidLogs()).isZero();
        assertThat(result.getInvalidLogs()).isZero();
        assertThat(result.getInvalidResults()).isEmpty();
    }

    private SysAuditLog createLog(String id, String prevHash, String logHash, LocalDateTime createdTime) {
        SysAuditLog log = new SysAuditLog();
        log.setId(id);
        log.setUserId("user-201");
        log.setAction("TEST");
        log.setObjectDigest("digest-" + id);
        log.setCreatedTime(createdTime);
        log.setPrevLogHash(prevHash);
        log.setLogHash(logHash);
        return log;
    }
}
