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
            String hash = "duplicate-hash";

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
}
