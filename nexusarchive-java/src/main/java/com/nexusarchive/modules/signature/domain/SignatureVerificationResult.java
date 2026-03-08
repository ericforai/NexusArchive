package com.nexusarchive.modules.signature.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class SignatureVerificationResult {

    SignatureVerificationStatus status;
    String providerCode;
    String providerVersion;
    LocalDateTime verifiedAt;
    String errorCode;
    String errorMessage;

    @Builder.Default
    List<SignatureVerificationSignature> signatureDetails = List.of();

    public int getSignatureCount() {
        return signatureDetails.size();
    }

    public int getValidSignatureCount() {
        return countByStatus(SignatureValidationStatus.VALID);
    }

    public int getInvalidSignatureCount() {
        return countByStatus(SignatureValidationStatus.INVALID);
    }

    public int getUnknownSignatureCount() {
        return countByStatus(SignatureValidationStatus.UNKNOWN);
    }

    private int countByStatus(SignatureValidationStatus status) {
        return Math.toIntExact(signatureDetails.stream()
                .filter(detail -> detail.getStatus() == status)
                .count());
    }
}
