package com.nexusarchive.modules.signature.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class SignatureVerificationSignature {

    SignatureValidationStatus status;
    String signerName;
    String signerOrganization;
    String certificateSerialNumber;
    String certificateSubject;
    String signatureAlgorithm;
    LocalDateTime signingTime;
    String message;

    public static SignatureVerificationSignature valid(String signerName) {
        return SignatureVerificationSignature.builder()
                .status(SignatureValidationStatus.VALID)
                .signerName(signerName)
                .build();
    }

    public static SignatureVerificationSignature invalid(String signerName, String message) {
        return SignatureVerificationSignature.builder()
                .status(SignatureValidationStatus.INVALID)
                .signerName(signerName)
                .message(message)
                .build();
    }

    public static SignatureVerificationSignature unknown(String signerName, String message) {
        return SignatureVerificationSignature.builder()
                .status(SignatureValidationStatus.UNKNOWN)
                .signerName(signerName)
                .message(message)
                .build();
    }
}
