// Input: Spring Framework, Lombok, Local Services
// Output: CollectionBatchServiceImpl
// Pos: Service Implementation

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.mapper.CollectionBatchMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.BatchToArchiveService;
import com.nexusarchive.service.CollectionBatchService;
import com.nexusarchive.service.PreArchiveCheckService;
import com.nexusarchive.service.collection.BatchFileStorageService;
import com.nexusarchive.service.collection.BatchFileValidator;
import com.nexusarchive.service.collection.BatchNumberGenerator;
import com.nexusarchive.util.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 资料收集批次服务实现
 *
 * <p>功能：</p>
 * <ol>
 *   <li>批次生命周期管理</li>
 *   <li>文件上传处理 (含幂等性控制)</li>
 *   <li>档案记录创建（上传后立即创建，符合 DA/T 94-2022）</li>
 *   <li>四性检测协调</li>
 *   <li>审计日志记录</li>
 * </ol>
 *
 * <p>合规性：上传的文件作为原始凭证附件处理，上传完成后立即创建 acc_archive 档案记录
 * (状态为 PENDING_METADATA)，确保元数据与文件同步捕获。</p>
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
    private final BatchToArchiveService batchToArchiveService;
    private final BatchNumberGenerator batchNumberGenerator;
    private final BatchFileValidator fileValidator;
    private final BatchFileStorageService storageService;

    // ===== CollectionBatchService Implementation =====

    @Override
    @Transactional
    public BatchUploadResponse createBatch(BatchUploadRequest request, Long userId) {
        log.info("创建上传批次: userId={}, request={}", userId, request);

        // 1. 生成批次编号
        String batchNo = batchNumberGenerator.generateBatchNo();

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
            .uploadToken(batchNumberGenerator.generateUploadToken(batch.getId(), userId))
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
        CollectionBatchFile duplicate = fileValidator.checkDuplicate(
            fileHash, batch.getFondsCode(), batch.getFiscalYear()
        );
        if (duplicate != null) {
            log.warn("检测到重复文件: hash={}", fileHash);
            return new FileUploadResult(null, file.getOriginalFilename(),
                "DUPLICATE", "文件已存在 (相同哈希值)");
        }

        // 4. 确定文件类型
        String fileType = fileValidator.detectFileType(file.getOriginalFilename());

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
            String fileExtension = fileValidator.getExtension(batchFile.getOriginalFilename());
            fileId = storageService.saveFile(file, batch.getFondsCode(), batch.getFiscalYear(),
                                           batch.getBatchNo(), fileExtension);
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
        ArcFileContent arcFile = createArcFileContent(batchFile, batch, fileId);
        arcFileContentMapper.insert(arcFile);

        // 8. 创建档案记录（符合 DA/T 94-2022 元数据同步捕获要求）
        Archive archive = batchToArchiveService.createArchiveFromBatch(arcFile, batch);

        // 9. 更新批次文件状态，关联档案ID
        batchFile.setFileId(fileId);
        batchFile.setArchiveId(archive.getId()); // 关联档案记录
        batchFile.setUploadStatus(CollectionBatchFile.STATUS_UPLOADED);
        batchFile.setCompletedTime(LocalDateTime.now());
        batchFileMapper.updateById(batchFile);

        // 10. 更新批次统计
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

    private ArcFileContent createArcFileContent(CollectionBatchFile batchFile, CollectionBatch batch,
                                                 String fileId) {
        String fileExtension = fileValidator.getExtension(batchFile.getOriginalFilename());
        String storagePath = storageService.buildStoragePathString(
            batch.getFondsCode(), batch.getFiscalYear(), batch.getBatchNo(), fileId, fileExtension
        );

        return ArcFileContent.builder()
            .id(fileId)
            .archivalCode(batch.getBatchNo() + "-" + batchFile.getUploadOrder())
            .fileName(batchFile.getOriginalFilename())
            .fileType(batchFile.getFileType())
            .fileSize(batchFile.getFileSizeBytes())
            .fileHash(batchFile.getFileHash())
            .hashAlgorithm(batchFile.getHashAlgorithm())
            .storagePath(storagePath)
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
