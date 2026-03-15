// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: OfdConvertService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.ofd;

import com.nexusarchive.common.constants.OperationResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ConvertLog;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ConvertLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * OFD 格式转换服务
 * 将 PDF 转换为 OFD 格式长期保存 (符合 DA/T 94-2022)
 * 
 * @author Agent D (基础设施工程师)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfdConvertService {

    @Value("${archive.root.path:./data/archives}")
    private String archiveRootPath;

    private final ArcFileContentMapper fileContentMapper;
    private final ConvertLogMapper convertLogMapper;

    /**
     * 转换结果 DTO
     */
    public record ConvertResult(boolean success, String targetPath, String errorMessage, long durationMs) {
        public static ConvertResult success(String path, long duration) {
            return new ConvertResult(true, path, null, duration);
        }
        public static ConvertResult fail(String error) {
            return new ConvertResult(false, null, error, 0);
        }
    }

    /**
     * 将指定档案的 PDF 文件转换为 OFD 格式
     * 
     * @param archiveId 档案ID
     * @return 转换结果列表（一个档案可能有多个PDF附件）
     */
    public List<ConvertResult> convertArchivePdfs(String archiveId) {
        log.info("开始转换档案 PDF 到 OFD: archiveId={}", archiveId);
        
        List<ConvertResult> results = new ArrayList<>();
        
        // 查找该档案下的所有 PDF 文件
        LambdaQueryWrapper<ArcFileContent> query = new LambdaQueryWrapper<>();
        query.eq(ArcFileContent::getItemId, archiveId)
              .eq(ArcFileContent::getFileType, "PDF");
        
        List<ArcFileContent> pdfFiles = fileContentMapper.selectList(query);
        
        if (pdfFiles.isEmpty()) {
            log.warn("档案下没有 PDF 文件: archiveId={}", archiveId);
            results.add(ConvertResult.fail("No PDF files found for this archive"));
            return results;
        }
        
        for (ArcFileContent pdfFile : pdfFiles) {
            ConvertResult result = convertSinglePdf(pdfFile, archiveId);
            results.add(result);
        }
        
        return results;
    }

    /**
     * 转换单个 PDF 文件
     */
    public ConvertResult convertSinglePdf(ArcFileContent pdfFile, String archiveId) {
        long startTime = System.currentTimeMillis();
        String pdfPath = pdfFile.getStoragePath();
        
        try {
            // 计算绝对路径
            Path sourcePath = resolvePath(pdfPath);
            if (!Files.exists(sourcePath)) {
                throw new FileNotFoundException("PDF 文件不存在: " + sourcePath);
            }
            
            // 生成 OFD 文件路径
            String ofdFileName = sourcePath.getFileName().toString()
                .replaceAll("(?i)\\.pdf$", ".ofd");
            Path targetPath = sourcePath.getParent().resolve(ofdFileName);
            
            log.info("转换 PDF -> OFD: {} -> {}", sourcePath, targetPath);
            
            // 【合规修复】PDF→OFD 转换功能需要集成 ofdrw-graphics2d 库
            // 当前版本暂不支持真正转换，必须明确抛出异常而非伪造 OFD 文件
            // 依据：GB/T 33190-2016 要求 OFD 文件格式符合标准
            //
            // 后续实现计划：
            // 1. 使用 PDFBox PDFRenderer 将 PDF 页面渲染为图像
            // 2. 使用 ofdrw-graphics2d 将图像写入 OFD 页面
            // 3. 重新计算 OFD 文件 SM3 哈希
            
            throw new UnsupportedOperationException(
                "PDF→OFD 转换功能尚未实现。" +
                "请使用专业的 OFD 转换工具或联系系统管理员启用转换模块。" +
                "【合规提示】根据 GB/T 33190-2016，OFD 文件必须符合标准格式。");
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("PDF 转 OFD 失败: {}", pdfPath, e);
            
            // 记录失败日志
            saveConvertLog(archiveId, pdfPath, null, OperationResult.FAIL,
                e.getMessage(), duration, pdfFile.getFileSize(), null);
            
            return ConvertResult.fail(e.getMessage());
        }
    }

    /**
     * 批量异步转换 (后台任务)
     */
    @Async
    public void batchConvertAsync(List<String> archiveIds) {
        log.info("开始批量转换任务, 共 {} 个档案", archiveIds.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (String archiveId : archiveIds) {
            List<ConvertResult> results = convertArchivePdfs(archiveId);
            for (ConvertResult result : results) {
                if (result.success()) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
        }
        
        log.info("批量转换任务完成: 成功 {}, 失败 {}", successCount, failCount);
    }

    /**
     * 获取档案的 OFD 文件路径
     */
    public String getOfdPath(String archiveId, String pdfFileName) {
        LambdaQueryWrapper<ArcFileContent> query = new LambdaQueryWrapper<>();
        query.eq(ArcFileContent::getItemId, archiveId)
              .eq(ArcFileContent::getFileType, "OFD");
        
        List<ArcFileContent> ofdFiles = fileContentMapper.selectList(query);
        
        if (!ofdFiles.isEmpty()) {
            // 如果指定了原 PDF 文件名，找对应的 OFD
            if (pdfFileName != null) {
                String expectedOfdName = pdfFileName.replaceAll("(?i)\\.pdf$", ".ofd");
                for (ArcFileContent ofd : ofdFiles) {
                    if (ofd.getFileName().equalsIgnoreCase(expectedOfdName)) {
                        return ofd.getStoragePath();
                    }
                }
            }
            // 返回第一个 OFD
            return ofdFiles.get(0).getStoragePath();
        }
        
        return null;
    }

    /**
     * 获取转换日志
     */
    public List<ConvertLog> getConvertLogs(String archiveId) {
        LambdaQueryWrapper<ConvertLog> query = new LambdaQueryWrapper<>();
        query.eq(ConvertLog::getArchiveId, archiveId)
              .orderByDesc(ConvertLog::getConvertTime);
        return convertLogMapper.selectList(query);
    }

    // === 私有方法 ===

    private Path resolvePath(String storagePath) {
        if (storagePath.startsWith("/") || storagePath.contains(":")) {
            // 绝对路径
            return Paths.get(storagePath);
        } else {
            // 相对路径
            return Paths.get(archiveRootPath).resolve(storagePath);
        }
    }

    private void saveConvertLog(String archiveId, String sourcePath, String targetPath,
            String status, String errorMessage, Long duration, Long sourceSize, Long targetSize) {
        ConvertLog log = ConvertLog.builder()
            .id(UUID.randomUUID().toString().replace("-", ""))
            .archiveId(archiveId)
            .sourceFormat("PDF")
            .targetFormat("OFD")
            .sourcePath(sourcePath)
            .targetPath(targetPath)
            .status(status)
            .errorMessage(errorMessage)
            .durationMs(duration)
            .sourceSize(sourceSize)
            .targetSize(targetSize)
            .convertTime(LocalDateTime.now())
            .createdTime(LocalDateTime.now())
            .build();
        
        convertLogMapper.insert(log);
    }

    private void createOfdFileRecord(ArcFileContent pdfFile, String ofdPath) {
        try {
            Path path = Paths.get(ofdPath);
            String ofdFileName = path.getFileName().toString();
            long ofdSize = Files.size(path);
            
            ArcFileContent ofdFile = ArcFileContent.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .archivalCode(pdfFile.getArchivalCode())
                .itemId(pdfFile.getItemId())
                .fileName(ofdFileName)
                .fileType("OFD")
                .fileSize(ofdSize)
                .storagePath(ofdPath)
                .createdTime(LocalDateTime.now())
                .build();
            
            fileContentMapper.insert(ofdFile);
        } catch (Exception e) {
            log.error("创建 OFD 文件记录失败", e);
        }
    }
}
