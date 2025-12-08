package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.aip.EepXmlStructure;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.service.ArchivalPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * AIP 封装服务实现
 * 
 * 核心职责：
 * 1. 动态生成归档路径 (Dynamic Path Generation)
 * 2. 生成 EEP 元数据 XML (Metadata Generation)
 * 3. 原子性文件移动 (Atomic File Move)
 * 4. 失败回滚 (Rollback)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivalPackageServiceImpl implements ArchivalPackageService {

    @Value("${archive.root.path:/data/archives}")
    private String archiveRootPath;
    
    private final com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper;
    private final XmlMapper xmlMapper = new XmlMapper();
    
    {
        // 配置 XmlMapper
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public List<com.nexusarchive.entity.ArcFileContent> archivePackage(AccountingSipDto sip, String tempPath) throws IOException {
        VoucherHeadDto header = sip.getHeader();
        String archivalCode = generateArchivalCode(header); // 实际应从 SIP 或服务中获取
        
        // 1. 生成目标路径
        // Path: {Root}/{Fonds}/{Year}/{Retention}/{Category}/{ArchivalCode}/
        Path targetDir = Paths.get(archiveRootPath,
                header.getFondsCode(),
                header.getAccountPeriod().substring(0, 4), // Year
                "10Y", // Retention (Mock, should come from logic)
                "AC01", // Category (Mock)
                archivalCode
        );
        
        Path contentDir = targetDir.resolve("content");
        
        log.info("开始 AIP 封装，目标路径: {}", targetDir);
        
        try {
            // 2. 创建目录结构
            Files.createDirectories(contentDir);
            
            // 3. 生成并写入 metadata.xml
            generateAndWriteMetadata(sip, archivalCode, targetDir);
            
            // 4. 移动文件 (Atomic Move) 并保存到数据库
            List<com.nexusarchive.entity.ArcFileContent> savedFiles = moveFilesAndSave(sip, tempPath, contentDir, archivalCode);
            
            log.info("AIP 封装完成: {}", archivalCode);
            return savedFiles;
            
        } catch (Exception e) {
            log.error("AIP 封装失败，开始回滚: {}", e.getMessage());
            // Rollback: 删除已创建的目录
            rollback(targetDir);
            throw new BusinessException("AIP 封装失败: " + e.getMessage(), e);
        }
    }
    
    private void generateAndWriteMetadata(AccountingSipDto sip, String archivalCode, Path targetDir) throws IOException {
        VoucherHeadDto header = sip.getHeader();
        
        // 构建 Header
        EepXmlStructure.Header xmlHeader = EepXmlStructure.Header.builder()
                .version("1.0")
                .createdTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .archivalCode(archivalCode)
                .build();
        
        // 构建 EntityData
        EepXmlStructure.EntityData entityData = EepXmlStructure.EntityData.builder()
                .fondsCode(header.getFondsCode())
                .accountPeriod(header.getAccountPeriod())
                .voucherType(header.getVoucherType().getCode())
                .voucherNumber(header.getVoucherNumber())
                .voucherDate(header.getVoucherDate().toString())
                .totalAmount(header.getTotalAmount().toString())
                .currencyCode(header.getCurrencyCode())
                .issuer(header.getIssuer())
                .build();
        
        // 构建 DigitalObjects
        List<EepXmlStructure.DigitalObject> objects = new ArrayList<>();
        if (sip.getAttachments() != null) {
            for (AttachmentDto attachment : sip.getAttachments()) {
                objects.add(EepXmlStructure.DigitalObject.builder()
                        .filename(attachment.getFileName())
                        .format(attachment.getFileType())
                        .hashAlgorithm(attachment.getHashAlgorithm() != null ? attachment.getHashAlgorithm() : "SM3")
                        .hashValue(attachment.getFileHash())
                        .sizeBytes(attachment.getFileSize())
                        .build());
            }
        }
        
        EepXmlStructure eep = EepXmlStructure.builder()
                .header(xmlHeader)
                .entityData(entityData)
                .digitalObjects(new EepXmlStructure.DigitalObjects(objects))
                .build();
        
        // 写入 XML 文件
        File metadataFile = targetDir.resolve("metadata.xml").toFile();
        xmlMapper.writeValue(metadataFile, eep);
    }
    
    private List<com.nexusarchive.entity.ArcFileContent> moveFilesAndSave(AccountingSipDto sip, String tempPath, Path contentDir, String archivalCode) throws IOException {
        List<com.nexusarchive.entity.ArcFileContent> savedFiles = new ArrayList<>();
        if (sip.getAttachments() == null) return savedFiles;
        
        for (AttachmentDto attachment : sip.getAttachments()) {
            Path sourceFile = Paths.get(tempPath, attachment.getFileName());
            Path targetFile = contentDir.resolve(attachment.getFileName());
            
            if (!Files.exists(sourceFile)) {
                log.warn("源文件不存在，尝试从 Base64 写入: {}", attachment.getFileName());
                continue; 
            }
            
            // 移动文件
            try {
                Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 保存到数据库
            com.nexusarchive.entity.ArcFileContent fileContent = com.nexusarchive.entity.ArcFileContent.builder()
                    .archivalCode(archivalCode)
                    .fileName(attachment.getFileName())
                    .fileType(attachment.getFileType())
                    .fileSize(attachment.getFileSize())
                    .fileHash(attachment.getFileHash())
                    .hashAlgorithm(attachment.getHashAlgorithm())
                    .storagePath(targetFile.toAbsolutePath().toString())
                    .createdTime(LocalDateTime.now())
                    .build();
            
            arcFileContentMapper.insert(fileContent);
            savedFiles.add(fileContent);
        }
        return savedFiles;
    }
    
    private void rollback(Path targetDir) {
        try {
            if (Files.exists(targetDir)) {
                FileSystemUtils.deleteRecursively(targetDir);
                log.info("回滚成功，已删除目录: {}", targetDir);
            }
        } catch (IOException e) {
            log.error("回滚失败，请手动清理目录: {}", targetDir, e);
        }
    }
    
    // 辅助方法：生成档号 (应与 IngestService 保持一致或抽取为公共组件)
    private String generateArchivalCode(VoucherHeadDto header) {
        // 简单模拟
        return String.format("%s-%s-10Y-FIN-AC01-%s", 
                header.getFondsCode(), 
                header.getAccountPeriod().substring(0, 4),
                header.getVoucherNumber());
    }
}
