package com.nexusarchive.modules.signature.app;

import com.nexusarchive.modules.signature.domain.ArchiveSignatureVerification;
import com.nexusarchive.modules.signature.domain.ArchiveSignatureVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SignatureVerificationRecordService implements SignatureVerificationRecordFacade {

    private final ArchiveSignatureVerificationRepository repository;

    @Override
    public ArchiveSignatureVerification save(ArchiveSignatureVerification verification) {
        if (verification == null) {
            throw new IllegalArgumentException("verification must not be null");
        }
        if (verification.getArchiveId() == null || verification.getArchiveId().isBlank()) {
            throw new IllegalArgumentException("archiveId must not be blank");
        }
        if (verification.getDocumentType() == null) {
            throw new IllegalArgumentException("documentType must not be null");
        }
        if (verification.getTriggerSource() == null || verification.getTriggerSource().isBlank()) {
            throw new IllegalArgumentException("triggerSource must not be blank");
        }
        if (verification.getResult() == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        if (verification.getResult().getStatus() == null) {
            throw new IllegalArgumentException("result.status must not be null");
        }
        return repository.save(verification);
    }

    @Override
    public List<ArchiveSignatureVerification> findByArchiveId(String archiveId) {
        if (archiveId == null || archiveId.isBlank()) {
            throw new IllegalArgumentException("archiveId must not be blank");
        }
        return repository.findByArchiveId(archiveId);
    }

    @Override
    public List<ArchiveSignatureVerification> findByFileId(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("fileId must not be blank");
        }
        return repository.findByFileId(fileId);
    }
}
