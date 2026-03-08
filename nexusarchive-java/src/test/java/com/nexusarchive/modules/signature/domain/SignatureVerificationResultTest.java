package com.nexusarchive.modules.signature.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Tag("unit")
class SignatureVerificationResultTest {

    @Test
    void should_derive_signature_counters_from_details() {
        SignatureVerificationResult result = SignatureVerificationResult.builder()
                .status(SignatureVerificationStatus.FAILED)
                .signatureDetails(List.of(
                        SignatureVerificationSignature.valid("Alice"),
                        SignatureVerificationSignature.invalid("Bob", "digest mismatch")))
                .build();

        assertEquals(2, result.getSignatureCount());
        assertEquals(1, result.getValidSignatureCount());
        assertEquals(1, result.getInvalidSignatureCount());
        assertEquals(0, result.getUnknownSignatureCount());
    }

    @Test
    void aggregate_should_copy_result_summary_for_persistence() {
        LocalDateTime verifiedAt = LocalDateTime.of(2026, 3, 6, 21, 30);
        SignatureVerificationResult result = SignatureVerificationResult.builder()
                .status(SignatureVerificationStatus.PASSED)
                .providerCode("pdfbox-bc")
                .providerVersion("1.0.0")
                .verifiedAt(verifiedAt)
                .signatureDetails(List.of(SignatureVerificationSignature.valid("Alice")))
                .build();

        ArchiveSignatureVerification verification = ArchiveSignatureVerification.create(
                "archive-1",
                "file-1",
                "invoice.pdf",
                SignatureDocumentType.PDF,
                null,
                result);

        assertEquals("archive-1", verification.getArchiveId());
        assertEquals("file-1", verification.getFileId());
        assertEquals("invoice.pdf", verification.getFileName());
        assertEquals(SignatureDocumentType.PDF, verification.getDocumentType());
        assertEquals(ArchiveSignatureVerification.DEFAULT_TRIGGER_SOURCE, verification.getTriggerSource());
        assertEquals(SignatureVerificationStatus.PASSED, verification.getVerificationStatus());
        assertEquals(1, verification.getSignatureCount());
        assertEquals(1, verification.getValidSignatureCount());
        assertEquals(0, verification.getInvalidSignatureCount());
        assertEquals("pdfbox-bc", verification.getProviderCode());
        assertEquals("1.0.0", verification.getProviderVersion());
        assertEquals(verifiedAt, verification.getVerifiedAt());
        assertSame(result, verification.getResult());
    }

    @Test
    void aggregate_should_require_archive_id() {
        SignatureVerificationResult result = SignatureVerificationResult.builder()
                .status(SignatureVerificationStatus.ERROR)
                .build();

        IllegalArgumentException error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ArchiveSignatureVerification.create(
                        " ",
                        "file-1",
                        "invoice.pdf",
                        SignatureDocumentType.PDF,
                        "MANUAL",
                        result));

        assertEquals("archiveId must not be blank", error.getMessage());
    }
}
