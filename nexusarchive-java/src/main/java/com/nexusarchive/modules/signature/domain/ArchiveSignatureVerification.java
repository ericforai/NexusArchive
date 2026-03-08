package com.nexusarchive.modules.signature.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Objects;

@Value
@Builder(toBuilder = true)
public class ArchiveSignatureVerification {

    public static final String DEFAULT_TRIGGER_SOURCE = "MANUAL";

    String id;
    String archiveId;
    String fileId;
    String fileName;
    SignatureDocumentType documentType;
    String triggerSource;
    String providerCode;
    String providerVersion;
    SignatureVerificationStatus verificationStatus;
    int signatureCount;
    int validSignatureCount;
    int invalidSignatureCount;
    String errorCode;
    String errorMessage;
    LocalDateTime verifiedAt;
    LocalDateTime createdTime;
    SignatureVerificationResult result;

    public static ArchiveSignatureVerification create(
            String archiveId,
            String fileId,
            String fileName,
            SignatureDocumentType documentType,
            String triggerSource,
            SignatureVerificationResult result) {
        Objects.requireNonNull(archiveId, "archiveId must not be blank");
        Objects.requireNonNull(documentType, "documentType must not be null");
        Objects.requireNonNull(result, "result must not be null");
        if (archiveId.isBlank()) {
            throw new IllegalArgumentException("archiveId must not be blank");
        }

        String normalizedTriggerSource = triggerSource == null || triggerSource.isBlank()
                ? DEFAULT_TRIGGER_SOURCE
                : triggerSource;

        return ArchiveSignatureVerification.builder()
                .archiveId(archiveId)
                .fileId(fileId)
                .fileName(fileName)
                .documentType(documentType)
                .triggerSource(normalizedTriggerSource)
                .providerCode(result.getProviderCode())
                .providerVersion(result.getProviderVersion())
                .verificationStatus(result.getStatus())
                .signatureCount(result.getSignatureCount())
                .validSignatureCount(result.getValidSignatureCount())
                .invalidSignatureCount(result.getInvalidSignatureCount())
                .errorCode(result.getErrorCode())
                .errorMessage(result.getErrorMessage())
                .verifiedAt(result.getVerifiedAt())
                .result(result)
                .build();
    }
}
