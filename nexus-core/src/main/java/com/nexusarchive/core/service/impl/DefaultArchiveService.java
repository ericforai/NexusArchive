// Input: Archive ID
// Output: 归档处理逻辑
// Pos: NexusCore service/impl
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.core.domain.ArchiveObject;
import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.domain.PreservationAudit;
import com.nexusarchive.core.mapper.ArchiveObjectMapper;
import com.nexusarchive.core.mapper.FileContentMapper;
import com.nexusarchive.core.mapper.PreservationAuditMapper;
import com.nexusarchive.core.preservation.CheckResult;
import com.nexusarchive.core.preservation.FourNaturesService;
import com.nexusarchive.core.service.ArchiveService;
import com.nexusarchive.core.storage.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultArchiveService implements ArchiveService {

    private final ArchiveObjectMapper archiveMapper;
    private final FileContentMapper fileContentMapper;
    private final PreservationAuditMapper auditMapper;
    private final FourNaturesService fourNaturesService;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(String archiveId) {
        log.info("Starting archival process for archiveId: {}", archiveId);

        ArchiveObject archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new IllegalArgumentException("Archive not found: " + archiveId);
        }

        // 1. Get all files
        List<FileContent> files = fileContentMapper.selectList(
                new LambdaQueryWrapper<FileContent>().eq(FileContent::getItemId, archiveId)
        );

        if (files.isEmpty()) {
            throw new IllegalStateException("No files found for archive: " + archiveId);
        }

        // 2. Perform Four Natures Check for each file
        List<Map<String, Object>> allResults = new ArrayList<>();
        boolean overallPass = true;

        for (FileContent file : files) {
            Path tempFile = null;
            try {
                // Determine storage path (assuming relative path in DB)
                String storagePath = file.getStoragePath();
                if (!storageService.exists(storagePath)) {
                    log.error("File not found in storage: {}", storagePath);
                    overallPass = false;
                    recordFailure(allResults, file, "STORAGE", "File missing in storage");
                    continue;
                }

                // Copy to temp file for validation
                tempFile = Files.createTempFile("archive-check-" + file.getId(), ".tmp");
                try (InputStream is = storageService.getInputStream(storagePath)) {
                    Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                List<CheckResult> results = fourNaturesService.validate(file, tempFile);
                boolean filePass = results.stream().allMatch(CheckResult::isPassed);
                if (!filePass) {
                    overallPass = false;
                }

                Map<String, Object> fileResult = new HashMap<>();
                fileResult.put("fileId", file.getId());
                fileResult.put("fileName", file.getFileName());
                fileResult.put("checks", results);
                fileResult.put("passed", filePass);
                allResults.add(fileResult);

            } catch (Exception e) {
                log.error("Validation failed for file: {}", file.getId(), e);
                overallPass = false;
                recordFailure(allResults, file, "SYSTEM", e.getMessage());
            } finally {
                if (tempFile != null) {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        // 3. Update Archive Status
        String newStatus = overallPass ? "ARCHIVED" : "CORRUPTED";
        archive.setStatus(newStatus);
        archive.setLastModifiedTime(LocalDateTime.now());
        archiveMapper.updateById(archive);

        // 4. Save Audit Report
        try {
            PreservationAudit audit = new PreservationAudit();
            audit.setId(UUID.randomUUID().toString());
            audit.setArchiveId(archiveId);
            audit.setActionType("ARCHIVE"); // or PRESERVATION_CHECK
            audit.setOverallStatus(overallPass ? "PASS" : "FAIL");
            audit.setOperator("system"); // TODO: Context
            audit.setCheckTime(LocalDateTime.now());
            audit.setCheckResultJson(objectMapper.writeValueAsString(allResults));
            
            auditMapper.insert(audit);
        } catch (Exception e) {
            log.error("Failed to save preservation audit", e);
            // Don't rollback archival just because reporting failed? 
            // Strict compliance says: "No audit, no action". So maybe rollback.
            throw new RuntimeException("Failed to save audit log", e);
        }

        log.info("Archival process completed for {}. Status: {}", archiveId, newStatus);
    }

    private void recordFailure(List<Map<String, Object>> allResults, FileContent file, String checkName, String msg) {
        Map<String, Object> res = new HashMap<>();
        res.put("fileId", file.getId());
        res.put("fileName", file.getFileName());
        res.put("passed", false);
        res.put("error", msg);
        allResults.add(res);
    }
}
