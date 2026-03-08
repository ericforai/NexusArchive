package com.nexusarchive.modules.signature.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class SignatureVerificationRequest {

    SignatureDocumentType documentType;
    String fileName;
    byte[] content;
}
