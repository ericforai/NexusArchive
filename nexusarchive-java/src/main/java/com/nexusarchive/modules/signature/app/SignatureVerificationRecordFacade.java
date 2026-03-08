package com.nexusarchive.modules.signature.app;

import com.nexusarchive.modules.signature.domain.ArchiveSignatureVerification;

import java.util.List;

public interface SignatureVerificationRecordFacade {

    ArchiveSignatureVerification save(ArchiveSignatureVerification verification);

    List<ArchiveSignatureVerification> findByArchiveId(String archiveId);

    List<ArchiveSignatureVerification> findByFileId(String fileId);
}
