// Input: Archive ID
// Output: ZIP Stream to response
// Pos: NexusCore preservation/aip/impl
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.aip.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.core.domain.ArchiveObject;
import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.domain.PreservationAudit;
import com.nexusarchive.core.mapper.ArchiveObjectMapper;
import com.nexusarchive.core.mapper.FileContentMapper;
import com.nexusarchive.core.mapper.PreservationAuditMapper;
import com.nexusarchive.core.preservation.aip.AipExportService;
import com.nexusarchive.core.preservation.aip.model.AipMetadata;
import com.nexusarchive.core.preservation.aip.model.ArchiveInfo;
import com.nexusarchive.core.preservation.aip.model.AuditLogInfo;
import com.nexusarchive.core.preservation.aip.model.FileInfo;
import com.nexusarchive.core.preservation.aip.model.FondsInfo;
import com.nexusarchive.core.storage.StorageService;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAipExportService implements AipExportService {

    private final ArchiveObjectMapper archiveMapper;
    private final FileContentMapper fileContentMapper;
    private final PreservationAuditMapper auditMapper;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public void exportAip(String archiveId, OutputStream outputStream) {
        log.info("Starting AIP export for archive: {}", archiveId);

        ArchiveObject archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new IllegalArgumentException("Archive not found");
        }

        List<FileContent> files = fileContentMapper.selectList(
                new LambdaQueryWrapper<FileContent>().eq(FileContent::getItemId, archiveId)
        );

        List<PreservationAudit> audits = auditMapper.selectList(
                new LambdaQueryWrapper<PreservationAudit>().eq(PreservationAudit::getArchiveId, archiveId)
        );

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            // 1. Prepare Metadata Model
            AipMetadata metadata = buildMetadata(archive, files, audits);
            
            // 2. Write metadata.xml
            zos.putNextEntry(new ZipEntry("metadata.xml"));
            JAXBContext context = JAXBContext.newInstance(AipMetadata.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(metadata, zos);
            zos.closeEntry();

            StringBuilder manifestBuilder = new StringBuilder();

            // 3. Helper buffer for hash calc
            byte[] buffer = new byte[8192];

            // 4. Write Files to content/ dir
            for (FileContent file : files) {
                String entryName = "content/" + sanitizeFileName(file.getFileName());
                zos.putNextEntry(new ZipEntry(entryName));
                
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                try (InputStream is = storageService.getInputStream(file.getStoragePath())) {
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                        sha256.update(buffer, 0, len);
                    }
                }
                zos.closeEntry();
                
                String hash = bytesToHex(sha256.digest());
                manifestBuilder.append(hash).append("  ").append(entryName).append("\n");
            }

            // 5. Write manifest.sha256
            zos.putNextEntry(new ZipEntry("manifest.sha256"));
            zos.write(manifestBuilder.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            
            log.info("AIP export completed for {}", archiveId);

        } catch (Exception e) {
            log.error("Failed to export AIP", e);
            throw new RuntimeException("AIP Export failed", e);
        }
    }

    private AipMetadata buildMetadata(ArchiveObject archive, List<FileContent> files, List<PreservationAudit> audits) {
        AipMetadata meta = new AipMetadata();
        
        // Fonds Info (Mock or Fetch if available)
        FondsInfo fonds = new FondsInfo();
        fonds.setFondsCode(archive.getFondsNo()); // Assuming exists
        fonds.setFondsName("Current Fonds"); // TODO: Fetch name
        meta.setFondsInfo(fonds);

        // Archive Info
        ArchiveInfo arcInfo = new ArchiveInfo();
        arcInfo.setId(archive.getId());
        arcInfo.setTitle(archive.getTitle());
        arcInfo.setFiscalYear(archive.getArchiveYear());
        arcInfo.setCategoryCode(archive.getCategoryCode());
        arcInfo.setStatus(archive.getStatus());
        if (archive.getAmount() != null) {
            arcInfo.setAmount(archive.getAmount().toString());
        }
        meta.setArchiveInfo(arcInfo);

        // Files
        List<FileInfo> fileInfos = new ArrayList<>();
        for (FileContent f : files) {
            FileInfo info = new FileInfo();
            info.setId(f.getId());
            info.setFileName(f.getFileName());
            info.setFileType(f.getFileType());
            info.setSize(f.getFileSize());
            info.setOriginalHash(f.getFileHash());
            info.setHashAlgorithm(f.getHashAlgorithm());
            info.setStoragePath(f.getStoragePath());
            fileInfos.add(info);
        }
        meta.setFiles(fileInfos);

        // Audits
        List<AuditLogInfo> auditInfos = new ArrayList<>();
        for (PreservationAudit a : audits) {
            AuditLogInfo info = new AuditLogInfo();
            info.setActionType(a.getActionType());
            info.setOperator(a.getOperator());
            info.setResult(a.getOverallStatus());
            info.setCheckTime(a.getCheckTime().toString());
            info.setDetails(a.getCheckResultJson());
            auditInfos.add(info);
        }
        meta.setAuditLogs(auditInfos);

        return meta;
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
