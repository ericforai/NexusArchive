// Input: FourNaturesService
// Output: Test Results
// Pos: NexusCore preservation/test
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation;

import com.nexusarchive.core.compliance.DigitalSignatureVerifier;
import com.nexusarchive.core.compliance.FileHashService;
import com.nexusarchive.core.compliance.HashAlgorithm;
import com.nexusarchive.core.compliance.SignatureVerifyResult;
import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.preservation.impl.AuthenticityCheck;
import com.nexusarchive.core.preservation.impl.DefaultFourNaturesService;
import com.nexusarchive.core.preservation.impl.IntegrityCheck;
import com.nexusarchive.core.preservation.impl.SecurityCheck;
import com.nexusarchive.core.preservation.impl.UsabilityCheck;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FourNaturesServiceTests {

    private DefaultFourNaturesService service;
    private FileHashService fileHashService;
    private DigitalSignatureVerifier signatureVerifier;
    private Path tempFile;

    @BeforeEach
    void setUp() throws Exception {
        fileHashService = mock(FileHashService.class);
        signatureVerifier = mock(DigitalSignatureVerifier.class);

        IntegrityCheck integrityCheck = new IntegrityCheck(fileHashService);
        AuthenticityCheck authenticityCheck = new AuthenticityCheck(signatureVerifier);
        UsabilityCheck usabilityCheck = mock(UsabilityCheck.class);
        SecurityCheck securityCheck = mock(SecurityCheck.class);

        when(usabilityCheck.check(any(), any())).thenReturn(CheckResult.pass("USABILITY", "OK"));
        when(usabilityCheck.getName()).thenReturn("USABILITY");
        when(securityCheck.check(any(), any())).thenReturn(CheckResult.pass("SECURITY", "OK"));
        when(securityCheck.getName()).thenReturn("SECURITY");

        service = new DefaultFourNaturesService(List.of(
                integrityCheck, authenticityCheck, usabilityCheck, securityCheck
        ));

        tempFile = Files.createTempFile("test-eegs", ".pdf");
        Files.writeString(tempFile, "%PDF-1.4 header simulation");
    }

    @Test
    void testAllChecksPass() throws Exception {
        FileContent fc = new FileContent();
        fc.setFileType("PDF");
        fc.setFileHash("mock-hash");
        fc.setHashAlgorithm("SM3");

        when(fileHashService.hashFile(eq(tempFile), eq(HashAlgorithm.SM3))).thenReturn("mock-hash");
        when(signatureVerifier.verify(any())).thenReturn(
                SignatureVerifyResult.successWithTimestamp("RSA", "User", null, "123", true, true, "OK"));

        List<CheckResult> results = service.validate(fc, tempFile);

        Assertions.assertEquals(4, results.size());
        Assertions.assertTrue(results.stream().allMatch(CheckResult::isPassed));
    }

    @Test
    void testIntegrityFail() throws Exception {
        FileContent fc = new FileContent();
        fc.setFileType("PDF");
        fc.setFileHash("expected-hash");
        fc.setHashAlgorithm("SM3");

        when(fileHashService.hashFile(eq(tempFile), eq(HashAlgorithm.SM3))).thenReturn("wrong-hash");
        when(signatureVerifier.verify(any())).thenReturn(
                SignatureVerifyResult.successWithTimestamp("RSA", "User", null, "123", true, true, "OK"));

        List<CheckResult> results = service.validate(fc, tempFile);
        
        CheckResult integrity = results.stream()
                .filter(r -> "INTEGRITY".equals(r.getCheckName()))
                .findFirst()
                .orElseThrow();
        Assertions.assertFalse(integrity.isPassed());
        Assertions.assertTrue(integrity.getMessage().contains("哈希不匹配"));
    }
}
