// Input: MyBatis-Plus、Lombok、Spring Framework
// Output: AipPackageExporter 类
// Pos: 案卷服务 - AIP包导出层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.volume;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VolumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * AIP包导出器
 * <p>
 * 符合 DA/T 94-2022 和 GB/T 39674 标准
 * </p>
 * <p>
 * AIP 包结构:
 * /AIP_Root
 *   ├── index.xml          - 总索引文件
 *   ├── /metadata          - 元数据目录
 *   │    └── volume.xml    - 案卷元数据
 *   ├── /content           - 内容文件目录 (凭证 PDF/OFD)
 *   └── /logs              - 审计日志
 *        └── audit.xml     - 归档审计日志
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AipPackageExporter {

    private final VolumeMapper volumeMapper;
    private final ArchiveMapper archiveMapper;

    @Value("${archive.root.path:/data/archives}")
    private String archiveRootPath;

    /**
     * 导出案卷 AIP 包
     */
    public File exportAipPackage(String volumeId) throws IOException {
        log.info("开始导出案卷 AIP 包: volumeId={}", volumeId);

        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }

        if (!"archived".equals(volume.getStatus())) {
            throw new BusinessException("只有已归档的案卷可以导出 AIP 包");
        }

        List<Archive> files = VolumeQuery.getVolumeFiles(archiveMapper, volumeId);

        // 创建临时目录 - 使用时间戳避免路径遍历风险
        Path tempDir = Files.createTempDirectory("aip_" + System.currentTimeMillis() + "_");

        try {
            // 1. 创建目录结构
            Path metadataDir = Files.createDirectories(tempDir.resolve("metadata"));
            Path contentDir = Files.createDirectories(tempDir.resolve("content"));
            Path logsDir = Files.createDirectories(tempDir.resolve("logs"));

            // 2. 生成 index.xml (总索引)
            String indexXml = generateIndexXml(volume, files);
            Files.writeString(tempDir.resolve("index.xml"), indexXml);

            // 3. 生成 volume.xml (案卷元数据)
            String volumeXml = generateVolumeXml(volume, files);
            Files.writeString(metadataDir.resolve("volume.xml"), volumeXml);

            // 4. 收集内容文件
            int fileIndex = 1;
            for (Archive archive : files) {
                String filename = String.format("%03d_%s.pdf", fileIndex++, archive.getArchiveCode().replace("/", "_"));
                Path targetPath = contentDir.resolve(filename);

                String sourcePath = archiveRootPath + "/" + archive.getFondsNo() + "/" + archive.getArchiveCode() + ".pdf";
                Path source = Paths.get(sourcePath);
                if (Files.exists(source)) {
                    Files.copy(source, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // 如果实际文件不存在，使用占位 PDF
                    VolumePdfGenerator.generatePlaceholderPdf(targetPath, archive);
                }
            }

            // 5. 生成审计日志
            String auditXml = generateAuditXml(volume, files);
            Files.writeString(logsDir.resolve("audit.xml"), auditXml);

            // 6. 打包为 ZIP
            String zipFileName = volume.getVolumeCode() + "_AIP.zip";
            File zipFile = File.createTempFile("aip_export_" + System.currentTimeMillis() + "_", ".zip");

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile), java.nio.charset.StandardCharsets.UTF_8)) {
                VolumeUtils.zipDirectory(tempDir, "", zos);
            }

            log.info("AIP 包导出完成: {}, 大小: {} bytes", volume.getVolumeCode(), zipFile.length());
            return zipFile;

        } finally {
            // 清理临时目录
            VolumeUtils.deleteDirectoryRecursively(tempDir);
        }
    }

    /**
     * 生成 index.xml (总索引)
     */
    private String generateIndexXml(Volume volume, List<Archive> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<aip_package xmlns=\"http://www.saac.gov.cn/dat94/2022\">\n");
        sb.append("  <header>\n");
        sb.append("    <version>1.0</version>\n");
        sb.append("    <standard>DA/T 94-2022</standard>\n");
        sb.append("    <created_time>").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</created_time>\n");
        sb.append("  </header>\n");
        sb.append("  <volume_info>\n");
        sb.append("    <volume_code>").append(VolumeUtils.escapeXml(volume.getVolumeCode())).append("</volume_code>\n");
        sb.append("    <title>").append(VolumeUtils.escapeXml(volume.getTitle())).append("</title>\n");
        sb.append("    <fonds_no>").append(volume.getFondsNo()).append("</fonds_no>\n");
        sb.append("    <fiscal_year>").append(volume.getFiscalYear()).append("</fiscal_year>\n");
        sb.append("    <fiscal_period>").append(volume.getFiscalPeriod()).append("</fiscal_period>\n");
        sb.append("    <category_code>").append(volume.getCategoryCode()).append("</category_code>\n");
        sb.append("    <retention_period>").append(volume.getRetentionPeriod()).append("</retention_period>\n");
        sb.append("    <file_count>").append(files.size()).append("</file_count>\n");
        sb.append("    <archived_at>").append(volume.getArchivedAt() != null ? volume.getArchivedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "").append("</archived_at>\n");
        sb.append("    <reviewed_by>").append(volume.getReviewedBy() != null ? volume.getReviewedBy() : "").append("</reviewed_by>\n");
        sb.append("  </volume_info>\n");
        sb.append("  <file_list>\n");

        int seq = 1;
        for (Archive f : files) {
            sb.append("    <file seq=\"").append(seq++).append("\">\n");
            sb.append("      <archive_code>").append(f.getArchiveCode()).append("</archive_code>\n");
            sb.append("      <title>").append(VolumeUtils.escapeXml(f.getTitle())).append("</title>\n");
            sb.append("      <doc_date>").append(f.getDocDate() != null ? f.getDocDate().toString() : "").append("</doc_date>\n");
            sb.append("      <amount>").append(f.getAmount() != null ? f.getAmount().toString() : "0").append("</amount>\n");
            sb.append("      <creator>").append(VolumeUtils.escapeXml(f.getCreator())).append("</creator>\n");
            sb.append("      <retention_period>").append(f.getRetentionPeriod()).append("</retention_period>\n");
            sb.append("      <content_path>content/").append(String.format("%03d_%s.pdf", seq - 1, f.getArchiveCode().replace("/", "_"))).append("</content_path>\n");
            sb.append("    </file>\n");
        }

        sb.append("  </file_list>\n");
        sb.append("</aip_package>\n");
        return sb.toString();
    }

    /**
     * 生成 volume.xml (案卷元数据)
     */
    private String generateVolumeXml(Volume volume, List<Archive> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<volume xmlns=\"http://www.saac.gov.cn/dat94/2022\">\n");
        sb.append("  <id>").append(volume.getId()).append("</id>\n");
        sb.append("  <volume_code>").append(VolumeUtils.escapeXml(volume.getVolumeCode())).append("</volume_code>\n");
        sb.append("  <title>").append(VolumeUtils.escapeXml(volume.getTitle())).append("</title>\n");
        sb.append("  <fonds_no>").append(volume.getFondsNo()).append("</fonds_no>\n");
        sb.append("  <fiscal_year>").append(volume.getFiscalYear()).append("</fiscal_year>\n");
        sb.append("  <fiscal_period>").append(volume.getFiscalPeriod()).append("</fiscal_period>\n");
        sb.append("  <category_code>").append(volume.getCategoryCode()).append("</category_code>\n");
        sb.append("  <category_name>会计凭证</category_name>\n");
        sb.append("  <file_count>").append(volume.getFileCount()).append("</file_count>\n");
        sb.append("  <retention_period>").append(volume.getRetentionPeriod()).append("</retention_period>\n");
        sb.append("  <status>").append(volume.getStatus()).append("</status>\n");
        sb.append("  <created_time>").append(volume.getCreatedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</created_time>\n");
        sb.append("  <archived_at>").append(volume.getArchivedAt() != null ? volume.getArchivedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "").append("</archived_at>\n");
        sb.append("  <reviewed_by>").append(volume.getReviewedBy() != null ? volume.getReviewedBy() : "").append("</reviewed_by>\n");
        sb.append("</volume>\n");
        return sb.toString();
    }

    /**
     * 生成审计日志 XML
     */
    private String generateAuditXml(Volume volume, List<Archive> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<audit_log xmlns=\"http://www.saac.gov.cn/dat94/2022\">\n");
        sb.append("  <export_time>").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</export_time>\n");
        sb.append("  <volume_code>").append(volume.getVolumeCode()).append("</volume_code>\n");
        sb.append("  <operation>AIP_EXPORT</operation>\n");
        sb.append("  <events>\n");
        sb.append("    <event>\n");
        sb.append("      <time>").append(volume.getCreatedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</time>\n");
        sb.append("      <action>VOLUME_CREATED</action>\n");
        sb.append("      <description>案卷创建</description>\n");
        sb.append("    </event>\n");
        if (volume.getArchivedAt() != null) {
            sb.append("    <event>\n");
            sb.append("      <time>").append(volume.getArchivedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</time>\n");
            sb.append("      <action>VOLUME_ARCHIVED</action>\n");
            sb.append("      <operator>").append(volume.getReviewedBy() != null ? volume.getReviewedBy() : "").append("</operator>\n");
            sb.append("      <description>案卷归档</description>\n");
            sb.append("    </event>\n");
        }
        sb.append("    <event>\n");
        sb.append("      <time>").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</time>\n");
        sb.append("      <action>AIP_EXPORTED</action>\n");
        sb.append("      <description>AIP包导出</description>\n");
        sb.append("      <file_count>").append(files.size()).append("</file_count>\n");
        sb.append("    </event>\n");
        sb.append("  </events>\n");
        sb.append("</audit_log>\n");
        return sb.toString();
    }
}
