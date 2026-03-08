package com.nexusarchive.modules.signature.app;

import com.nexusarchive.modules.signature.domain.ArchiveSignatureVerification;
import com.nexusarchive.modules.signature.domain.ArchiveSignatureVerificationRepository;
import com.nexusarchive.modules.signature.domain.SignatureDocumentType;
import com.nexusarchive.modules.signature.domain.SignatureVerificationResult;
import com.nexusarchive.modules.signature.domain.SignatureVerificationSignature;
import com.nexusarchive.modules.signature.domain.SignatureVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class SignatureVerificationRecordServiceTest {

    @Mock
    private ArchiveSignatureVerificationRepository repository;

    private SignatureVerificationRecordService service;

    @BeforeEach
    void setUp() {
        service = new SignatureVerificationRecordService(repository);
    }

    @Test
    void save_should_require_archive_id() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.save(ArchiveSignatureVerification.builder().build()));

        assertEquals("archiveId must not be blank", error.getMessage());
    }

    @Test
    void save_should_delegate_to_repository() {
        ArchiveSignatureVerification verification = sampleVerification();
        when(repository.save(verification)).thenReturn(verification);

        ArchiveSignatureVerification saved = service.save(verification);

        assertSame(verification, saved);
        verify(repository).save(verification);
    }

    @Test
    void find_by_archive_id_should_delegate_to_repository() {
        List<ArchiveSignatureVerification> expected = List.of(sampleVerification());
        when(repository.findByArchiveId("archive-1")).thenReturn(expected);

        List<ArchiveSignatureVerification> actual = service.findByArchiveId("archive-1");

        assertSame(expected, actual);
        verify(repository).findByArchiveId("archive-1");
    }

    @Test
    void save_should_require_trigger_source() {
        SignatureVerificationResult result = SignatureVerificationResult.builder()
                .status(SignatureVerificationStatus.ERROR)
                .build();
        ArchiveSignatureVerification verification = ArchiveSignatureVerification.builder()
                .archiveId("archive-1")
                .documentType(SignatureDocumentType.PDF)
                .result(result)
                .build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.save(verification));

        assertEquals("triggerSource must not be blank", error.getMessage());
    }

    @Test
    void save_should_require_result_status() {
        SignatureVerificationResult result = SignatureVerificationResult.builder()
                .providerCode("pdfbox-bc")
                .build();
        ArchiveSignatureVerification verification = ArchiveSignatureVerification.builder()
                .archiveId("archive-1")
                .documentType(SignatureDocumentType.PDF)
                .triggerSource("MANUAL")
                .result(result)
                .build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.save(verification));

        assertEquals("result.status must not be null", error.getMessage());
    }

    private ArchiveSignatureVerification sampleVerification() {
        SignatureVerificationResult result = SignatureVerificationResult.builder()
                .status(SignatureVerificationStatus.PASSED)
                .providerCode("pdfbox-bc")
                .providerVersion("1.0.0")
                .verifiedAt(LocalDateTime.of(2026, 3, 6, 22, 0))
                .signatureDetails(List.of(SignatureVerificationSignature.valid("Alice")))
                .build();

        return ArchiveSignatureVerification.create(
                "archive-1",
                "file-1",
                "invoice.pdf",
                SignatureDocumentType.PDF,
                "MANUAL",
                result);
    }
}
