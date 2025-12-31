// Input: DB, Storage
// Output: Audit Logs
// Pos: NexusCore preservation/fixity/impl
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.fixity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.core.compliance.FileHashService;
import com.nexusarchive.core.compliance.HashAlgorithm;
import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.domain.PreservationAudit;
import com.nexusarchive.core.mapper.FileContentMapper;
import com.nexusarchive.core.mapper.PreservationAuditMapper;
import com.nexusarchive.core.preservation.fixity.FixityCheckService;
import com.nexusarchive.core.storage.StorageService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultFixityCheckService implements FixityCheckService {

    private final FileContentMapper fileContentMapper;
    private final PreservationAuditMapper auditMapper;
    private final StorageService storageService;
    private final FileHashService fileHashService;

    @Override
    @Transactional
    public int performBatchCheck(int limit) {
        log.info("Starting fixity check batch, limit: {}", limit);
        
        // Simple strategy: Check files ordered by ID (or last checked time if we had that column)
        // For compliance MVP, we just take the first N for demonstration/testing
        // In production, this should likely track 'last_fixity_check_time'
        Page<FileContent> page = new Page<>(1, limit);
        List<FileContent> files = fileContentMapper.selectPage(page, new LambdaQueryWrapper<FileContent>()
                .orderByAsc(FileContent::getId)).getRecords();

        int processed = 0;
        for (FileContent file : files) {
            checkFile(file);
            processed++;
        }
        
        log.info("Fixity check batch completed. Processed: {}", processed);
        return processed;
    }

    private void checkFile(FileContent file) {
        String status = "VALID";
        String details = "Hash match";
        
        try {
            // 1. Get content stream
            Path tempFile = Files.createTempFile("fixity_", ".tmp");
            try (InputStream is = storageService.getInputStream(file.getStoragePath())) {
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                
                // 2. Calculate current hash
                // Map DB algorithm string to Enum. Default to SHA256 if null/unknown.
                HashAlgorithm alg = HashAlgorithm.SHA256;
                if ("SM3".equalsIgnoreCase(file.getHashAlgorithm())) {
                    alg = HashAlgorithm.SM3;
                }
                
                String currentHash = fileHashService.hashFile(tempFile, alg);
                
                // 3. Compare
                if (!currentHash.equalsIgnoreCase(file.getFileHash())) {
                    status = "CORRUPTED";
                    details = "Hash mismatch. Expected: " + file.getFileHash() + ", Actual: " + currentHash;
                    log.error("Fixity check FAILED for file {}: {}", file.getId(), details);
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            status = "ERROR";
            details = "Check failed: " + e.getMessage();
            log.error("Error during fixity check for file {}", file.getId(), e);
        }

        // 4. Record Audit
        PreservationAudit audit = new PreservationAudit();
        audit.setArchiveId(file.getItemId()); // Link to Archive Object
        audit.setActionType("FIXITY_CHECK");
        audit.setOperator("SYSTEM_SCHEDULER");
        audit.setCheckTime(LocalDateTime.now());
        audit.setOverallStatus(status);
        String auditJson = String.format(
                "{\"fileId\":\"%s\", \"details\":\"%s\"}", file.getId(), details);
        audit.setCheckResultJson(auditJson);
        
        auditMapper.insert(audit);
    }
}
