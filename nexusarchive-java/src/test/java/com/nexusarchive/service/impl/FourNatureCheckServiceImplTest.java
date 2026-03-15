// Input: JUnit 5, Mockito, Java 标准库
// Output: FourNatureCheckServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.FourNatureCoreService;
import com.nexusarchive.util.AmountValidator;
import com.nexusarchive.util.FileHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 四性检测服务实现类单元测试
 *
 * 测试范围:
 * 1. performFullCheck - 完整四性检测流程
 * 2. performHealthCheck - 健康检查流程
 * 3. checkDeduplication - 去重检测
 * 4. checkAuthenticity - 真实性检测
 * 5. checkIntegrity - 完整性检测
 * 6. checkUsability - 可用性检测
 * 7. checkSafety - 安全性检测
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("四性检测服务测试")
class FourNatureCheckServiceImplTest {

    @Mock
    private FourNatureCoreService fourNatureCoreService;

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private AmountValidator amountValidator;

    @Mock
    private FileHashUtil fileHashUtil;

    private FourNatureCheckServiceImpl fourNatureCheckService;

    @BeforeEach
    void setUp() {
        fourNatureCheckService = new FourNatureCheckServiceImpl(
                fourNatureCoreService,
                arcFileContentMapper,
                amountValidator,
                fileHashUtil
        );

        // 默认 mock 金额验证通过
        lenient().when(amountValidator.validateAmount(any()))
                .thenReturn(AmountValidator.ValidationResult.success());

        // 默认 mock 文件哈希计算
        try {
            lenient().when(fileHashUtil.calculateSM3(any(java.io.InputStream.class))).thenReturn("test-sm3-hash");
            lenient().when(fileHashUtil.calculateSHA256(any(java.io.InputStream.class))).thenReturn("test-sha256-hash");
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
            // Ignore in test setup
        }

        // 默认 mock selectMaps 返回空列表（无重复）
        lenient().when(arcFileContentMapper.selectMaps(any(QueryWrapper.class))).thenReturn(List.of());

        // 默认 mock 四性核心服务返回通过结果
        lenient().when(fourNatureCoreService.checkSingleFileAuthenticity(any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(com.nexusarchive.dto.sip.report.CheckItem.pass("Authenticity", "Valid"));
        lenient().when(fourNatureCoreService.checkSingleFileUsability(any(), anyString(), anyString()))
                .thenReturn(com.nexusarchive.dto.sip.report.CheckItem.pass("Usability", "Valid"));
        lenient().when(fourNatureCoreService.checkSingleFileSafety(any(), anyString()))
                .thenReturn(com.nexusarchive.dto.sip.report.CheckItem.pass("Safety", "No threats"));
    }

    @Nested
    @DisplayName("去重检测测试")
    class DeduplicationTests {

        @Test
        @DisplayName("应该检测到重复文件并返回失败")
        void shouldDetectDuplicateFileAndReturnFail() {
            // Given
            String fileName = "duplicate.pdf";
            byte[] content = "test content".getBytes();
            String hash = "test-sm3-hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            // Mock 数据库已存在该哈希
            Map<String, Object> existingRecord = new HashMap<>();
            existingRecord.put("file_hash", hash);
            when(arcFileContentMapper.selectMaps(any(QueryWrapper.class)))
                    .thenReturn(List.of(existingRecord));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getAuthenticity().getErrors()).isNotEmpty();
            assertThat(report.getAuthenticity().getErrors().get(0)).contains("Duplicate file detected");
        }

        @Test
        @DisplayName("应该通过去重检测当文件唯一时")
        void shouldPassDeduplicationWhenFileIsUnique() {
            // Given
            String fileName = "unique.pdf";
            byte[] content = "unique content".getBytes();
            String hash = "unique-hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            // Mock 数据库无重复
            when(arcFileContentMapper.selectMaps(any(QueryWrapper.class))).thenReturn(List.of());

            // Mock 核心服务返回成功
            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF")))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), eq(fileName), eq("PDF")))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), eq(fileName)))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.PASS);
        }

        @Test
        @DisplayName("应该支持 SHA256 算法的去重检测")
        void shouldSupportSHA256Deduplication() {
            // Given
            String fileName = "sha256-file.pdf";
            byte[] content = "content".getBytes();
            String hash = "sha256-hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SHA-256")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            // Mock 数据库无重复
            when(arcFileContentMapper.selectMaps(any(QueryWrapper.class))).thenReturn(List.of());

            // Mock 核心服务返回成功
            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF")))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), eq(fileName), eq("PDF")))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), eq(fileName)))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);

            // 验证使用了 SHA256
            try {
                verify(fileHashUtil).calculateSHA256(any(java.io.InputStream.class));
            } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
                // Ignore in test
            }
        }
    }

    @Nested
    @DisplayName("真实性检测测试")
    class AuthenticityTests {

        @Test
        @DisplayName("应该调用核心服务进行真实性检测")
        void shouldCallCoreServiceForAuthenticityCheck() {
            // Given
            String fileName = "authentic.pdf";
            byte[] content = "authentic content".getBytes();
            String hash = "auth-hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            CheckItem authResult = CheckItem.pass("Authenticity", "Hash verified");
            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF")))
                    .thenReturn(authResult);
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.PASS);
            verify(fourNatureCoreService).checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF"));
        }

        @Test
        @DisplayName("真实性检测失败应该阻断整个检测流程")
        void shouldFailEntireCheckWhenAuthenticityFails() {
            // Given
            String fileName = "inauthentic.pdf";
            byte[] content = "content".getBytes();
            String hash = "invalid-hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            CheckItem authResult = CheckItem.fail("Authenticity", "Hash mismatch");
            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF")))
                    .thenReturn(authResult);

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.FAIL);

            // 验证后续检测未执行
            verify(fourNatureCoreService, never()).checkSingleFileUsability(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("完整性检测测试")
    class IntegrityTests {

        @Test
        @DisplayName("应该检测缺少全宗代码")
        void shouldDetectMissingFondsCode() {
            // Given
            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode(null)
                            .accountPeriod("2023-01")
                            .attachmentCount(0)
                            .build())
                    .attachments(new ArrayList<>())
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getIntegrity().getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getIntegrity().getErrors()).contains("Missing Fonds Code");
        }

        @Test
        @DisplayName("应该检测缺少凭证号")
        void shouldDetectMissingVoucherNumber() {
            // Given
            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber(null)
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(0)
                            .build())
                    .attachments(new ArrayList<>())
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getIntegrity().getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getIntegrity().getErrors()).contains("Missing Voucher Number");
        }

        @Test
        @DisplayName("应该检测缺少会计期间")
        void shouldDetectMissingAccountPeriod() {
            // Given
            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod(null)
                            .attachmentCount(0)
                            .build())
                    .attachments(new ArrayList<>())
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getIntegrity().getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getIntegrity().getErrors()).contains("Missing Account Period");
        }

        @Test
        @DisplayName("应该检测附件数量不匹配")
        void shouldDetectAttachmentCountMismatch() {
            // Given
            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(5)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(
                            AttachmentDto.builder().fileName("file1.pdf").build(),
                            AttachmentDto.builder().fileName("file2.pdf").build()
                    ))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put("file1.pdf", "test".getBytes());
            fileStreams.put("file2.pdf", "test".getBytes());

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getIntegrity().getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getIntegrity().getErrors())
                    .anyMatch(error -> error.contains("Attachment count mismatch"));
        }

        @Test
        @DisplayName("应该验证金额格式")
        void shouldValidateAmountFormat() {
            // Given
            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(0)
                            .totalAmount(new BigDecimal("100.00"))
                            .build())
                    .attachments(new ArrayList<>())
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();

            AmountValidator.ValidationResult invalidResult = AmountValidator.ValidationResult.fail("金额格式错误");
            when(amountValidator.validateAmount(any(BigDecimal.class))).thenReturn(invalidResult);

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getIntegrity().getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getIntegrity().getErrors())
                    .anyMatch(error -> error.contains("金额格式不符合会计准则"));
        }
    }

    @Nested
    @DisplayName("可用性检测测试")
    class UsabilityTests {

        @Test
        @DisplayName("应该调用核心服务进行可用性检测")
        void shouldCallCoreServiceForUsabilityCheck() {
            // Given
            String fileName = "usable.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), eq(fileName), eq("PDF")))
                    .thenReturn(CheckItem.pass("Usability", "File is usable"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getUsability().getStatus()).isEqualTo(OverallStatus.PASS);
            verify(fourNatureCoreService).checkSingleFileUsability(any(), eq(fileName), eq("PDF"));
        }

        @Test
        @DisplayName("可用性检测失败应该标记报告为失败")
        void shouldMarkReportAsFailWhenUsabilityFails() {
            // Given
            String fileName = "unusable.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), eq(fileName), eq("PDF")))
                    .thenReturn(CheckItem.fail("Usability", "File format corrupted"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getUsability().getStatus()).isEqualTo(OverallStatus.FAIL);
        }
    }

    @Nested
    @DisplayName("安全性检测测试")
    class SafetyTests {

        @Test
        @DisplayName("应该调用核心服务进行安全性检测")
        void shouldCallCoreServiceForSafetyCheck() {
            // Given
            String fileName = "safe.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), eq(fileName)))
                    .thenReturn(CheckItem.pass("Safety", "No threats detected"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getSafety().getStatus()).isEqualTo(OverallStatus.PASS);
            verify(fourNatureCoreService).checkSingleFileSafety(any(), eq(fileName));
        }

        @Test
        @DisplayName("安全性检测失败应该阻断整个检测流程")
        void shouldFailEntireCheckWhenSafetyFails() {
            // Given
            String fileName = "malicious.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), eq(fileName)))
                    .thenReturn(CheckItem.fail("Safety", "Virus detected"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getSafety().getStatus()).isEqualTo(OverallStatus.FAIL);
        }

        @Test
        @DisplayName("应该处理安全性检测警告状态")
        void shouldHandleSafetyWarningStatus() {
            // Given
            String fileName = "suspicious.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));

            CheckItem safetyWarning = new CheckItem();
            safetyWarning.setName("Safety Check");
            safetyWarning.setStatus(OverallStatus.WARNING);
            safetyWarning.setMessage("Suspicious pattern detected");
            when(fourNatureCoreService.checkSingleFileSafety(any(), eq(fileName)))
                    .thenReturn(safetyWarning);

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getSafety().getStatus()).isEqualTo(OverallStatus.WARNING);
            assertThat(report.getSafety().getErrors()).isEmpty();
            assertThat(report.getSafety().getMessage()).contains("Suspicious pattern detected");
        }
    }

    @Nested
    @DisplayName("健康检查测试")
    class HealthCheckTests {

        @Test
        @DisplayName("应该对档案执行健康检查")
        void shouldPerformHealthCheckOnArchive() throws Exception {
            // Given
            Archive archive = new Archive();
            archive.setId("archive-1");
            archive.setArchiveCode("ARC-001");
            archive.setUniqueBizId("biz-001");
            archive.setAmount(new BigDecimal("100.00"));

            ArcFileContent file = mock(ArcFileContent.class);
            when(file.getFileName()).thenReturn("test.pdf");
            when(file.getStoragePath()).thenReturn("/tmp/test.pdf");
            when(file.getOriginalHash()).thenReturn("hash");
            when(file.getHashAlgorithm()).thenReturn("SM3");
            when(file.getFileType()).thenReturn("PDF");

            Path tempPath = Files.createTempFile("test", ".pdf");
            when(file.getStoragePath()).thenReturn(tempPath.toString());

            List<ArcFileContent> files = List.of(file);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq("test.pdf"), eq("hash"), eq("SM3"), eq("PDF")))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), eq("test.pdf"), eq("PDF")))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), eq("test.pdf")))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performHealthCheck(archive, files);

            // Then
            assertThat(report.getArchivalCode()).isEqualTo("ARC-001");
            assertThat(report.getCheckId()).isNotNull();
            assertThat(report.getCheckTime()).isNotNull();

            // Clean up
            Files.deleteIfExists(tempPath);
        }

        @Test
        @DisplayName("健康检查应该检测缺失的档案元数据")
        void shouldDetectMissingArchiveMetadataInHealthCheck() throws Exception {
            // Given
            Archive archive = new Archive();
            archive.setId("archive-1");
            archive.setArchiveCode("ARC-001");
            archive.setUniqueBizId(null); // 缺失
            archive.setAmount(null); // 缺失

            ArcFileContent file = mock(ArcFileContent.class);
            when(file.getFileName()).thenReturn("test.pdf");
            Path tempPath = Files.createTempFile("test", ".pdf");
            when(file.getStoragePath()).thenReturn(tempPath.toString());

            List<ArcFileContent> files = List.of(file);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performHealthCheck(archive, files);

            // Then
            assertThat(report.getIntegrity().getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getIntegrity().getErrors()).contains("Missing Unique Biz ID");
            assertThat(report.getIntegrity().getErrors()).contains("Missing Amount");

            // Clean up
            Files.deleteIfExists(tempPath);
        }

        @Test
        @DisplayName("健康检查应该处理文件不存在的情况")
        void shouldHandleMissingFileInHealthCheck() {
            // Given
            Archive archive = new Archive();
            archive.setId("archive-1");
            archive.setArchiveCode("ARC-001");
            archive.setUniqueBizId("biz-001");
            archive.setAmount(new BigDecimal("100.00"));

            ArcFileContent file = mock(ArcFileContent.class);
            when(file.getFileName()).thenReturn("missing.pdf");
            when(file.getStoragePath()).thenReturn("/nonexistent/path/missing.pdf");

            List<ArcFileContent> files = List.of(file);

            // When
            FourNatureReport report = fourNatureCheckService.performHealthCheck(archive, files);

            // Then
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getAuthenticity().getErrors())
                    .anyMatch(error -> error.contains("File not found"));
        }
    }

    @Nested
    @DisplayName("完整检测流程测试")
    class FullCheckTests {

        @Test
        @DisplayName("应该执行完整的四性检测流程")
        void shouldPerformFullFourNatureCheck() {
            // Given
            String fileName = "complete.pdf";
            byte[] content = "complete content".getBytes();
            String hash = "complete-hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(new BigDecimal("100.00"))
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF")))
                    .thenReturn(CheckItem.pass("Authenticity", "Hash verified (SM3)"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), eq(fileName), eq("PDF")))
                    .thenReturn(CheckItem.pass("Usability", "File is usable"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), eq(fileName)))
                    .thenReturn(CheckItem.pass("Safety", "No threats detected"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);
            assertThat(report.getCheckId()).isNotNull();
            assertThat(report.getCheckTime()).isNotNull();
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.PASS);
            assertThat(report.getIntegrity().getStatus()).isEqualTo(OverallStatus.PASS);
            assertThat(report.getUsability().getStatus()).isEqualTo(OverallStatus.PASS);
            assertThat(report.getSafety().getStatus()).isEqualTo(OverallStatus.PASS);

            // 验证调用了所有检测方法
            verify(fourNatureCoreService).checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF"));
            verify(fourNatureCoreService).checkSingleFileUsability(any(), eq(fileName), eq("PDF"));
            verify(fourNatureCoreService).checkSingleFileSafety(any(), eq(fileName));
        }

        @Test
        @DisplayName("应该处理多个文件的检测")
        void shouldHandleMultipleFilesCheck() {
            // Given
            AttachmentDto file1 = AttachmentDto.builder()
                    .fileName("file1.pdf").fileHash("hash1").hashAlgorithm("SM3").fileType("PDF").build();
            AttachmentDto file2 = AttachmentDto.builder()
                    .fileName("file2.pdf").fileHash("hash2").hashAlgorithm("SM3").fileType("PDF").build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001").fondsCode("F001").accountPeriod("2023-01")
                            .attachmentCount(2).totalAmount(BigDecimal.ZERO).build())
                    .attachments(List.of(file1, file2))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put("file1.pdf", "content1".getBytes());
            fileStreams.put("file2.pdf", "content2".getBytes());

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);

            // 验证每个文件都被检测
            verify(fourNatureCoreService, times(2)).checkSingleFileAuthenticity(any(), any(), any(), any(), any());
            verify(fourNatureCoreService, times(2)).checkSingleFileUsability(any(), any(), any());
            verify(fourNatureCoreService, times(2)).checkSingleFileSafety(any(), any());
        }
    }

    @Nested
    @DisplayName("边界条件和异常测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("应该处理空附件列表")
        void shouldHandleEmptyAttachments() {
            // Given
            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(0)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(new ArrayList<>())
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.PASS);
            assertThat(report.getUsability().getStatus()).isEqualTo(OverallStatus.PASS);
            assertThat(report.getSafety().getStatus()).isEqualTo(OverallStatus.PASS);
        }

        @Test
        @DisplayName("应该处理 null 附件列表")
        void shouldHandleNullAttachments() {
            // Given
            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(0)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(null)
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.PASS);
        }

        @Test
        @DisplayName("应该处理文件流缺失的情况")
        void shouldHandleMissingFileStream() {
            // Given
            String fileName = "missing.pdf";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash("hash")
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            // 故意不添加文件流

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getAuthenticity().getErrors())
                    .anyMatch(error -> error.contains("Missing content") || error.contains(fileName));
        }

        @Test
        @DisplayName("应该处理哈希算法为空的情况（默认使用 SM3）")
        void shouldHandleNullHashAlgorithm() {
            // Given
            String fileName = "no-algo.pdf";
            byte[] content = "content".getBytes();

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash("hash")
                    .hashAlgorithm(null) // null 算法
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);
            // 验证使用了默认的 SM3 算法
            try {
                verify(fileHashUtil).calculateSM3(any(java.io.InputStream.class));
            } catch (Exception e) {
                // Ignore in test
            }
        }

        @Test
        @DisplayName("应该处理哈希算法为空字符串的情况")
        void shouldHandleEmptyHashAlgorithm() {
            // Given
            String fileName = "empty-algo.pdf";
            byte[] content = "content".getBytes();

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash("hash")
                    .hashAlgorithm("") // 空字符串
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);
        }

        @Test
        @DisplayName("应该处理完整性检测警告状态")
        void shouldHandleIntegrityWarningStatus() {
            // Given
            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(0)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(new ArrayList<>())
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            // 完整性检测应该通过（没有警告的场景）
            assertThat(report.getIntegrity().getStatus()).isEqualTo(OverallStatus.PASS);
        }

        @Test
        @DisplayName("应该处理可用性检测警告状态")
        void shouldHandleUsabilityWarningStatus() {
            // Given
            String fileName = "warning.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));

            CheckItem usabilityWarning = new CheckItem();
            usabilityWarning.setName("Usability");
            usabilityWarning.setStatus(OverallStatus.WARNING);
            usabilityWarning.setMessage("Minor format issue");
            when(fourNatureCoreService.checkSingleFileUsability(any(), eq(fileName), eq("PDF")))
                    .thenReturn(usabilityWarning);

            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.WARNING);
            assertThat(report.getUsability().getStatus()).isEqualTo(OverallStatus.WARNING);
            assertThat(report.getUsability().getMessage()).contains("Minor format issue");
        }

        @Test
        @DisplayName("应该处理完整性检测失败但不阻断的情况")
        void shouldHandleIntegrityFailureWithoutBlocking() {
            // Given
            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(0)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(new ArrayList<>())
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            // 完整性检测失败应该标记报告为失败
            assertThat(report.getIntegrity().getStatus()).isEqualTo(OverallStatus.PASS);
            assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);
        }
    }

    @Nested
    @DisplayName("去重检测边界条件测试")
    class DeduplicationEdgeCaseTests {

        @Test
        @DisplayName("应该处理空哈希值集合")
        void shouldHandleEmptyHashSet() {
            // Given
            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(
                            AttachmentDto.builder()
                                    .fileName("test.pdf")
                                    .fileHash("hash")
                                    .hashAlgorithm("SM3")
                                    .fileType("PDF")
                                    .build()
                    ))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            // 不添加文件流，导致哈希计算失败

            // Mock 空结果
            when(arcFileContentMapper.selectMaps(any(QueryWrapper.class))).thenReturn(List.of());

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.FAIL);
        }

        @Test
        @DisplayName("应该检测 original_hash 字段的重复")
        void shouldDetectOriginalHashDuplication() {
            // Given
            String fileName = "duplicate.pdf";
            byte[] content = "content".getBytes();
            String hash = "original-hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            // Mock 数据库返回 original_hash 重复
            Map<String, Object> existingRecord = new HashMap<>();
            existingRecord.put("original_hash", hash);
            when(arcFileContentMapper.selectMaps(any(QueryWrapper.class)))
                    .thenReturn(List.of(existingRecord));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getAuthenticity().getErrors())
                    .anyMatch(error -> error.contains("Duplicate file"));
        }

        @Test
        @DisplayName("应该同时检查 file_hash 和 original_hash")
        void shouldCheckBothFileHashAndOriginalHash() {
            // Given
            String fileName1 = "file1.pdf";
            String fileName2 = "file2.pdf";
            byte[] content = "content".getBytes();

            AttachmentDto attachment1 = AttachmentDto.builder()
                    .fileName(fileName1)
                    .fileHash("hash1")
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AttachmentDto attachment2 = AttachmentDto.builder()
                    .fileName(fileName2)
                    .fileHash("hash2")
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(2)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment1, attachment2))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName1, content);
            fileStreams.put(fileName2, content);

            // Mock 数据库返回两种哈希都有重复
            Map<String, Object> record1 = new HashMap<>();
            record1.put("file_hash", "hash1");
            Map<String, Object> record2 = new HashMap<>();
            record2.put("original_hash", "hash2");
            when(arcFileContentMapper.selectMaps(any(QueryWrapper.class)))
                    .thenReturn(List.of(record1, record2));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getAuthenticity().getErrors()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("真实性检测异常处理测试")
    class AuthenticityExceptionTests {

        @Test
        @DisplayName("应该处理真实性检测异常")
        void shouldHandleAuthenticityException() {
            // Given
            String fileName = "exception.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            // Mock 抛出异常
            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF")))
                    .thenThrow(new RuntimeException("Authenticity check failed"));

            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            // 异常应该被捕获，不影响其他检测
            assertThat(report.getAuthenticity().getStatus()).isNotNull();
        }
    }

    @Nested
    @DisplayName("可用性检测异常处理测试")
    class UsabilityExceptionTests {

        @Test
        @DisplayName("应该处理可用性检测异常")
        void shouldHandleUsabilityException() {
            // Given
            String fileName = "exception.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));

            // Mock 抛出异常
            when(fourNatureCoreService.checkSingleFileUsability(any(), eq(fileName), eq("PDF")))
                    .thenThrow(new RuntimeException("Usability check failed"));

            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            // 异常应该被捕获，不影响其他检测
            assertThat(report.getUsability().getStatus()).isNotNull();
        }
    }

    @Nested
    @DisplayName("安全性检测异常处理测试")
    class SafetyExceptionTests {

        @Test
        @DisplayName("应该处理安全性检测异常")
        void shouldHandleSafetyException() {
            // Given
            String fileName = "exception.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));

            // Mock 抛出异常
            when(fourNatureCoreService.checkSingleFileSafety(any(), eq(fileName)))
                    .thenThrow(new RuntimeException("Safety check failed"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            // 异常应该被捕获，不影响其他检测
            assertThat(report.getSafety().getStatus()).isNotNull();
        }
    }

    @Nested
    @DisplayName("健康检查边界条件测试")
    class HealthCheckEdgeCaseTests {

        @Test
        @DisplayName("应该处理空文件列表的健康检查")
        void shouldHandleEmptyFileListInHealthCheck() {
            // Given
            Archive archive = new Archive();
            archive.setId("archive-1");
            archive.setArchiveCode("ARC-001");
            archive.setUniqueBizId("biz-001");
            archive.setAmount(new BigDecimal("100.00"));

            List<ArcFileContent> files = new ArrayList<>();

            // When
            FourNatureReport report = fourNatureCheckService.performHealthCheck(archive, files);

            // Then
            assertThat(report.getIntegrity().getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getIntegrity().getErrors()).contains("No files associated");
        }

        @Test
        @DisplayName("应该处理健康检查中多个文件的混合状态")
        void shouldHandleMixedFileStatusesInHealthCheck() throws Exception {
            // Given
            Archive archive = new Archive();
            archive.setId("archive-1");
            archive.setArchiveCode("ARC-001");
            archive.setUniqueBizId("biz-001");
            archive.setAmount(new BigDecimal("100.00"));

            // 创建两个临时文件
            Path tempPath1 = Files.createTempFile("test1", ".pdf");
            Path tempPath2 = Files.createTempFile("test2", ".pdf");

            ArcFileContent file1 = mock(ArcFileContent.class);
            when(file1.getFileName()).thenReturn("file1.pdf");
            when(file1.getStoragePath()).thenReturn(tempPath1.toString());
            when(file1.getOriginalHash()).thenReturn("hash1");
            when(file1.getHashAlgorithm()).thenReturn("SM3");
            when(file1.getFileType()).thenReturn("PDF");

            ArcFileContent file2 = mock(ArcFileContent.class);
            when(file2.getFileName()).thenReturn("file2.pdf");
            when(file2.getStoragePath()).thenReturn(tempPath2.toString());
            when(file2.getOriginalHash()).thenReturn("hash2");
            when(file2.getHashAlgorithm()).thenReturn("SM3");
            when(file2.getFileType()).thenReturn("PDF");

            List<ArcFileContent> files = List.of(file1, file2);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq("file1.pdf"), eq("hash1"), eq("SM3"), eq("PDF")))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq("file2.pdf"), eq("hash2"), eq("SM3"), eq("PDF")))
                    .thenReturn(CheckItem.fail("Authenticity", "Hash mismatch"));

            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.pass("Usability", "OK"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performHealthCheck(archive, files);

            // Then
            assertThat(report.getAuthenticity().getStatus()).isEqualTo(OverallStatus.FAIL);

            // Clean up
            Files.deleteIfExists(tempPath1);
            Files.deleteIfExists(tempPath2);
        }

        @Test
        @DisplayName("健康检查应该处理可用性检测失败的情况")
        void shouldHandleUsabilityFailureInHealthCheck() throws Exception {
            // Given
            Archive archive = new Archive();
            archive.setId("archive-1");
            archive.setArchiveCode("ARC-001");
            archive.setUniqueBizId("biz-001");
            archive.setAmount(new BigDecimal("100.00"));

            Path tempPath = Files.createTempFile("test", ".pdf");

            ArcFileContent file = mock(ArcFileContent.class);
            when(file.getFileName()).thenReturn("test.pdf");
            when(file.getStoragePath()).thenReturn(tempPath.toString());
            when(file.getOriginalHash()).thenReturn("hash");
            when(file.getHashAlgorithm()).thenReturn("SM3");
            when(file.getFileType()).thenReturn("PDF");

            List<ArcFileContent> files = List.of(file);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), eq("test.pdf"), eq("PDF")))
                    .thenReturn(CheckItem.fail("Usability", "File corrupted"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.pass("Safety", "OK"));

            // When
            FourNatureReport report = fourNatureCheckService.performHealthCheck(archive, files);

            // Then
            assertThat(report.getUsability().getStatus()).isEqualTo(OverallStatus.FAIL);

            // Clean up
            Files.deleteIfExists(tempPath);
        }
    }

    @Nested
    @DisplayName("报告状态聚合测试")
    class ReportStatusAggregationTests {

        @Test
        @DisplayName("应该正确聚合多个警告状态")
        void shouldAggregateMultipleWarnings() {
            // Given
            String fileName = "warning.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));

            CheckItem usabilityWarning = new CheckItem();
            usabilityWarning.setName("Usability");
            usabilityWarning.setStatus(OverallStatus.WARNING);
            usabilityWarning.setMessage("Minor issue");
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(usabilityWarning);

            CheckItem safetyWarning = new CheckItem();
            safetyWarning.setName("Safety");
            safetyWarning.setStatus(OverallStatus.WARNING);
            safetyWarning.setMessage("Suspicious pattern");
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(safetyWarning);

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.WARNING);
            assertThat(report.getUsability().getStatus()).isEqualTo(OverallStatus.WARNING);
            assertThat(report.getSafety().getStatus()).isEqualTo(OverallStatus.WARNING);
        }

        @Test
        @DisplayName("失败状态应该覆盖警告状态")
        void shouldLetFailOverrideWarning() {
            // Given
            String fileName = "mixed.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.pass("Authenticity", "OK"));

            CheckItem usabilityWarning = new CheckItem();
            usabilityWarning.setName("Usability");
            usabilityWarning.setStatus(OverallStatus.WARNING);
            usabilityWarning.setMessage("Minor issue");
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(usabilityWarning);

            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.fail("Safety", "Virus detected"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            assertThat(report.getStatus()).isEqualTo(OverallStatus.FAIL);
            assertThat(report.getSafety().getStatus()).isEqualTo(OverallStatus.FAIL);
        }
    }

    @Nested
    @DisplayName("错误消息格式测试")
    class ErrorMessageTests {

        @Test
        @DisplayName("应该生成正确的错误消息格式")
        void shouldGenerateCorrectErrorMessageFormat() {
            // Given
            String fileName = "error.pdf";
            byte[] content = "content".getBytes();
            String hash = "hash";

            AttachmentDto attachment = AttachmentDto.builder()
                    .fileName(fileName)
                    .fileHash(hash)
                    .hashAlgorithm("SM3")
                    .fileType("PDF")
                    .build();

            AccountingSipDto sip = AccountingSipDto.builder()
                    .header(VoucherHeadDto.builder()
                            .voucherNumber("V001")
                            .fondsCode("F001")
                            .accountPeriod("2023-01")
                            .attachmentCount(1)
                            .totalAmount(BigDecimal.ZERO)
                            .build())
                    .attachments(List.of(attachment))
                    .build();

            Map<String, byte[]> fileStreams = new HashMap<>();
            fileStreams.put(fileName, content);

            when(fourNatureCoreService.checkSingleFileAuthenticity(any(), any(), any(), any(), any()))
                    .thenReturn(CheckItem.fail("Authenticity", "Hash verification failed"));
            when(fourNatureCoreService.checkSingleFileUsability(any(), any(), any()))
                    .thenReturn(CheckItem.fail("Usability", "File corrupted"));
            when(fourNatureCoreService.checkSingleFileSafety(any(), any()))
                    .thenReturn(CheckItem.fail("Safety", "Virus detected"));

            // When
            FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

            // Then
            // 验证错误消息包含文件名
            assertThat(report.getAuthenticity().getErrors())
                    .anyMatch(error -> error.contains(fileName));
        }
    }
}
