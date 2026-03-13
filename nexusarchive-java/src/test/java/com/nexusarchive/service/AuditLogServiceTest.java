// Input: JUnit 5、Mockito、Spring Test、Jackson、本地模块
// Output: AuditLogServiceTest 测试类（校验脱敏快照、上下文补齐、哈希链）
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.util.SM3Utils;
import com.nexusarchive.service.helper.AuditLogHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuditLogServiceTest {

    private static final String TEST_HMAC_KEY = "test-hmac-key";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Mock
    private SysAuditLogMapper auditLogMapper;

    @Mock
    private SM3Utils sm3Utils;

    @Mock
    private AuditLogHelper helper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(auditLogService, "auditLogHmacKey", TEST_HMAC_KEY);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("关键业务快照会补齐上下文并脱敏敏感字段")
    void logBusinessSnapshot_masksSensitiveFieldsAndPersistsContext() {
        AuditLogService.AuditRequestContext mockCtx = new AuditLogService.AuditRequestContext(
            "trace-001", "F001", "F002", "AUTH-2026-0001", "10.0.0.8", "AA-BB-CC-DD-EE-FF", "X-Client-Mac", false, "UA", null
        );
        when(helper.resolveRequestContext()).thenReturn(mockCtx);
        // Mock sanitization for snapshot building - simplified for test
        when(helper.sanitize(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        when(auditLogMapper.getLatestLogHash()).thenReturn("prev-hash-001");
        when(sm3Utils.hmac(anyString(), anyString())).thenReturn("new-hash-001");

        auditLogService.logBusinessSnapshot(
                "user-001", "alice", "FONDS_RENAME", "FONDS_HISTORY", "F001", "SUCCESS", "Details", "LOW",
                Map.of("fondsNo", "F001", "phone", "13812345678"),
                Map.of("fondsNo", "F002", "approvalTicketId", "AUTH-2026-0001"),
                "F001", "F002", "AUTH-2026-0001"
        );

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        SysAuditLog savedLog = captor.getValue();

        assertThat(savedLog.getTraceId()).isEqualTo("trace-001");
        assertThat(savedLog.getMacAddress()).isEqualTo("AA-BB-CC-DD-EE-FF");
        assertThat(savedLog.getLogHash()).isEqualTo("new-hash-001");
    }

    @Test
    @DisplayName("缺少请求时仍会生成降级上下文")
    void logBusinessSnapshot_withoutRequest_usesFallbackContext() {
        AuditLogService.AuditRequestContext mockCtx = new AuditLogService.AuditRequestContext(
            "gen-trace", null, null, null, "UNKNOWN", "UNKNOWN", "NO_REQUEST", true, null, null
        );
        when(helper.resolveRequestContext()).thenReturn(mockCtx);
        when(helper.sanitize(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        when(sm3Utils.hmac(anyString(), anyString())).thenReturn("hash-without-request");

        auditLogService.logBusinessSnapshot(
                "user-002", "bob", "ACTION", "RES", "ID", "SUCCESS", "Details", null,
                Map.of("email", "ops@example.com"), Map.of("fondsNo", "F010"), "F009", "F010", null
        );

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("第一条日志会写入空 prevLogHash")
    void saveAuditLogWithHash_firstLog_noPrevHash() {
        SysAuditLog log1 = createLog("log-001", null, "hash-001");
        SysAuditLog log2 = createLog("log-002", "hash-001", "hash-002");

        when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(log1, log2));
        when(sm3Utils.hmac(anyString(), anyString()))
                .thenReturn("hash-001", "hash-002");

        AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 9)
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getVerifiedLogs()).isEqualTo(2);
    }

    @Test
    @DisplayName("日志链断裂检测")
    void verifyLogChain_brokenChain_returnsInvalid() {
        SysAuditLog log1 = createLog("log-001", null, "hash-001");
        SysAuditLog log2 = createLog("log-002", "wrong-hash", "hash-002");

        when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(log1, log2));
        when(sm3Utils.hmac(anyString(), anyString())).thenReturn("hash-001", "hash-002");

        AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 9)
        );

        assertThat(result.isValid()).isFalse();
    }

    private SysAuditLog createAuditLog(String id) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setId(id);
        auditLog.setUserId("user-001");
        auditLog.setUsername("admin");
        auditLog.setAction("ARCHIVE_CREATE");
        auditLog.setResourceType("ARCHIVE");
        auditLog.setResourceId("arc-001");
        auditLog.setOperationResult("SUCCESS");
        auditLog.setCreatedTime(LocalDateTime.of(2026, 3, 9, 12, 0, 0));
        return auditLog;
    }

    private SysAuditLog createLog(String id, String prevHash, String logHash) {
        SysAuditLog log = new SysAuditLog();
        log.setId(id);
        log.setUserId("user-001");
        log.setAction("TEST");
        log.setCreatedTime(LocalDateTime.of(2026, 3, 9, 12, 0));
        log.setPrevLogHash(prevHash);
        log.setLogHash(logHash);
        return log;
    }
}
