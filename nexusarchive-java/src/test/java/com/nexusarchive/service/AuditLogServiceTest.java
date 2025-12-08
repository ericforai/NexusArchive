package com.nexusarchive.service;

import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.util.SM3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuditLogService 单元测试
 * 
 * 测试覆盖:
 * - 审计日志记录
 * - 哈希链计算
 * - 日志链完整性验证
 * - 日志篡改检测
 * 
 * 合规要求参考: GB/T 39784-2021 表36
 * 
 * @author Agent E - 质量保障工程师
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private SysAuditLogMapper auditLogMapper;

    @Mock
    private SM3Utils sm3Utils;

    @InjectMocks
    private AuditLogService auditLogService;

    private SysAuditLog testAuditLog;

    @BeforeEach
    void setUp() {
        testAuditLog = new SysAuditLog();
        testAuditLog.setId("log-001");
        testAuditLog.setUserId("user-001");
        testAuditLog.setUsername("admin");
        testAuditLog.setAction("ARCHIVE_CREATE");
        testAuditLog.setResourceType("ARCHIVE");
        testAuditLog.setResourceId("arc-001");
        testAuditLog.setOperationResult("SUCCESS");
        testAuditLog.setDetails("创建了档案 ARC-2023-001");
        testAuditLog.setClientIp("192.168.1.100");
        testAuditLog.setCreatedTime(LocalDateTime.now());
    }

    // ========== 日志记录测试 ==========

    @Nested
    @DisplayName("审计日志记录")
    class LogTests {

        @Test
        @DisplayName("简化方法记录日志成功")
        void log_SimpleMethod_Success() {
            // Arrange
            when(auditLogMapper.getLatestLogHash()).thenReturn("prev-hash-123");
            when(sm3Utils.calculateLogHash(any(), any(), any(), any(), any())).thenReturn("new-hash-456");
            when(auditLogMapper.insert(any(SysAuditLog.class))).thenReturn(1);

            // Act
            auditLogService.log(
                    "user-001", "admin", "LOGIN", 
                    "USER", "user-001", 
                    "SUCCESS", "用户登录成功", "192.168.1.100"
            );

            // Assert
            ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
            verify(auditLogMapper).insert(captor.capture());

            SysAuditLog savedLog = captor.getValue();
            assertThat(savedLog.getUserId()).isEqualTo("user-001");
            assertThat(savedLog.getAction()).isEqualTo("LOGIN");
            assertThat(savedLog.getLogHash()).isEqualTo("new-hash-456");
            assertThat(savedLog.getPrevLogHash()).isEqualTo("prev-hash-123");
        }

        @Test
        @DisplayName("直接传入对象记录日志成功")
        void log_DirectObject_Success() {
            // Arrange
            when(auditLogMapper.getLatestLogHash()).thenReturn(null); // 第一条日志
            when(sm3Utils.calculateLogHash(any(), any(), any(), any(), any())).thenReturn("first-hash");
            when(auditLogMapper.insert(any(SysAuditLog.class))).thenReturn(1);

            // Act
            auditLogService.log(testAuditLog);

            // Assert
            verify(auditLogMapper).insert(testAuditLog);
            assertThat(testAuditLog.getLogHash()).isEqualTo("first-hash");
            assertThat(testAuditLog.getPrevLogHash()).isNull();
        }

        @Test
        @DisplayName("自动生成ID和时间")
        void log_AutoGenerateIdAndTime() {
            // Arrange
            SysAuditLog logWithoutIdAndTime = new SysAuditLog();
            logWithoutIdAndTime.setUserId("user-001");
            logWithoutIdAndTime.setAction("CREATE");
            
            when(auditLogMapper.getLatestLogHash()).thenReturn("prev-hash");
            when(sm3Utils.calculateLogHash(any(), any(), any(), any(), any())).thenReturn("new-hash");
            when(auditLogMapper.insert(any(SysAuditLog.class))).thenReturn(1);

            // Act
            auditLogService.log(logWithoutIdAndTime);

            // Assert
            assertThat(logWithoutIdAndTime.getId()).isNotNull().isNotEmpty();
            assertThat(logWithoutIdAndTime.getCreatedTime()).isNotNull();
        }
    }

    // ========== 哈希链计算测试 ==========

    @Nested
    @DisplayName("哈希链计算")
    class HashChainTests {

        @Test
        @DisplayName("正确计算哈希链 - 包含前一条哈希")
        void saveAuditLogWithHash_CalculatesHashChain() {
            // Arrange
            String prevHash = "abc123";
            String expectedHash = "xyz789";
            
            when(auditLogMapper.getLatestLogHash()).thenReturn(prevHash);
            when(sm3Utils.calculateLogHash(
                    eq("user-001"),
                    eq("ARCHIVE_CREATE"),
                    any(),
                    anyString(),
                    eq(prevHash)
            )).thenReturn(expectedHash);
            when(auditLogMapper.insert(any(SysAuditLog.class))).thenReturn(1);

            // Act
            auditLogService.saveAuditLogWithHash(testAuditLog);

            // Assert
            assertThat(testAuditLog.getPrevLogHash()).isEqualTo(prevHash);
            assertThat(testAuditLog.getLogHash()).isEqualTo(expectedHash);
        }

        @Test
        @DisplayName("第一条日志 - prevLogHash为空")
        void saveAuditLogWithHash_FirstLog_NoPrevHash() {
            // Arrange
            when(auditLogMapper.getLatestLogHash()).thenReturn(null);
            when(sm3Utils.calculateLogHash(any(), any(), any(), any(), isNull())).thenReturn("first-hash");
            when(auditLogMapper.insert(any(SysAuditLog.class))).thenReturn(1);

            // Act
            auditLogService.saveAuditLogWithHash(testAuditLog);

            // Assert
            assertThat(testAuditLog.getPrevLogHash()).isNull();
            assertThat(testAuditLog.getLogHash()).isEqualTo("first-hash");
        }

        @Test
        @DisplayName("哈希计算失败仍保存日志")
        void saveAuditLogWithHash_HashFailed_StillSavesLog() {
            // Arrange
            when(auditLogMapper.getLatestLogHash()).thenThrow(new RuntimeException("DB error"));
            when(auditLogMapper.insert(any(SysAuditLog.class))).thenReturn(1);

            // Act
            auditLogService.saveAuditLogWithHash(testAuditLog);

            // Assert - 确保日志仍被保存
            verify(auditLogMapper).insert(testAuditLog);
        }
    }

    // ========== 日志链验证测试 ==========

    @Nested
    @DisplayName("日志链完整性验证")
    class VerifyLogChainTests {

        @Test
        @DisplayName("日志链验证通过")
        void verifyLogChain_ValidChain_ReturnsValid() {
            // Arrange
            SysAuditLog log1 = createLogWithHash("log-001", null, "hash-001");
            SysAuditLog log2 = createLogWithHash("log-002", "hash-001", "hash-002");
            SysAuditLog log3 = createLogWithHash("log-003", "hash-002", "hash-003");

            List<SysAuditLog> logs = Arrays.asList(log1, log2, log3);
            when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class))).thenReturn(logs);
            
            // Mock hash calculation to return same hash
            when(sm3Utils.calculateLogHash(any(), any(), any(), any(), isNull())).thenReturn("hash-001");
            when(sm3Utils.calculateLogHash(any(), any(), any(), any(), eq("hash-001"))).thenReturn("hash-002");
            when(sm3Utils.calculateLogHash(any(), any(), any(), any(), eq("hash-002"))).thenReturn("hash-003");

            // Act
            AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                    LocalDate.now().minusDays(7), LocalDate.now()
            );

            // Assert
            assertThat(result.isValid()).isTrue();
            assertThat(result.getTotalLogs()).isEqualTo(3);
            assertThat(result.getVerifiedLogs()).isEqualTo(3);
            assertThat(result.getMessage()).contains("验证通过");
        }

        @Test
        @DisplayName("日志链断裂检测")
        void verifyLogChain_BrokenChain_ReturnsInvalid() {
            // Arrange - log2的prevLogHash与log1的logHash不匹配
            SysAuditLog log1 = createLogWithHash("log-001", null, "hash-001");
            SysAuditLog log2 = createLogWithHash("log-002", "wrong-hash", "hash-002"); // 链断裂
            
            List<SysAuditLog> logs = Arrays.asList(log1, log2);
            when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class))).thenReturn(logs);
            when(sm3Utils.calculateLogHash(any(), any(), any(), any(), any())).thenReturn("hash-001", "hash-002");

            // Act
            AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                    LocalDate.now().minusDays(7), LocalDate.now()
            );

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("链条断裂");
        }

        @Test
        @DisplayName("日志篡改检测")
        void verifyLogChain_TamperedLog_ReturnsInvalid() {
            // Arrange - 重新计算的哈希与存储的哈希不匹配
            SysAuditLog log1 = createLogWithHash("log-001", null, "original-hash");
            
            List<SysAuditLog> logs = Collections.singletonList(log1);
            when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class))).thenReturn(logs);
            // 重新计算得到不同的哈希，表明日志被篡改
            when(sm3Utils.calculateLogHash(any(), any(), any(), any(), any())).thenReturn("tampered-hash");

            // Act
            AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                    LocalDate.now().minusDays(7), LocalDate.now()
            );

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("日志篡改");
        }

        @Test
        @DisplayName("空日志范围 - 验证通过")
        void verifyLogChain_NoLogs_ReturnsValid() {
            // Arrange
            when(auditLogMapper.findByDateRange(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            AuditLogService.LogChainVerifyResult result = auditLogService.verifyLogChain(
                    LocalDate.now().minusDays(7), LocalDate.now()
            );

            // Assert
            assertThat(result.isValid()).isTrue();
            assertThat(result.getTotalLogs()).isEqualTo(0);
            assertThat(result.getMessage()).contains("无日志");
        }
    }

    // ========== 辅助方法 ==========

    private SysAuditLog createLogWithHash(String id, String prevHash, String logHash) {
        SysAuditLog log = new SysAuditLog();
        log.setId(id);
        log.setUserId("user-001");
        log.setAction("TEST_ACTION");
        log.setCreatedTime(LocalDateTime.now());
        log.setPrevLogHash(prevHash);
        log.setLogHash(logHash);
        return log;
    }
}
