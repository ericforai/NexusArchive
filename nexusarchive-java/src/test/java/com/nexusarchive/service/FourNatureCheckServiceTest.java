// Input: MyBatis-Plus、org.junit、org.mockito、Java 标准库、等
// Output: FourNatureCheckServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.impl.FourNatureCheckServiceImpl;
import com.nexusarchive.util.FileHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FourNatureCheckServiceTest {

        @Mock
        private FourNatureCoreService fourNatureCoreService;
        @Mock
        private ArcFileContentMapper arcFileContentMapper;
        @Mock
        private com.nexusarchive.util.AmountValidator amountValidator;
        @Mock
        private FileHashUtil fileHashUtil;

        private FourNatureCheckServiceImpl fourNatureCheckService;

        @BeforeEach
        void setUp() {
                fourNatureCheckService = new FourNatureCheckServiceImpl(
                                fourNatureCoreService, arcFileContentMapper, amountValidator, fileHashUtil);

                org.mockito.Mockito.lenient().when(amountValidator.validateAmount(any()))
                                .thenReturn(com.nexusarchive.util.AmountValidator.ValidationResult.success());
        }

        @Test
        void testDeduplication_DuplicateFound() throws Exception {
                // Arrange
                String fileName = "test.pdf";
                byte[] content = "test content".getBytes();
                String hash = "dummy_hash";

                AttachmentDto attachment = AttachmentDto.builder()
                                .fileName(fileName)
                                .fileHash(hash)
                                .hashAlgorithm("SM3")
                                .fileType("PDF")
                                .build();

                AccountingSipDto sip = AccountingSipDto.builder()
                                .header(VoucherHeadDto.builder().voucherNumber("V001").build())
                                .attachments(List.of(attachment))
                                .build();

                Map<String, byte[]> fileStreams = new HashMap<>();
                fileStreams.put(fileName, content);

                when(fileHashUtil.calculateSM3(any(ByteArrayInputStream.class))).thenReturn(hash);
                when(arcFileContentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

                // Act
                FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

                // Assert
                assertEquals(OverallStatus.FAIL, report.getStatus());
                assertEquals(OverallStatus.FAIL, report.getAuthenticity().getStatus());
                assert (report.getAuthenticity().getErrors().get(0).contains("Duplicate file detected"));
        }

        @Test
        void testAuthenticity_DelegationCheck() throws Exception {
                // Arrange
                String fileName = "test.pdf";
                byte[] content = "test content".getBytes();
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

                when(fileHashUtil.calculateSM3(any(ByteArrayInputStream.class))).thenReturn(hash);
                when(arcFileContentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

                // Mock Core Service to return success
                when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF")))
                                .thenReturn(CheckItem.pass("Authenticity", "OK"));
                when(fourNatureCoreService.checkSingleFileUsability(any(), eq(fileName), eq("PDF")))
                                .thenReturn(CheckItem.pass("Usability", "OK"));
                when(fourNatureCoreService.checkSingleFileSafety(any(), eq(fileName)))
                                .thenReturn(CheckItem.pass("Safety", "OK"));

                // Act
                FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

                // Assert
                assertEquals(OverallStatus.PASS, report.getStatus());
                assertEquals(OverallStatus.PASS, report.getAuthenticity().getStatus());
        }

        @Test
        void testIntegrity_MissingMetadata() throws Exception {
                // Arrange
                AccountingSipDto sip = AccountingSipDto.builder()
                                .header(VoucherHeadDto.builder().voucherNumber("V001").attachmentCount(0).build())
                                .attachments(Collections.emptyList())
                                .build();
                Map<String, byte[]> fileStreams = new HashMap<>();

                // Act
                FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

                // Assert
                assertEquals(OverallStatus.FAIL, report.getStatus());
                assertEquals(OverallStatus.FAIL, report.getIntegrity().getStatus());
        }

        @Test
        void testAuthenticity_CoreServiceCalledForPdfFile() throws Exception {
                // Arrange - 验证 PDF 文件会触发真实性检测（包含签章校验）
                String fileName = "signed_invoice.pdf";
                byte[] content = "PDF content".getBytes();
                String hash = "pdf_hash";

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

                when(fileHashUtil.calculateSM3(any(ByteArrayInputStream.class))).thenReturn(hash);
                when(arcFileContentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

                // Mock Core Service 返回包含签章校验信息的结果
                CheckItem authResult = CheckItem.pass("Authenticity", "Hash verified (SM3); PDF签章校验通过");
                when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF")))
                                .thenReturn(authResult);
                when(fourNatureCoreService.checkSingleFileUsability(any(), eq(fileName), eq("PDF")))
                                .thenReturn(CheckItem.pass("Usability", "OK"));
                when(fourNatureCoreService.checkSingleFileSafety(any(), eq(fileName)))
                                .thenReturn(CheckItem.pass("Safety", "OK"));

                // Act
                FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

                // Assert
                assertEquals(OverallStatus.PASS, report.getStatus());
                // 验证消息中包含签章校验相关内容
                assert (report.getAuthenticity().getMessage().contains("签章") ||
                                report.getAuthenticity().getMessage().contains("Hash"));
        }

        @Test
        void testAuthenticity_NoSignatureAdapter_ReturnsWarning() throws Exception {
                // Arrange - 无签章服务时应返回 WARNING 而非 FAIL
                String fileName = "document.pdf";
                byte[] content = "PDF content".getBytes();
                String hash = "hash123";

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

                when(fileHashUtil.calculateSM3(any(ByteArrayInputStream.class))).thenReturn(hash);
                when(arcFileContentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

                // Mock Core Service 返回 WARNING（模拟签章服务不可用的场景）
                CheckItem authResult = CheckItem.pass("Authenticity", "Hash verified (SM3); 签章服务不可用，跳过校验");
                authResult.setStatus(OverallStatus.WARNING);
                when(fourNatureCoreService.checkSingleFileAuthenticity(any(), eq(fileName), eq(hash), any(), eq("PDF")))
                                .thenReturn(authResult);
                when(fourNatureCoreService.checkSingleFileUsability(any(), eq(fileName), eq("PDF")))
                                .thenReturn(CheckItem.pass("Usability", "OK"));
                when(fourNatureCoreService.checkSingleFileSafety(any(), eq(fileName)))
                                .thenReturn(CheckItem.pass("Safety", "OK"));

                // Act
                FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

                // Assert - WARNING 不应阻断流程
                assertEquals(OverallStatus.WARNING, report.getAuthenticity().getStatus());
                // 整体流程应该继续（WARNING 不是 FAIL）
        }
}
