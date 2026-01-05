// Input: Spring Framework, Lombok, Local Services
// Output: CollectionBatchServiceImpl
// Pos: Service Implementation

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.mapper.CollectionBatchMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.CollectionBatchService;
import com.nexusarchive.service.PreArchiveCheckService;
import com.nexusarchive.util.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 资料收集批次服务实现
 *
 * 功能：
 * 1. 批次生命周期管理
 * 2. 文件上传处理 (含幂等性控制)
 * 3. 四性检测协调
 * 4. 审计日志记录
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionBatchServiceImpl implements CollectionBatchService {

    private final CollectionBatchMapper batchMapper;
    private final CollectionBatchFileMapper batchFileMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final FileHashUtil fileHashUtil;
    private final PreArchiveCheckService preArchiveCheckService;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter BATCH_NO_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd");

    // ===== CollectionBatchService Implementation =====

    @Override
    @Transactional
    public BatchUploadResponse createBatch(BatchUploadRequest request, Long userId) {
        log.info("创建上传批次: userId={}, request={}", userId, request);

        // 1. 生成批次编号
        String batchNo = generateBatchNo();

        // 2. 创建批次记录
        CollectionBatch batch = CollectionBatch.builder()
            .batchNo(batchNo)
            .batchName(request.getBatchName())
            .fondsCode(request.getFondsCode())
            .fiscalYear(request.getFiscalYear())
            .fiscalPeriod(request.getFiscalPeriod())
            .archivalCategory(request.getArchivalCategory())
            .sourceChannel("WEB上传")
            .status(CollectionBatch.STATUS_UPLOADING)
            .totalFiles(request.getTotalFiles())
            .uploadedFiles(0)
            .failedFiles(0)
            .totalSizeBytes(0L)
            .createdBy(userId)
            .build();

        batchMapper.insert(batch);

        // 3. 记录审计日志
        auditLogService.log(
            String.valueOf(userId),
            String.valueOf(userId),
            "CREATE_BATCH",
            "COLLECTION_BATCH",
            String.valueOf(batch.getId()),
            "SUCCESS",
            "创建上传批次: " + batchNo,
            null
        );

        // 4. 返回响应
        return BatchUploadResponse.builder()
            .batchId(batch.getId())
            .batchNo(batchNo)
            .status(batch.getStatus())
            .uploadToken(generateUploadToken(batch.getId(), userId))
            .totalFiles(request.getTotalFiles())
            .uploadedFiles(0)
            .failedFiles(0)
            .progress(0)
            .build();
    }

    @Override
    @Transactional
    public FileUploadResult uploadFile(Long batchId, MultipartFile file, Long userId) {
        log.info("上传文件: batchId={}, filename={}, size={}",
                 batchId, file.getOriginalFilename(), file.getSize());

        // 1. 验证批次状态
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "批次不存在");
        }
        if (!batch.canUpload()) {
            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "批次状态不允许上传: " + batch.getStatus());
        }

        // 2. 计算文件哈希 (幂等性控制)
        String fileHash;
        try {
            fileHash = fileHashUtil.calculateSHA256(file.getInputStream());
        } catch (Exception e) {
            log.error("计算文件哈希失败", e);
            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "文件处理失败: " + e.getMessage());
        }

        // 3. 检查重复文件 (同一全宗、同一年度内)
        // Use fondsCode as the identifier since fondsId may not be populated yet
        CollectionBatchFile duplicate = batchFileMapper.findDuplicateByHash(
            fileHash, batch.getFondsCode(), batch.getFiscalYear()
        );
        if (duplicate != null) {
            log.warn("检测到重复文件: hash={}", fileHash);
            return new FileUploadResult(null, file.getOriginalFilename(),
                "DUPLICATE", "文件已存在 (相同哈希值)");
        }

        // 4. 确定文件类型
        String fileType = getFileType(file.getOriginalFilename());

        // 5. 创建批次文件记录
        Long batchIdParam = batchId; // Rename to avoid lambda capture issue
        AtomicInteger uploadOrder = new AtomicInteger(
            batchFileMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CollectionBatchFile>()
                    .eq(CollectionBatchFile::getBatchId, batchIdParam)
            ).intValue()
        );

        CollectionBatchFile batchFile = CollectionBatchFile.builder()
            .batchId(batchId)
            .originalFilename(file.getOriginalFilename())
            .fileSizeBytes(file.getSize())
            .fileType(fileType)
            .fileHash(fileHash)
            .hashAlgorithm("SHA-256")
            .uploadStatus(CollectionBatchFile.STATUS_UPLOADING)
            .uploadOrder(uploadOrder.incrementAndGet())
            .startedTime(LocalDateTime.now())
            .build();

        batchFileMapper.insert(batchFile);

        // 6. 保存文件到存储
        String fileId;
        try {
            fileId = saveFileToStorage(file, batch, batchFile);
        } catch (Exception e) {
            log.error("保存文件失败", e);
            batchFile.setUploadStatus(CollectionBatchFile.STATUS_FAILED);
            batchFile.setErrorMessage(e.getMessage());
            batchFile.setCompletedTime(LocalDateTime.now());
            batchFileMapper.updateById(batchFile);

            // 更新批次统计
            batchMapper.updateStatistics(batchId);

            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "文件保存失败: " + e.getMessage());
        }

        // 7. 创建 arc_file_content 记录
        ArcFileContent arcFile = createArcFileContent(file, batch, batchFile, fileId, userId);
        arcFileContentMapper.insert(arcFile);

        // 8. 更新批次文件状态
        batchFile.setFileId(fileId);
        batchFile.setUploadStatus(CollectionBatchFile.STATUS_UPLOADED);
        batchFile.setCompletedTime(LocalDateTime.now());
        batchFileMapper.updateById(batchFile);

        // 9. 更新批次统计
        batchMapper.updateStatistics(batchId);

        log.info("文件上传成功: fileId={}, batchFileId={}", fileId, batchFile.getId());
        return new FileUploadResult(fileId, file.getOriginalFilename(),
            "UPLOADED", null);
    }

    @Override
    @Transactional
    public BatchCompleteResult completeBatch(Long batchId, Long userId) {
        log.info("完成批次: batchId={}, userId={}", batchId, userId);

        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        // 更新批次状态
        batch.setStatus(CollectionBatch.STATUS_UPLOADED);
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        // 记录审计日志
        auditLogService.log(
            String.valueOf(userId),
            String.valueOf(userId),
            "COMPLETE_BATCH",
            "COLLECTION_BATCH",
            String.valueOf(batchId),
            "SUCCESS",
            "完成批次上传: " + batch.getBatchNo(),
            null
        );

        return new BatchCompleteResult(
            batch.getId(),
            batch.getBatchNo(),
            batch.getStatus(),
            batch.getTotalFiles(),
            batch.getUploadedFiles(),
            batch.getFailedFiles()
        );
    }

    @Override
    @Transactional
    public void cancelBatch(Long batchId, Long userId) {
        log.info("取消批次: batchId={}, userId={}", batchId, userId);

        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        batch.setStatus(CollectionBatch.STATUS_FAILED);
        batch.setErrorMessage("用户取消");
        batch.setLastModifiedTime(LocalDateTime.now());
        batch.setCompletedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        auditLogService.log(
            String.valueOf(userId),
            String.valueOf(userId),
            "CANCEL_BATCH",
            "COLLECTION_BATCH",
            String.valueOf(batchId),
            "SUCCESS",
            "取消批次: " + batch.getBatchNo(),
            null
        );
    }

    @Override
    public BatchDetailResponse getBatchDetail(Long batchId) {
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        return new BatchDetailResponse(
            batch.getId(),
            batch.getBatchNo(),
            batch.getBatchName(),
            batch.getFondsCode(),
            batch.getFiscalYear(),
            batch.getArchivalCategory(),
            batch.getStatus(),
            batch.getTotalFiles(),
            batch.getUploadedFiles(),
            batch.getFailedFiles(),
            batch.getTotalSizeBytes(),
            batch.getProgress()
        );
    }

    @Override
    public List<BatchFileResponse> getBatchFiles(Long batchId) {
        List<CollectionBatchFile> files = batchFileMapper.findByBatchId(batchId);
        return files.stream()
            .map(f -> new BatchFileResponse(
                f.getId(),
                f.getFileId(),
                f.getOriginalFilename(),
                f.getUploadStatus(),
                f.getFileSizeBytes(),
                f.getErrorMessage()
            ))
            .toList();
    }

    @Override
    @Transactional
    public BatchCheckResult runFourNatureCheck(Long batchId, Long userId) {
        log.info("执行批次四性检测: batchId={}, userId={}", batchId, userId);

        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        // 更新批次状态
        batch.setStatus(CollectionBatch.STATUS_VALIDATING);
        batchMapper.updateById(batch);

        // 获取已上传的文件
        Long batchIdParam2 = batchId; // Rename to avoid lambda capture issue
        List<CollectionBatchFile> files = batchFileMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CollectionBatchFile>()
                .eq(CollectionBatchFile::getBatchId, batchIdParam2)
                .eq(CollectionBatchFile::getUploadStatus, CollectionBatchFile.STATUS_UPLOADED)
                .orderByAsc(CollectionBatchFile::getUploadOrder)
        );

        int totalFiles = files.size();
        int passedFiles = 0;
        int failedFiles = 0;

        for (CollectionBatchFile batchFile : files) {
            if (batchFile.getFileId() != null) {
                try {
                    var report = preArchiveCheckService.checkSingleFile(batchFile.getFileId());
                    if ("PASSED".equals(report.getStatus().name())) {
                        passedFiles++;
                        batchFile.setUploadStatus(CollectionBatchFile.STATUS_VALIDATED);
                    } else {
                        failedFiles++;
                        batchFile.setUploadStatus(CollectionBatchFile.STATUS_CHECK_FAILED);
                    }
                    batchFileMapper.updateById(batchFile);
                } catch (Exception e) {
                    log.error("四性检测失败: fileId={}", batchFile.getFileId(), e);
                    failedFiles++;
                }
            }
        }

        // 更新批次状态
        if (failedFiles == 0) {
            batch.setStatus(CollectionBatch.STATUS_VALIDATED);
        } else {
            batch.setStatus(CollectionBatch.STATUS_UPLOADED); // 允许部分失败后重试
        }
        batchMapper.updateById(batch);

        String summary = String.format("检测完成: 共 %d 个文件，通过 %d 个，失败 %d 个",
            totalFiles, passedFiles, failedFiles);

        return new BatchCheckResult(batchId, totalFiles, totalFiles, passedFiles, failedFiles, summary);
    }

    // ===== Private Helper Methods =====

    private String generateBatchNo() {
        String datePart = LocalDateTime.now().format(BATCH_NO_FORMATTER);
        String randomPart = String.format("%03d", (int)(Math.random() * 1000));
        return "COL-" + datePart + "-" + randomPart;
    }

    private String generateUploadToken(Long batchId, Long userId) {
        // 简单的令牌生成 (生产环境应使用JWT)
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String getFileType(String filename) {
        if (filename == null) return "UNKNOWN";
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "pdf" -> "PDF";
            case "ofd" -> "OFD";
            case "xml" -> "XML";
            case "jpg", "jpeg" -> "JPG";
            case "png" -> "PNG";
            case "tif", "tiff" -> "TIFF";
            default -> "UNKNOWN";
        };
    }

    private String saveFileToStorage(MultipartFile file, CollectionBatch batch,
                                      CollectionBatchFile batchFile) throws IOException {
        // 构建存储路径: /tmp/nexusarchive/uploads/{fondsCode}/{fiscalYear}/{batchNo}/
        String uploadDir = String.format("/tmp/nexusarchive/uploads/%s/%s/%s",
            batch.getFondsCode(), batch.getFiscalYear(), batch.getBatchNo());
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        // 生成唯一文件ID
        String fileId = UUID.randomUUID().toString();
        String fileExtension = getFileExtension(batchFile.getOriginalFilename());
        String targetFileName = fileId + "." + fileExtension;
        Path targetPath = uploadPath.resolve(targetFileName);

        // 保存文件
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return fileId;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "bin";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "bin";
    }

    private ArcFileContent createArcFileContent(MultipartFile file, CollectionBatch batch,
                                                 CollectionBatchFile batchFile, String fileId, Long userId) {
        return ArcFileContent.builder()
            .id(fileId)
            .archivalCode(batch.getBatchNo() + "-" + batchFile.getUploadOrder())
            .fileName(batchFile.getOriginalFilename())
            .fileType(batchFile.getFileType())
            .fileSize(batchFile.getFileSizeBytes())
            .fileHash(batchFile.getFileHash())
            .hashAlgorithm(batchFile.getHashAlgorithm())
            .storagePath(String.format("/tmp/nexusarchive/uploads/%s/%s/%s/%s.%s",
                batch.getFondsCode(), batch.getFiscalYear(), batch.getBatchNo(),
                fileId, getFileExtension(batchFile.getOriginalFilename())))
            .fiscalYear(batch.getFiscalYear())
            .voucherType(batch.getArchivalCategory())
            .fondsCode(batch.getFondsCode())
            .sourceSystem("WEB上传")
            .preArchiveStatus("PENDING_CHECK")
            .batchId(batch.getId())
            .createdTime(LocalDateTime.now())
            .build();
    }
}
