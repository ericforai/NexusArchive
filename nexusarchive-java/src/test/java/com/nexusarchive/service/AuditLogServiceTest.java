// Input: JUnit 5、Mockito、Spring Test、Jackson、本地模块
// Output: AuditLogServiceTest 测试类（校验脱敏快照、上下文补齐、哈希链写入与验真）
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
import java.util.List;
import java.util.Map;

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

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(auditLogService, "auditLogHmacKey", "test-hmac-key");
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
    @DisplayName("哈希链查询失败时仍会写入日志")
    void saveAuditLogWithHash_whenPreviousHashLookupFails_stillInserts() {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setUserId("user-003");
        auditLog.setAction("UPDATE");
        auditLog.setCreatedTime(LocalDateTime.of(2026, 3, 9, 12, 0));

        when(auditLogMapper.getLatestLogHash()).thenThrow(new RuntimeException("db unavailable"));

        auditLogService.saveAuditLogWithHash(auditLog);

        verify(auditLogMapper).insert(auditLog);
        assertThat(auditLog.getLogHash()).isNull();
    }

    @Test
    @DisplayName("首条日志写入时会生成首节点哈希并补齐默认上下文")
    void saveAuditLogWithHash_firstLog_persistsGenesisHash() {
        SysAuditLog auditLog = createLog("log-100", null, null);
        auditLog.setUserId("user-100");
        auditLog.setAction("CREATE");
        auditLog.setObjectDigest("digest-100");
        auditLog.setCreatedTime(LocalDateTime.of(2026, 3, 9, 9, 30));

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
        SysAuditLog firstLog = createLog("log-101", null, null);
        firstLog.setUserId("user-101");
        firstLog.setAction("CREATE");
        firstLog.setObjectDigest("digest-101");
        firstLog.setCreatedTime(LocalDateTime.of(2026, 3, 9, 10, 0));

        SysAuditLog secondLog = createLog("log-102", null, null);
        secondLog.setUserId("user-101");
        secondLog.setAction("UPDATE");
        secondLog.setObjectDigest("digest-102");
        secondLog.setCreatedTime(LocalDateTime.of(2026, 3, 9, 10, 5));

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
        SysAuditLog log1 = createLog("log-001", null, "hash-001");
        SysAuditLog log2 = createLog("log-002", "wrong-prev-hash", "hash-002");

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
        SysAuditLog tamperedLog = createLog("log-003", null, "persisted-hash");
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

    private SysAuditLog createLog(String id, String prevHash, String logHash) {
        SysAuditLog log = new SysAuditLog();
        log.setId(id);
        log.setUserId("user-001");
        log.setAction("TEST");
        log.setCreatedTime(LocalDateTime.of(2026, 3, 9, 12, 0));
        log.setObjectDigest("digest-" + id);
        log.setPrevLogHash(prevHash);
        log.setLogHash(logHash);
        return log;
    }
}
