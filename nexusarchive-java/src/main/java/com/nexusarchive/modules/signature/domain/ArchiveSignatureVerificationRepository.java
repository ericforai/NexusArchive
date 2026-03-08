package com.nexusarchive.modules.signature.domain;

import java.util.List;

public interface ArchiveSignatureVerificationRepository {

    ArchiveSignatureVerification save(ArchiveSignatureVerification verification);

    List<ArchiveSignatureVerification> findByArchiveId(String archiveId);

    List<ArchiveSignatureVerification> findByFileId(String fileId);
}
