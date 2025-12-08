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

        // 2. 获取文件列表
        List<ArcFileContent> files = arcFileContentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                .eq(ArcFileContent::getArchivalCode, archivalCode)
        );
        
        if (files.isEmpty()) {
            throw new BusinessException("档案没有关联文件: " + archivalCode);
        }
        
        // 3. 创建临时工作目录
        Path tempDir = Files.createTempDirectory("AIP_WORK_" + archivalCode + "_");
        log.info("创建临时工作目录: {}", tempDir);
        
        try {
            // 4. 创建目录结构
            Path contentDir = Files.createDirectories(tempDir.resolve("content"));
            Path dataDir = Files.createDirectories(tempDir.resolve("data"));
            Path attachmentDir = Files.createDirectories(tempDir.resolve("attachment"));
            Path signatureDir = Files.createDirectories(tempDir.resolve("signature"));
            
            List<AipIndexFile> indexFiles = new ArrayList<>();
            
            // 5. 处理文件并分类
            for (ArcFileContent fileContent : files) {
                Path sourcePath = Paths.get(fileContent.getStoragePath());
                if (!Files.exists(sourcePath)) {
                    log.warn("文件丢失: {}", sourcePath);
                    continue;
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
            
            // 8. 打包为 ZIP
            File zipFile = File.createTempFile(archivalCode + "_", ".zip");
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                zipDirectory(tempDir, "", zos);
            }
            
            log.info("AIP 包导出完成: {}, 文件大小: {} bytes", archivalCode, zipFile.length());
            return zipFile;
            
        } finally {
            // 清理临时目录 (简单实现，生产环境建议使用专门的清理工具或异步清理)
            // FileUtil.del(tempDir); // 如果引入了 Hutool
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

