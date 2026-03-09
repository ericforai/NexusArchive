// Input: JUnit 5、Mockito、Spring Test、Jackson、本地模块
// Output: AuditLogServiceTest 测试类（校验脱敏快照、上下文补齐、哈希链）
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.util.SM3Utils;
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
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-001");
        request.addHeader("X-Client-Mac", "AA-BB-CC-DD-EE-FF");
        request.addHeader("X-Device-Fingerprint", "device-fp-01");
        request.setRemoteAddr("10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(auditLogMapper.getLatestLogHash()).thenReturn("prev-hash-001");
        when(sm3Utils.hmac(anyString(), anyString())).thenReturn("new-hash-001");

        auditLogService.logBusinessSnapshot(
                "user-001",
                "alice",
                "FONDS_RENAME",
                "FONDS_HISTORY",
                "F001",
                "SUCCESS",
                "关键业务链路审计：全宗重命名",
                "LOW",
                Map.of("fondsNo", "F001", "phone", "13812345678"),
                Map.of("fondsNo", "F002", "approvalTicketId", "AUTH-2026-0001"),
                "F001",
                "F002",
                "AUTH-2026-0001"
        );

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        SysAuditLog savedLog = captor.getValue();

        assertThat(savedLog.getTraceId()).isEqualTo("trace-001");
        assertThat(savedLog.getSourceFonds()).isEqualTo("F001");
        assertThat(savedLog.getTargetFonds()).isEqualTo("F002");
        assertThat(savedLog.getAuthTicketId()).isEqualTo("AUTH-2026-0001");
        assertThat(savedLog.getMacAddress()).isEqualTo("AA-BB-CC-DD-EE-FF");
        assertThat(savedLog.getClientIp()).isEqualTo("10.0.0.8");
        assertThat(savedLog.getPrevLogHash()).isEqualTo("prev-hash-001");
        assertThat(savedLog.getLogHash()).isEqualTo("new-hash-001");
        assertThat(savedLog.getDataBefore()).contains("\"phone\":\"13****78\"");
        assertThat(savedLog.getDataBefore()).doesNotContain("13812345678");
        assertThat(savedLog.getDataAfter()).contains("\"approvalTicketId\":\"AU****01\"");
        assertThat(savedLog.getDataSnapshot()).contains("\"traceId\":\"trace-001\"");
        assertThat(savedLog.getDataSnapshot()).contains("\"macSource\":\"X-Client-Mac\"");
        assertThat(savedLog.getDataSnapshot()).contains("\"authTicketId\":\"AU****01\"");
        assertThat(savedLog.getDataSnapshot()).doesNotContain("AUTH-2026-0001");
    }

    @Test
    @DisplayName("缺少请求时仍会生成降级上下文")
    void logBusinessSnapshot_withoutRequest_usesFallbackContext() {
        when(auditLogMapper.getLatestLogHash()).thenReturn(null);
        when(sm3Utils.hmac(anyString(), anyString())).thenReturn("hash-without-request");

        auditLogService.logBusinessSnapshot(
                "user-002",
                "bob",
                "FONDS_RENAME",
                "FONDS_HISTORY",
                "F009",
                "SUCCESS",
                "关键业务链路审计：全宗重命名",
                null,
                Map.of("email", "ops@example.com"),
                Map.of("fondsNo", "F010"),
                "F009",
                "F010",
                null
        );

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        SysAuditLog savedLog = captor.getValue();

        assertThat(savedLog.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(savedLog.getTraceId()).isNotBlank();
        assertThat(savedLog.getMacAddress()).isEqualTo("UNKNOWN");
        assertThat(savedLog.getClientIp()).isEqualTo("UNKNOWN");
        assertThat(savedLog.getDataBefore()).contains("\"email\":\"op****om\"");
        assertThat(savedLog.getDataSnapshot()).contains("\"macFallback\":true");
    }

    @Test
    @DisplayName("正确计算哈希链 - 包含前一条哈希")
    void saveAuditLogWithHash_calculatesHashChain() {
        SysAuditLog auditLog = createAuditLog("log-001");
        when(auditLogMapper.getLatestLogHash()).thenReturn("prev-hash-123");
        when(sm3Utils.hmac(eq(TEST_HMAC_KEY), anyString())).thenReturn("new-hash-456");

        auditLogService.saveAuditLogWithHash(auditLog);

        verify(auditLogMapper).insert(auditLog);
        assertThat(auditLog.getPrevLogHash()).isEqualTo("prev-hash-123");
        assertThat(auditLog.getLogHash()).isEqualTo("new-hash-456");
        verify(sm3Utils).hmac(eq(TEST_HMAC_KEY), eq(String.format(
                "%s|%s|%s|%s|%s",
                auditLog.getUserId(),
                auditLog.getAction(),
                "",
                auditLog.getCreatedTime().format(TIME_FORMATTER),
                "prev-hash-123"
        )));
    }

    @Test
    @DisplayName("第一条日志会写入空 prevLogHash")
    void saveAuditLogWithHash_firstLog_noPrevHash() {
        SysAuditLog auditLog = createAuditLog("log-001");
        when(auditLogMapper.getLatestLogHash()).thenReturn(null);
        when(sm3Utils.hmac(eq(TEST_HMAC_KEY), anyString())).thenReturn("first-hash");

        auditLogService.saveAuditLogWithHash(auditLog);

        verify(auditLogMapper).insert(auditLog);
        assertThat(auditLog.getPrevLogHash()).isNull();
        assertThat(auditLog.getLogHash()).isEqualTo("first-hash");
    }

    @Test
    @DisplayName("哈希链查询失败时仍会写入日志")
    void saveAuditLogWithHash_whenPreviousHashLookupFails_stillInserts() {
        SysAuditLog auditLog = createAuditLog("log-003");
        when(auditLogMapper.getLatestLogHash()).thenThrow(new RuntimeException("db unavailable"));

        auditLogService.saveAuditLogWithHash(auditLog);

        verify(auditLogMapper).insert(auditLog);
        assertThat(auditLog.getPrevLogHash()).isNull();
        assertThat(auditLog.getLogHash()).isNull();
        verify(sm3Utils, never()).hmac(anyString(), anyString());
    }

    @Test
    @DisplayName("空 HMAC key 时仍会走统一哈希路径")
    void saveAuditLogWithHash_blankHmacKey_stillHashes() {
        SysAuditLog auditLog = createAuditLog("log-004");
        ReflectionTestUtils.setField(auditLogService, "auditLogHmacKey", "");
        when(auditLogMapper.getLatestLogHash()).thenReturn("prev-hash");
        when(sm3Utils.hmac(eq(""), anyString())).thenReturn("fallback-hash");

        auditLogService.saveAuditLogWithHash(auditLog);

        verify(auditLogMapper).insert(auditLog);
        assertThat(auditLog.getPrevLogHash()).isEqualTo("prev-hash");
        assertThat(auditLog.getLogHash()).isEqualTo("fallback-hash");
        verify(sm3Utils).hmac(eq(""), anyString());
    }

    @Test
    @DisplayName("日志链验证会使用同一哈希算法重新计算")
    void verifyLogChain_recalculatesWithHmac() {
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
        assertThat(result.getMessage()).contains("验证通过");
    }

    @Test
    @DisplayName("日志链断裂检测")
    void verifyLogChain_brokenChain_returnsInvalid() {
        SysAuditLog log1 = createLog("log-001", null, "hash-001");
        SysAuditLog log2 = createLog("log-002", "wrong-hash", "hash-002");

        when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(log1, log2));
        when(sm3Utils.hmac(anyString(), anyString()))
                .thenReturn("hash-001", "hash-002");

        AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 9)
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("链条断裂");
    }

    @Test
    @DisplayName("日志篡改检测")
    void verifyLogChain_tamperedLog_returnsInvalid() {
        SysAuditLog log1 = createLog("log-001", null, "original-hash");

        when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(log1));
        when(sm3Utils.hmac(anyString(), anyString()))
                .thenReturn("tampered-hash");

        AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 9)
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("日志篡改");
    }

    @Test
    @DisplayName("空日志范围视为验证通过")
    void verifyLogChain_noLogs_returnsValid() {
        when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 9)
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.getTotalLogs()).isEqualTo(0);
        assertThat(result.getMessage()).contains("无日志");
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
        auditLog.setDetails("创建了档案 ARC-2023-001");
        auditLog.setClientIp("192.168.1.100");
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
