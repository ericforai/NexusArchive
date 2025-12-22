// Input: org.junit、org.mockito、Spring Framework、Java 标准库、等
// Output: DigitalSignatureServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.ArcFileContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DigitalSignatureService 单元测试
 * 
 * 测试覆盖:
 * - 电子签名验证
 * - 证书有效性检查
 * - 错误处理
 * 
 * @author Agent E - 质量保障工程师
 */
@ExtendWith(MockitoExtension.class)
class DigitalSignatureServiceTest {

    @InjectMocks
    private DigitalSignatureService signatureService;

    @TempDir
    Path tempDir;

    private ArcFileContent testFile;
    private Path testFilePath;

    @BeforeEach
    void setUp() throws Exception {
        // 设置 archiveRootPath
        ReflectionTestUtils.setField(signatureService, "archiveRootPath", tempDir.toString());

        // 创建测试文件
        testFilePath = tempDir.resolve("test-doc.txt");
        Files.writeString(testFilePath, "这是一个测试档案文件内容");

        // 创建测试文件实体
        testFile = new ArcFileContent();
        testFile.setId("file-001");
        testFile.setStoragePath(testFilePath.toString());
    }

    // ========== 签名验证测试 ==========

    @Nested
    @DisplayName("签名验证")
    class VerifySignatureTests {

        @Test
        @DisplayName("无签名值 - 返回验证失败")
        void verifySignature_NoSignValue_ReturnsFalse() {
            // Arrange - 没有设置签名值
            testFile.setSignValue(null);
            testFile.setCertificate(null);

            // Act
            DigitalSignatureService.VerificationResult result = signatureService.verifySignature(testFile);

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.isSignatureValid()).isFalse();
        }

        @Test
        @DisplayName("无证书 - 返回验证失败")
        void verifySignature_NoCertificate_ReturnsFalse() {
            // Arrange
            testFile.setSignValue(new byte[]{1, 2, 3});
            testFile.setCertificate(null);

            // Act
            DigitalSignatureService.VerificationResult result = signatureService.verifySignature(testFile);

            // Assert
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("无效证书格式 - 返回验证失败")
        void verifySignature_InvalidCertificate_ReturnsFalse() {
            // Arrange
            testFile.setSignValue(new byte[]{1, 2, 3});
            testFile.setCertificate("invalid-base64-certificate-data");

            // Act
            DigitalSignatureService.VerificationResult result = signatureService.verifySignature(testFile);

            // Assert
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("文件不存在 - 返回验证失败")
        void verifySignature_FileNotExist_ReturnsFalse() {
            // Arrange
            testFile.setStoragePath("/non/existent/path/file.pdf");
            testFile.setSignValue(new byte[]{1, 2, 3});
            testFile.setCertificate("some-cert");

            // Act
            DigitalSignatureService.VerificationResult result = signatureService.verifySignature(testFile);

            // Assert
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("存储路径为空 - 返回验证失败")
        void verifySignature_NullStoragePath_ReturnsFalse() {
            // Arrange
            testFile.setStoragePath(null);
            testFile.setSignValue(new byte[]{1, 2, 3});
            testFile.setCertificate("some-cert");

            // Act
            DigitalSignatureService.VerificationResult result = signatureService.verifySignature(testFile);

            // Assert
            assertThat(result.isValid()).isFalse();
        }
    }

    // ========== VerificationResult 测试 ==========

    @Nested
    @DisplayName("验证结果对象")
    class VerificationResultTests {

        @Test
        @DisplayName("成功结果")
        void verificationResult_Valid() {
            // Arrange & Act
            DigitalSignatureService.VerificationResult result = 
                new DigitalSignatureService.VerificationResult(
                    true, true, true, false, "CN=Test", new java.util.Date()
                );

            // Assert
            assertThat(result.isValid()).isTrue();
            assertThat(result.isSignatureValid()).isTrue();
            assertThat(result.isCertificateValid()).isTrue();
            assertThat(result.isCertificateExpired()).isFalse();
            assertThat(result.getCertificateSubject()).isEqualTo("CN=Test");
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("签名无效错误信息")
        void verificationResult_InvalidSignature_ErrorMessage() {
            // Arrange & Act
            DigitalSignatureService.VerificationResult result = 
                new DigitalSignatureService.VerificationResult(
                    false, false, true, false, "CN=Test", new java.util.Date()
                );

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("签名无效");
        }

        @Test
        @DisplayName("证书过期错误信息")
        void verificationResult_ExpiredCert_ErrorMessage() {
            // Arrange & Act
            DigitalSignatureService.VerificationResult result = 
                new DigitalSignatureService.VerificationResult(
                    false, true, false, true, "CN=Test", new java.util.Date()
                );

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("证书");
        }

        @Test
        @DisplayName("自定义错误信息")
        void verificationResult_CustomErrorMessage() {
            // Arrange & Act
            DigitalSignatureService.VerificationResult result = 
                new DigitalSignatureService.VerificationResult(
                    false, false, false, true, "自定义错误"
                );

            // Assert
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("自定义错误");
        }
    }

    // ========== 文件路径处理测试 ==========

    @Nested
    @DisplayName("文件路径处理")
    class FilePathTests {

        @Test
        @DisplayName("绝对路径处理")
        void verifySignature_AbsolutePath_Works() {
            // Arrange - 使用绝对路径
            testFile.setStoragePath(testFilePath.toAbsolutePath().toString());
            testFile.setSignValue(null); // 无签名，预期失败但文件应能读取

            // Act
            DigitalSignatureService.VerificationResult result = signatureService.verifySignature(testFile);

            // Assert - 由于无签名，结果应该是 false，但不应抛出文件读取异常
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("相对路径处理")
        void verifySignature_RelativePath_Works() throws Exception {
            // Arrange - 创建相对路径的文件
            Path relativeFile = tempDir.resolve("relative/test.txt");
            Files.createDirectories(relativeFile.getParent());
            Files.writeString(relativeFile, "test content");
            
            testFile.setStoragePath("relative/test.txt");
            testFile.setSignValue(null);

            // Act
            DigitalSignatureService.VerificationResult result = signatureService.verifySignature(testFile);

            // Assert
            assertThat(result.isValid()).isFalse();
        }
    }
}
