package com.nexusarchive.modules.signature.infra;

import com.nexusarchive.modules.signature.domain.ArchiveSignatureVerification;
import com.nexusarchive.modules.signature.domain.SignatureDocumentType;
import com.nexusarchive.modules.signature.domain.SignatureVerificationResult;
import com.nexusarchive.modules.signature.domain.SignatureVerificationSignature;
import com.nexusarchive.modules.signature.domain.SignatureVerificationStatus;
import com.nexusarchive.modules.signature.infra.mapper.SignatureVerificationRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class SignatureVerificationRepositoryTest {

    @Mock
    private SignatureVerificationRecordMapper mapper;

    private MybatisArchiveSignatureVerificationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MybatisArchiveSignatureVerificationRepository(mapper);
    }

    @Test
    void save_should_serialize_summary_fields_to_mapper_entity() {
        SignatureVerificationResult result = SignatureVerificationResult.builder()
                .status(SignatureVerificationStatus.PASSED)
                .providerCode("pdfbox-bc")
                .providerVersion("1.0.0")
                .signatureDetails(List.of(SignatureVerificationSignature.valid("Alice")))
                .build();
        ArchiveSignatureVerification verification = ArchiveSignatureVerification.create(
                "archive-1",
                "file-1",
                "invoice.pdf",
                SignatureDocumentType.PDF,
                null,
                result);

        ArchiveSignatureVerification saved = repository.save(verification);

        ArgumentCaptor<SignatureVerificationRecordEntity> captor =
                ArgumentCaptor.forClass(SignatureVerificationRecordEntity.class);
        verify(mapper).insert(captor.capture());
        SignatureVerificationRecordEntity entity = captor.getValue();

        assertEquals("archive-1", entity.getArchiveId());
        assertEquals(ArchiveSignatureVerification.DEFAULT_TRIGGER_SOURCE, entity.getTriggerSource());
        assertEquals(SignatureVerificationStatus.PASSED.name(), entity.getVerificationStatus());
        assertEquals(1, entity.getSignatureCount());
        assertEquals(1, entity.getValidSignatureCount());
        assertEquals(0, entity.getInvalidSignatureCount());
        assertNotNull(entity.getVerifiedAt());
        assertTrue(entity.getResultPayload().contains("\"signerName\":\"Alice\""));

        assertEquals(entity.getVerifiedAt(), saved.getVerifiedAt());
        assertEquals("Alice", saved.getResult().getSignatureDetails().get(0).getSignerName());
    }

    @Test
    void find_by_archive_id_should_deserialize_result_payload() {
        LocalDateTime verifiedAt = LocalDateTime.of(2026, 3, 6, 22, 5);
        SignatureVerificationRecordEntity entity = SignatureVerificationRecordEntity.builder()
                .id("record-1")
                .archiveId("archive-1")
                .fileId("file-1")
                .fileName("invoice.pdf")
                .documentType(SignatureDocumentType.PDF.name())
                .triggerSource("MANUAL")
                .providerCode("pdfbox-bc")
                .providerVersion("1.0.0")
                .verificationStatus(SignatureVerificationStatus.FAILED.name())
                .signatureCount(2)
                .validSignatureCount(1)
                .invalidSignatureCount(1)
                .verifiedAt(verifiedAt)
                .resultPayload("""
                        {"status":"FAILED","providerCode":"pdfbox-bc","providerVersion":"1.0.0","verifiedAt":"%s","signatureDetails":[{"status":"VALID","signerName":"Alice"},{"status":"INVALID","signerName":"Bob","message":"digest mismatch"}]}
                        """.formatted(verifiedAt))
                .build();
        when(mapper.findByArchiveId("archive-1")).thenReturn(List.of(entity));

        List<ArchiveSignatureVerification> records = repository.findByArchiveId("archive-1");

        assertEquals(1, records.size());
        ArchiveSignatureVerification record = records.get(0);
        assertEquals(SignatureVerificationStatus.FAILED, record.getVerificationStatus());
        assertEquals("MANUAL", record.getTriggerSource());
        assertEquals(2, record.getResult().getSignatureCount());
        assertEquals(1, record.getResult().getValidSignatureCount());
        assertEquals(1, record.getResult().getInvalidSignatureCount());
        assertEquals("Bob", record.getResult().getSignatureDetails().get(1).getSignerName());
        assertEquals("digest mismatch", record.getResult().getSignatureDetails().get(1).getMessage());
    }

    @Test
    void save_should_escape_special_characters_in_result_payload() {
        SignatureVerificationResult result = SignatureVerificationResult.builder()
                .status(SignatureVerificationStatus.FAILED)
                .providerCode("pdfbox-bc")
                .providerVersion("1.0.0")
                .verifiedAt(LocalDateTime.of(2026, 3, 6, 23, 0))
                .signatureDetails(List.of(SignatureVerificationSignature.invalid(
                        "Alice",
                        "digest \"mismatch\"\nline2")))
                .build();
        ArchiveSignatureVerification verification = ArchiveSignatureVerification.create(
                "archive-1",
                "file-1",
                "invoice.pdf",
                SignatureDocumentType.PDF,
                "MANUAL",
                result);

        ArchiveSignatureVerification saved = repository.save(verification);

        assertEquals("digest \"mismatch\"\nline2",
                saved.getResult().getSignatureDetails().get(0).getMessage());
    }
}
