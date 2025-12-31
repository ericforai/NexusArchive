// Input: JUnit + 四性检测服务（含签名/完整性/病毒）
// Output: FourNatureCheckService 覆盖测试
// Pos: NexusCore tests/compliance
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FourNatureCheckServiceTests {
    private final FileHashService fileHashService = new FileHashService();
    private final MagicNumberValidator magicNumberValidator = new MagicNumberValidator();

    @TempDir
    Path tempDir;

    @Test
    void shouldPassWhenHashAndMagicMatch() throws IOException {
        Path file = writeSamplePdf("sample-pass.pdf");
        Path xml = writeSampleXml("sample-pass.xml");
        String expectedHash = fileHashService.hashFile(file, HashAlgorithm.SM3);
        FourNatureCheckService service = createService(
                IntegrityCheckResult.success(),
                SignatureVerifyResult.success("SM2", "tester", null, "CERT-1"),
                VirusScanResult.clean(5));

        FourNatureCheckRequest request = FourNatureCheckRequest.builder()
                .expectedHash(expectedHash)
                .hashAlgorithm(HashAlgorithm.SM3)
                .expectedExtension("pdf")
                .metadataXmlPath(xml)
                .build();

        FourNatureCheckResult result = service.check(file, request);

        assertTrue(result.isAllPassed());
        assertTrue(result.isAuthenticityPassed());
        assertTrue(result.isIntegrityPassed());
        assertTrue(result.isUsabilityPassed());
        assertTrue(result.isSafetyPassed());
        assertEquals(expectedHash, result.getComputedHash());
    }

    @Test
    void shouldFailAuthenticityWhenHashMismatch() throws IOException {
        Path file = writeSamplePdf("sample-hash-mismatch.pdf");
        Path xml = writeSampleXml("sample-hash-mismatch.xml");
        String expectedHash = "deadbeef";
        FourNatureCheckService service = createService(
                IntegrityCheckResult.success(),
                SignatureVerifyResult.success("SM2", "tester", null, "CERT-1"),
                VirusScanResult.clean(5));

        FourNatureCheckRequest request = FourNatureCheckRequest.builder()
                .expectedHash(expectedHash)
                .hashAlgorithm(HashAlgorithm.SM3)
                .expectedExtension("pdf")
                .metadataXmlPath(xml)
                .build();

        FourNatureCheckResult result = service.check(file, request);

        assertFalse(result.isAuthenticityPassed());
        assertFalse(result.isAllPassed());
    }

    @Test
    void shouldFailUsabilityWhenExtensionMismatch() throws IOException {
        Path file = writeSamplePdf("sample-extension-mismatch.pdf");
        Path xml = writeSampleXml("sample-extension-mismatch.xml");
        String expectedHash = fileHashService.hashFile(file, HashAlgorithm.SM3);
        FourNatureCheckService service = createService(
                IntegrityCheckResult.success(),
                SignatureVerifyResult.success("SM2", "tester", null, "CERT-1"),
                VirusScanResult.clean(5));

        FourNatureCheckRequest request = FourNatureCheckRequest.builder()
                .expectedHash(expectedHash)
                .hashAlgorithm(HashAlgorithm.SM3)
                .expectedExtension("xml")
                .metadataXmlPath(xml)
                .build();

        FourNatureCheckResult result = service.check(file, request);

        assertFalse(result.isUsabilityPassed());
        assertFalse(result.isAllPassed());
    }

    @Test
    void shouldFailIntegrityWhenXmlMissing() throws IOException {
        Path file = writeSamplePdf("sample-missing-hash.pdf");
        FourNatureCheckService service = createService(
                IntegrityCheckResult.success(),
                SignatureVerifyResult.success("SM2", "tester", null, "CERT-1"),
                VirusScanResult.clean(5));

        FourNatureCheckRequest request = FourNatureCheckRequest.builder()
                .expectedHash(fileHashService.hashFile(file, HashAlgorithm.SM3))
                .hashAlgorithm(HashAlgorithm.SM3)
                .expectedExtension("pdf")
                .metadataXmlPath(null)
                .build();

        FourNatureCheckResult result = service.check(file, request);

        assertFalse(result.isIntegrityPassed());
        assertFalse(result.isAllPassed());
    }

    @Test
    void shouldFailAuthenticityWhenSignatureInvalid() throws IOException {
        Path file = writeSamplePdf("sample-signature-fail.pdf");
        Path xml = writeSampleXml("sample-signature-fail.xml");
        String expectedHash = fileHashService.hashFile(file, HashAlgorithm.SM3);
        FourNatureCheckService service = createService(
                IntegrityCheckResult.success(),
                SignatureVerifyResult.failure("签名无效"),
                VirusScanResult.clean(5));

        FourNatureCheckRequest request = FourNatureCheckRequest.builder()
                .expectedHash(expectedHash)
                .hashAlgorithm(HashAlgorithm.SM3)
                .expectedExtension("pdf")
                .metadataXmlPath(xml)
                .build();

        FourNatureCheckResult result = service.check(file, request);

        assertFalse(result.isAuthenticityPassed());
        assertFalse(result.isAllPassed());
        assertEquals("签名无效", result.getAuthenticityMessage());
    }

    @Test
    void shouldFailSafetyWhenVirusDetected() throws IOException {
        Path file = writeSamplePdf("sample-virus.pdf");
        Path xml = writeSampleXml("sample-virus.xml");
        String expectedHash = fileHashService.hashFile(file, HashAlgorithm.SM3);
        FourNatureCheckService service = createService(
                IntegrityCheckResult.success(),
                SignatureVerifyResult.success("SM2", "tester", null, "CERT-1"),
                VirusScanResult.infected("Eicar-Test-Signature", 5));

        FourNatureCheckRequest request = FourNatureCheckRequest.builder()
                .expectedHash(expectedHash)
                .hashAlgorithm(HashAlgorithm.SM3)
                .expectedExtension("pdf")
                .metadataXmlPath(xml)
                .build();

        FourNatureCheckResult result = service.check(file, request);

        assertFalse(result.isSafetyPassed());
        assertFalse(result.isAllPassed());
    }

    private Path writeSamplePdf(String filename) throws IOException {
        byte[] header = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        byte[] payload = "sample-content".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(header);
        output.write(payload);
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, output.toByteArray());
        return filePath;
    }

    private Path writeSampleXml(String filename) throws IOException {
        String xml = "<Invoice><InvoiceNo>001</InvoiceNo></Invoice>";
        Path filePath = tempDir.resolve(filename);
        Files.writeString(filePath, xml, StandardCharsets.UTF_8);
        return filePath;
    }

    private FourNatureCheckService createService(IntegrityCheckResult integrityResult,
                                                 SignatureVerifyResult signatureResult,
                                                 VirusScanResult virusScanResult) {
        return new FourNatureCheckService(
                fileHashService,
                magicNumberValidator,
                new StubIntegrityChecker(integrityResult),
                new StubSignatureVerifier(signatureResult),
                new StubVirusScanService(virusScanResult));
    }

    private static final class StubIntegrityChecker implements IntegrityChecker {
        private final IntegrityCheckResult result;

        private StubIntegrityChecker(IntegrityCheckResult result) {
            this.result = result;
        }

        @Override
        public IntegrityCheckResult verify(Path xmlPath, Path formatPath) {
            return result;
        }
    }

    private static final class StubSignatureVerifier implements DigitalSignatureVerifier {
        private final SignatureVerifyResult result;

        private StubSignatureVerifier(SignatureVerifyResult result) {
            this.result = result;
        }

        @Override
        public SignatureVerifyResult verify(Path filePath) {
            return result;
        }
    }

    private static final class StubVirusScanService implements VirusScanService {
        private final VirusScanResult result;

        private StubVirusScanService(VirusScanResult result) {
            this.result = result;
        }

        @Override
        public VirusScanResult scan(Path filePath) {
            return result;
        }

        @Override
        public VirusScanResult scan(java.io.InputStream inputStream, String fileName) {
            return result;
        }
    }
}
