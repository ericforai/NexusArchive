package com.nexusarchive.modules.signature.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusarchive.modules.signature.domain.ArchiveSignatureVerification;
import com.nexusarchive.modules.signature.domain.ArchiveSignatureVerificationRepository;
import com.nexusarchive.modules.signature.domain.SignatureDocumentType;
import com.nexusarchive.modules.signature.domain.SignatureValidationStatus;
import com.nexusarchive.modules.signature.domain.SignatureVerificationResult;
import com.nexusarchive.modules.signature.domain.SignatureVerificationSignature;
import com.nexusarchive.modules.signature.domain.SignatureVerificationStatus;
import com.nexusarchive.modules.signature.infra.mapper.SignatureVerificationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class MybatisArchiveSignatureVerificationRepository implements ArchiveSignatureVerificationRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final SignatureVerificationRecordMapper mapper;

    @Override
    public ArchiveSignatureVerification save(ArchiveSignatureVerification verification) {
        ArchiveSignatureVerification normalized = normalize(verification);
        SignatureVerificationRecordEntity entity = toEntity(normalized);
        if (entity.getId() == null || entity.getId().isBlank()) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public List<ArchiveSignatureVerification> findByArchiveId(String archiveId) {
        return mapper.findByArchiveId(archiveId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ArchiveSignatureVerification> findByFileId(String fileId) {
        return mapper.findByFileId(fileId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ArchiveSignatureVerification normalize(ArchiveSignatureVerification verification) {
        LocalDateTime verifiedAt = verification.getVerifiedAt();
        if (verifiedAt == null && verification.getResult() != null) {
            verifiedAt = verification.getResult().getVerifiedAt();
        }
        if (verifiedAt == null) {
            verifiedAt = LocalDateTime.now();
        }

        SignatureVerificationResult result = verification.getResult();
        if (result != null && result.getVerifiedAt() == null) {
            result = result.toBuilder()
                    .verifiedAt(verifiedAt)
                    .build();
        }

        return ArchiveSignatureVerification.builder()
                .id(verification.getId())
                .archiveId(verification.getArchiveId())
                .fileId(verification.getFileId())
                .fileName(verification.getFileName())
                .documentType(verification.getDocumentType())
                .triggerSource(verification.getTriggerSource())
                .providerCode(result != null ? result.getProviderCode() : verification.getProviderCode())
                .providerVersion(result != null ? result.getProviderVersion() : verification.getProviderVersion())
                .verificationStatus(result != null ? result.getStatus() : verification.getVerificationStatus())
                .signatureCount(result != null ? result.getSignatureCount() : verification.getSignatureCount())
                .validSignatureCount(result != null ? result.getValidSignatureCount() : verification.getValidSignatureCount())
                .invalidSignatureCount(result != null ? result.getInvalidSignatureCount() : verification.getInvalidSignatureCount())
                .errorCode(result != null ? result.getErrorCode() : verification.getErrorCode())
                .errorMessage(result != null ? result.getErrorMessage() : verification.getErrorMessage())
                .verifiedAt(verifiedAt)
                .createdTime(verification.getCreatedTime())
                .result(result)
                .build();
    }

    private SignatureVerificationRecordEntity toEntity(ArchiveSignatureVerification verification) {
        return SignatureVerificationRecordEntity.builder()
                .id(verification.getId())
                .archiveId(verification.getArchiveId())
                .fileId(verification.getFileId())
                .fileName(verification.getFileName())
                .documentType(verification.getDocumentType().name())
                .triggerSource(verification.getTriggerSource())
                .providerCode(verification.getProviderCode())
                .providerVersion(verification.getProviderVersion())
                .verificationStatus(verification.getVerificationStatus().name())
                .signatureCount(verification.getSignatureCount())
                .validSignatureCount(verification.getValidSignatureCount())
                .invalidSignatureCount(verification.getInvalidSignatureCount())
                .errorCode(verification.getErrorCode())
                .errorMessage(verification.getErrorMessage())
                .verifiedAt(verification.getVerifiedAt())
                .resultPayload(serializeResult(verification.getResult()))
                .createdTime(verification.getCreatedTime())
                .build();
    }

    private ArchiveSignatureVerification toDomain(SignatureVerificationRecordEntity entity) {
        SignatureVerificationResult result = deserializeResult(entity.getResultPayload(), entity);

        return ArchiveSignatureVerification.builder()
                .id(entity.getId())
                .archiveId(entity.getArchiveId())
                .fileId(entity.getFileId())
                .fileName(entity.getFileName())
                .documentType(SignatureDocumentType.valueOf(entity.getDocumentType()))
                .triggerSource(entity.getTriggerSource())
                .providerCode(entity.getProviderCode())
                .providerVersion(entity.getProviderVersion())
                .verificationStatus(SignatureVerificationStatus.valueOf(entity.getVerificationStatus()))
                .signatureCount(defaultInt(entity.getSignatureCount()))
                .validSignatureCount(defaultInt(entity.getValidSignatureCount()))
                .invalidSignatureCount(defaultInt(entity.getInvalidSignatureCount()))
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .verifiedAt(entity.getVerifiedAt())
                .createdTime(entity.getCreatedTime())
                .result(result)
                .build();
    }

    private SignatureVerificationResult deserializeResult(String payload, SignatureVerificationRecordEntity entity) {
        if (payload == null || payload.isBlank()) {
            return SignatureVerificationResult.builder()
                    .status(SignatureVerificationStatus.valueOf(entity.getVerificationStatus()))
                    .providerCode(entity.getProviderCode())
                    .providerVersion(entity.getProviderVersion())
                    .verifiedAt(entity.getVerifiedAt())
                    .errorCode(entity.getErrorCode())
                    .errorMessage(entity.getErrorMessage())
                    .signatureDetails(List.of())
                    .build();
        }

        Map<String, Object> json = parsePayload(payload);
        List<SignatureVerificationSignature> details = parseDetails(json.get("signatureDetails"));

        return SignatureVerificationResult.builder()
                .status(parseVerificationStatus(json.get("status"), entity.getVerificationStatus()))
                .providerCode(stringValue(json.get("providerCode"), entity.getProviderCode()))
                .providerVersion(stringValue(json.get("providerVersion"), entity.getProviderVersion()))
                .verifiedAt(parseDateTime(json.get("verifiedAt"), entity.getVerifiedAt()))
                .errorCode(stringValue(json.get("errorCode"), entity.getErrorCode()))
                .errorMessage(stringValue(json.get("errorMessage"), entity.getErrorMessage()))
                .signatureDetails(details)
                .build();
    }

    private List<SignatureVerificationSignature> parseDetails(Object rawValue) {
        if (!(rawValue instanceof List<?> rawList)) {
            return List.of();
        }

        List<SignatureVerificationSignature> details = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            details.add(SignatureVerificationSignature.builder()
                    .status(parseValidationStatus(rawMap.get("status")))
                    .signerName(stringValue(rawMap.get("signerName"), null))
                    .signerOrganization(stringValue(rawMap.get("signerOrganization"), null))
                    .certificateSerialNumber(stringValue(rawMap.get("certificateSerialNumber"), null))
                    .certificateSubject(stringValue(rawMap.get("certificateSubject"), null))
                    .signatureAlgorithm(stringValue(rawMap.get("signatureAlgorithm"), null))
                    .signingTime(parseDateTime(rawMap.get("signingTime"), null))
                    .message(stringValue(rawMap.get("message"), null))
                    .build());
        }
        return details;
    }

    private SignatureVerificationStatus parseVerificationStatus(Object rawValue, String fallback) {
        String value = stringValue(rawValue, fallback);
        return value == null ? SignatureVerificationStatus.ERROR : SignatureVerificationStatus.valueOf(value);
    }

    private SignatureValidationStatus parseValidationStatus(Object rawValue) {
        String value = stringValue(rawValue, SignatureValidationStatus.UNKNOWN.name());
        return SignatureValidationStatus.valueOf(value);
    }

    private LocalDateTime parseDateTime(Object rawValue, LocalDateTime fallback) {
        String value = stringValue(rawValue, null);
        return value == null ? fallback : LocalDateTime.parse(value);
    }

    private String stringValue(Object rawValue, String fallback) {
        return rawValue == null ? fallback : String.valueOf(rawValue);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String serializeResult(SignatureVerificationResult result) {
        if (result == null) {
            return "{}";
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", result.getStatus());
        payload.put("providerCode", result.getProviderCode());
        payload.put("providerVersion", result.getProviderVersion());
        payload.put("verifiedAt", result.getVerifiedAt());
        payload.put("errorCode", result.getErrorCode());
        payload.put("errorMessage", result.getErrorMessage());
        payload.put("signatureDetails", result.getSignatureDetails().stream()
                .map(this::toPayload)
                .toList());
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize signature verification result payload", exception);
        }
    }

    private Map<String, Object> toPayload(SignatureVerificationSignature signature) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", signature.getStatus());
        payload.put("signerName", signature.getSignerName());
        payload.put("signerOrganization", signature.getSignerOrganization());
        payload.put("certificateSerialNumber", signature.getCertificateSerialNumber());
        payload.put("certificateSubject", signature.getCertificateSubject());
        payload.put("signatureAlgorithm", signature.getSignatureAlgorithm());
        payload.put("signingTime", signature.getSigningTime());
        payload.put("message", signature.getMessage());
        return payload;
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            return OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse signature verification result payload", exception);
        }
    }
}
