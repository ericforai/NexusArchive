package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.PoolItemDto;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 电子凭证池控制器
 */
@RestController
@RequestMapping("/pool")
@RequiredArgsConstructor
@Slf4j
public class PoolController {

    private final ArcFileContentMapper arcFileContentMapper;
    private final ArcFileMetadataIndexMapper arcFileMetadataIndexMapper;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private static final String[] SOURCE_SYSTEMS = {
        "Web上传", "用友", "金蝶", "泛微OA", "易快报", "汇联易", "SAP"
    };

    /**
     * 查询电子凭证池列表
     * @return 凭证池列表
     */
    @GetMapping("/list")
    public Result<List<PoolItemDto>> listPoolItems() {
        log.info("查询电子凭证池列表");
        
        // 查询所有临时档号开头的记录
        QueryWrapper<ArcFileContent> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("archival_code", "TEMP-POOL-")
                   .orderByDesc("created_time");
        
        List<ArcFileContent> fileContents = arcFileContentMapper.selectList(queryWrapper);
        
        // 转换为前端需要的格式
        List<PoolItemDto> poolItems = fileContents.stream()
                .map(this::convertToPoolItemDto)
                .collect(Collectors.toList());
        
        log.info("查询到 {} 条电子凭证池记录", poolItems.size());
        return Result.success(poolItems);
    }

    /**
     * 预览文件
     * @param id 文件ID
     * @return 文件流
     */
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewFile(@PathVariable String id) {
        log.info("请求预览文件: {}", id);
        
        ArcFileContent fileContent = arcFileContentMapper.selectById(id);
        if (fileContent == null) {
            log.error("文件不存在: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        try {
            Path filePath = Paths.get(fileContent.getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/octet-stream";
                String fileName = fileContent.getFileName().toLowerCase();
                if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".xml")) {
                    contentType = "text/xml";
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileContent.getFileName() + "\"")
                        .body(resource);
            } else {
                log.error("文件无法读取: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("文件路径错误", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 生成演示数据
     * @return 结果
     */
    @GetMapping("/generate-demo")
    public Result<String> generateDemoData() {
        log.info("开始生成演示数据...");
        
        try {
            ClassPathResource templateResource = new ClassPathResource("templates/default_voucher.pdf");
            if (!templateResource.exists()) {
                return Result.error("模板文件不存在(classpath): templates/default_voucher.pdf");
            }
            Random random = new Random();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateStr = LocalDateTime.now().format(dateFormatter);

            // 1. 清理旧的演示数据
            QueryWrapper<ArcFileContent> queryWrapper = new QueryWrapper<>();
            queryWrapper.likeRight("file_hash", "DEMO_HASH_");
            List<ArcFileContent> oldFiles = arcFileContentMapper.selectList(queryWrapper);
            
            // 删除元数据
            if (!oldFiles.isEmpty()) {
                List<String> oldFileIds = oldFiles.stream().map(ArcFileContent::getId).collect(Collectors.toList());
                arcFileMetadataIndexMapper.delete(new QueryWrapper<ArcFileMetadataIndex>().in("file_id", oldFileIds));
            }
            
            int deletedCount = arcFileContentMapper.delete(queryWrapper);
            log.info("已清理 {} 条旧演示数据", deletedCount);

            // 2. 生成新数据
            for (int i = 0; i < 10; i++) {
                String fileId = UUID.randomUUID().toString();
                String targetFileName = fileId + ".pdf";
                Path targetPath = Paths.get("/tmp/nexusarchive/uploads", targetFileName);
                
                // 确保目录存在
                Files.createDirectories(targetPath.getParent());
                
                // 复制模板文件 (从Classpath读取)
                try (java.io.InputStream is = templateResource.getInputStream()) {
                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                // 使用固定金额 (与模板 default_voucher.pdf 一致)
                // 凭证金额 = 借方合计 = 贷方合计 (不能双算)
                BigDecimal amount = new BigDecimal("43758.00");
                
                // 随机来源系统 (0-6)
                int sourceIndex = random.nextInt(SOURCE_SYSTEMS.length);
                
                // 创建记录
                ArcFileContent content = ArcFileContent.builder()
                        .id(fileId)
                        .archivalCode("TEMP-POOL-" + dateStr + "-" + fileId.substring(0, 8).toUpperCase())
                        .fileName("凭证_" + dateStr + "_" + (1000 + i) + ".pdf")
                        .fileType("PDF")
                        .fileSize(Files.size(targetPath))
                        .fileHash("DEMO_HASH_" + fileId.substring(0, 8) + "_" + sourceIndex) // 演示数据用伪哈希 + 来源索引
                        .hashAlgorithm("SHA-256")
                        .storagePath(targetPath.toString())
                        .createdTime(LocalDateTime.now().minusMinutes(random.nextInt(60)))
                        .build();
                
                arcFileContentMapper.insert(content);
                
                // 创建元数据索引 (包含金额)
                ArcFileMetadataIndex metadata = ArcFileMetadataIndex.builder()
                        .fileId(fileId)
                        .totalAmount(amount)
                        .invoiceNumber("INV-" + dateStr + "-" + (1000 + i))
                        .issueDate(java.time.LocalDate.now())
                        .sellerName("演示供应商 " + (char)('A' + random.nextInt(26)))
                        .parsedTime(LocalDateTime.now())
                        .parserType("DEMO_GENERATOR")
                        .build();
                arcFileMetadataIndexMapper.insert(metadata);
            }
            
            return Result.success("成功生成10条演示数据");
        } catch (Exception e) {
            log.error("生成演示数据失败", e);
            return Result.error("生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 转换实体为DTO
     */
    private PoolItemDto convertToPoolItemDto(ArcFileContent fileContent) {
        // 生成显示用的流水号 (去掉 TEMP- 前缀)
        String displayCode = fileContent.getArchivalCode().replace("TEMP-", "");
        
        // 查询元数据获取金额
        String amountStr = "-";
        ArcFileMetadataIndex metadata = arcFileMetadataIndexMapper.selectOne(
                new QueryWrapper<ArcFileMetadataIndex>().eq("file_id", fileContent.getId())
        );
        
        if (metadata != null && metadata.getTotalAmount() != null) {
            amountStr = metadata.getTotalAmount().toString();
        }
        
        // 解析来源系统
        String source = "Web上传";
        String fileHash = fileContent.getFileHash();
        if (fileHash != null && fileHash.startsWith("DEMO_HASH_")) {
            try {
                String[] parts = fileHash.split("_");
                if (parts.length >= 4) { // DEMO, HASH, ID, INDEX
                    int index = Integer.parseInt(parts[3]);
                    if (index >= 0 && index < SOURCE_SYSTEMS.length) {
                        source = SOURCE_SYSTEMS[index];
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return PoolItemDto.builder()
                .id(fileContent.getId())
                .code(displayCode)
                .source(source)
                .type(fileContent.getFileType())
                .amount(amountStr)
                .date(fileContent.getCreatedTime().format(FORMATTER))
                .status("已识别")  // 默认状态
                .build();
    }
}
