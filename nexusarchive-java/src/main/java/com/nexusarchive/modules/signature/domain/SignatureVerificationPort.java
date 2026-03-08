package com.nexusarchive.modules.signature.domain;

public interface SignatureVerificationPort {

    boolean supports(SignatureDocumentType documentType);

    SignatureVerificationResult verify(SignatureVerificationRequest request);
}
