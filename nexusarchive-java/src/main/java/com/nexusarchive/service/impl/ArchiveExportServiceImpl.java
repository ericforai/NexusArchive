// Input: Jackson、Lombok、Spring Framework、Java 标准库、等
// Output: ArchiveExportServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.aip.AipAccountingXml;
import com.nexusarchive.dto.aip.AipIndexFile;
import com.nexusarchive.dto.aip.AipIndexXml;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.ArchiveExportService;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.util.PathSecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.util.FileSystemUtils;

/**
 * 档案导出服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveExportServiceImpl implements ArchiveExportService {

    @Value("${archive.root.path:/data/archives}")
    private String archiveRootPath;

    private final ArcFileContentMapper arcFileContentMapper;
    private final ArchiveMapper archiveMapper;
    private final DataScopeService dataScopeService;
    private final PathSecurityUtils pathSecurityUtils;
    
    private final XmlMapper xmlMapper = new XmlMapper();
    
    {
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    @Override
    public File exportAipPackage(String archivalCode) throws IOException {
        log.info("开始导出结构化 AIP 包: {}", archivalCode);

        // 1. 获取档案信息
        Archive archive = archiveMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Archive>()
                .eq(Archive::getArchiveCode, archivalCode)
        );

        if (archive == null) {
            throw new BusinessException("档案不存在: " + archivalCode);
        }

        // 2. 数据权限检查 - 防止用户导出无权访问的全宗档案
        DataScopeService.DataScopeContext scope = dataScopeService.resolve();
        if (!dataScopeService.canAccessArchive(archive, scope)) {
            log.warn("用户尝试导出无权访问的档案: archivalCode={}, fondsNo={}", archivalCode, archive.getFondsNo());
            throw new BusinessException("无权访问该档案");
        }

        // 3. 获取文件列表
        List<ArcFileContent> files = arcFileContentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                .eq(ArcFileContent::getArchivalCode, archivalCode)
        );

        if (files.isEmpty()) {
            throw new BusinessException("档案没有关联文件: " + archivalCode);
        }

        // 4. 创建临时工作目录 - 使用时间戳避免路径遍历风险
        Path tempDir = Files.createTempDirectory("AIP_WORK_" + System.currentTimeMillis() + "_");
        log.info("创建临时工作目录: {}", tempDir);

        try {
            // 5. 创建目录结构
            Path contentDir = Files.createDirectories(tempDir.resolve("content"));
            Path dataDir = Files.createDirectories(tempDir.resolve("data"));
            Path attachmentDir = Files.createDirectories(tempDir.resolve("attachment"));
            Path signatureDir = Files.createDirectories(tempDir.resolve("signature"));

            List<AipIndexFile> indexFiles = new ArrayList<>();

            // 6. 处理文件并分类
            for (ArcFileContent fileContent : files) {
                // [S2229] 路径遍历防护：使用 PathSecurityUtils 验证路径
                // storagePath 来自数据库，需要验证其在允许的目录内
                Path sourcePath = pathSecurityUtils.validateArchivePath(fileContent.getStoragePath());
                if (!Files.exists(sourcePath)) {
                    throw new BusinessException("文件丢失: " + sourcePath);
                }
                
                String fileName = fileContent.getFileName();
                String lowerName = fileName.toLowerCase();
                Path targetPath;
                String type;
                String relation;
                String relativePath;
                
                // 简单的分类逻辑 (实际项目中可能需要更复杂的规则)
                if (lowerName.endsWith(".ofd") || (lowerName.endsWith(".pdf") && lowerName.contains("voucher"))) {
                    // 主件: OFD发票 或 凭证PDF
                    targetPath = contentDir.resolve(fileName);
                    type = "Main";
                    relation = "Primary";
                    relativePath = "content/" + fileName;
                } else {
                    // 附件: 其他文件
                    targetPath = attachmentDir.resolve(fileName);
                    type = "Attachment";
                    relation = "Support";
                    relativePath = "attachment/" + fileName;
                }
                
                Files.copy(sourcePath, targetPath);
                
                indexFiles.add(AipIndexFile.builder()
                        .type(type)
                        .filename(relativePath)
                        .relation(relation)
                        .build());
            }
            
            // 6. 生成 index.xml
            AipIndexXml indexXml = AipIndexXml.builder()
                    .files(indexFiles)
                    .build();
            xmlMapper.writeValue(tempDir.resolve("index.xml").toFile(), indexXml);
            
            // 7. 生成 accounting.xml
            AipAccountingXml accountingXml = AipAccountingXml.builder()
                    .fondsCode(archive.getFondsNo())
                    .archivalCode(archive.getArchiveCode())
                    .fiscalYear(archive.getFiscalYear())
                    .fiscalPeriod(archive.getFiscalPeriod())
                    .retentionPeriod(archive.getRetentionPeriod())
                    .categoryCode(archive.getCategoryCode())
                    .title(archive.getTitle())
                    .creator(archive.getCreator())
                    .orgName(archive.getOrgName())
                    .securityLevel(archive.getSecurityLevel())
                    .build();
            xmlMapper.writeValue(dataDir.resolve("accounting.xml").toFile(), accountingXml);
            
            // 8. 打包为 ZIP - 使用时间戳避免路径遍历风险
            File zipFile = File.createTempFile("aip_export_" + System.currentTimeMillis() + "_", ".zip");
            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                zipDirectory(tempDir, "", zos);
            }
            
            log.info("AIP 包导出完成: {}, 文件大小: {} bytes", archivalCode, zipFile.length());
            return zipFile;
            
        } finally {
            // 清理临时目录 (简单实现，生产环境建议使用专门的清理工具或异步清理)
            try {
                FileSystemUtils.deleteRecursively(tempDir);
            } catch (Exception e) {
                log.warn("临时目录清理失败: {}", tempDir, e);
            }
        }
    }
    
    /**
     * 递归压缩目录
     */
    private void zipDirectory(Path sourceDir, String baseName, ZipOutputStream zos) throws IOException {
        Files.walk(sourceDir).forEach(path -> {
            try {
                if (Files.isRegularFile(path)) {
                    String zipEntryName = baseName + sourceDir.relativize(path).toString();
                    // Windows下路径分隔符处理
                    zipEntryName = zipEntryName.replace('\\', '/');
                    
                    zos.putNextEntry(new ZipEntry(zipEntryName));
                    Files.copy(path, zos);
                    zos.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException("压缩文件失败: " + path, e);
            }
        });
    }
}
