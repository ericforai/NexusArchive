// Input: Spring Framework, Lombok, Local Services
// Output: CollectionBatchServiceImpl
// Pos: Service Implementation

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.dto.batch.BatchArchiveRequest;
import com.nexusarchive.dto.batch.BatchArchiveResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.BasFondsMapper;
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
import org.springframework.dao.DuplicateKeyException;
import com.nexusarchive.security.FondsContext;
import com.nexusarchive.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final BasFondsMapper fondsMapper;
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
    public BatchUploadResponse createBatch(BatchUploadRequest request, String userId) {
        log.info("创建上传批次: userId={}, request={}", userId, request);

        // 1. 生成批次编号
        String batchNo = batchNumberGenerator.generateBatchNo();

        // 验证全宗权限
        String currentFonds = FondsContext.requireCurrentFondsNo();
        if (request.getFondsCode() != null && !currentFonds.equals(request.getFondsCode())) {
            throw new BusinessException(403, "越权操作：无法在非当前全宗下创建批次");
        }

        // 获取全宗 ID
        var fonds = fondsMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.nexusarchive.entity.BasFonds>()
                .eq(com.nexusarchive.entity.BasFonds::getFondsCode, currentFonds)
        );
        String fondsId = fonds != null ? fonds.getId() : "UNKNOWN";

        // 2. 创建批次记录
        CollectionBatch batch = CollectionBatch.builder()
            .batchNo(batchNo)
            .batchName(request.getBatchName())
            .fondsId(fondsId)
            .fondsCode(currentFonds)
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
            .createdTime(LocalDateTime.now())
            .lastModifiedTime(LocalDateTime.now())
            .build();

        batchMapper.insert(batch);

        // 3. 记录审计日志
        auditLogService.log(
            userId,
            userId,
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
    public FileUploadResult uploadFile(Long batchId, MultipartFile file, String userId) {
        log.info("上传文件: batchId={}, filename={}, size={}",
                 batchId, file.getOriginalFilename(), file.getSize());

        // 1. 验证批次状态
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "批次不存在");
        }
        
        // 全宗校验
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) {
            throw new BusinessException(403, "越权操作：非当前全宗数据");
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

        // 4.5 检查单次内同名文件 (防止数据库唯一索引冲突)
        // Root Cause Fix: 之前未检查文件名重复，导致插入时触发 idx_collection_batch_file_batch_name 唯一索引报错 (500)
        boolean nameExists = batchFileMapper.exists(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CollectionBatchFile>()
                .eq(CollectionBatchFile::getBatchId, batchId)
                .eq(CollectionBatchFile::getOriginalFilename, file.getOriginalFilename())
        );
        if (nameExists) {
            log.warn("检测到同名文件重复上传: batchId={}, filename={}", batchId, file.getOriginalFilename());
            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "当前批次已存在同名文件，请先删除旧文件或重命名");
        }

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
            .createdTime(LocalDateTime.now())
            .build();

        try {
            batchFileMapper.insert(batchFile);
        } catch (DuplicateKeyException e) {
            log.warn("并发上传冲突: batchId={}, filename={}", batchId, file.getOriginalFilename());
            return new FileUploadResult(null, file.getOriginalFilename(),
                "FAILED", "文件处理中或已存在，请勿重复操作");
        }

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
    public BatchCompleteResult completeBatch(Long batchId, String userId) {
        log.info("完成批次: batchId={}, userId={}", batchId, userId);

        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        // 全宗校验
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) {
            throw new BusinessException(403, "越权操作：非当前全宗数据");
        }

        // 更新批次状态
        batch.setStatus(CollectionBatch.STATUS_UPLOADED);
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        // 记录审计日志
        auditLogService.log(
            userId,
            userId,
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
    public void cancelBatch(Long batchId, String userId) {
        log.info("取消批次: batchId={}, userId={}", batchId, userId);

        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        // 全宗校验
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) {
            throw new BusinessException(403, "越权操作：非当前全宗数据");
        }

        batch.setStatus(CollectionBatch.STATUS_FAILED);
        batch.setErrorMessage("用户取消");
        batch.setLastModifiedTime(LocalDateTime.now());
        batch.setCompletedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        auditLogService.log(
            userId,
            userId,
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

        // 全宗校验
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) {
            throw new BusinessException(403, "越权操作：非当前全宗数据");
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
    public BatchCheckResult runFourNatureCheck(Long batchId, String userId) {
        log.info("执行批次四性检测: batchId={}, userId={}", batchId, userId);

        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }

        // 全宗校验
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) {
            throw new BusinessException(403, "越权操作：非当前全宗数据");
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

    @Override
    @Transactional
    public BatchArchiveResponse batchApprove(BatchArchiveRequest request) {
        log.info("批量批准批次归档: batchCount={}, operator={}",
                 request.getBatchIds().size(), request.getOperatorId());

        BatchArchiveResponse response = new BatchArchiveResponse();

        // 构建跳过ID集合（如果存在）
        Set<Long> skipIdSet = request.getSkipBatchIds() != null
                ? new HashSet<>(request.getSkipBatchIds())
                : new HashSet<>();

        // 遍历所有批次ID进行处理
        for (Long batchId : request.getBatchIds()) {
            // 跳过标记为跳过的记录
            if (skipIdSet.contains(batchId)) {
                log.info("Skipping batch: {}", batchId);
                continue;
            }

            try {
                // 调用单个批准方法
                approveBatch(batchId, request.getOperatorId(), request.getOperatorName(),
                            request.getComment());
                response.incrementSuccess();
                log.debug("Successfully approved batch: {}", batchId);
            } catch (Exception e) {
                // 记录失败，继续处理下一条
                CollectionBatch batch = batchMapper.selectById(batchId);
                String batchNo = batch != null ? batch.getBatchNo() : "UNKNOWN";
                response.addError(batchId, batchNo, e.getMessage());
                log.warn("Failed to approve batch {}: {}", batchId, e.getMessage(), e);
            }
        }

        log.info("Batch approval completed: {} succeeded, {} failed",
                 response.getSuccess(), response.getFailed());

        return response;
    }

    @Override
    @Transactional
    public BatchArchiveResponse batchReject(BatchArchiveRequest request) {
        log.info("批量拒绝批次归档: batchCount={}, operator={}",
                 request.getBatchIds().size(), request.getOperatorId());

        BatchArchiveResponse response = new BatchArchiveResponse();

        // 构建跳过ID集合（如果存在）
        Set<Long> skipIdSet = request.getSkipBatchIds() != null
                ? new HashSet<>(request.getSkipBatchIds())
                : new HashSet<>();

        // 遍历所有批次ID进行处理
        for (Long batchId : request.getBatchIds()) {
            // 跳过标记为跳过的记录
            if (skipIdSet.contains(batchId)) {
                log.info("Skipping batch: {}", batchId);
                continue;
            }

            try {
                // 调用单个拒绝方法
                rejectBatch(batchId, request.getOperatorId(), request.getOperatorName(),
                           request.getComment());
                response.incrementSuccess();
                log.debug("Successfully rejected batch: {}", batchId);
            } catch (Exception e) {
                // 记录失败，继续处理下一条
                CollectionBatch batch = batchMapper.selectById(batchId);
                String batchNo = batch != null ? batch.getBatchNo() : "UNKNOWN";
                response.addError(batchId, batchNo, e.getMessage());
                log.warn("Failed to reject batch {}: {}", batchId, e.getMessage(), e);
            }
        }

        log.info("Batch rejection completed: {} succeeded, {} failed",
                 response.getSuccess(), response.getFailed());

        return response;
    }

    // ===== Private Helper Methods =====

    /**
     * 批准单个批次归档
     */
    private void approveBatch(Long batchId, String operatorId, String operatorName, String comment) {
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }

        // 全宗校验
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) {
            throw new BusinessException(403, "越权操作：非当前全宗数据");
        }

        // 验证批次状态：只允许已验证的批次进行归档
        if (!CollectionBatch.STATUS_VALIDATED.equals(batch.getStatus())) {
            throw new IllegalStateException("批次状态不允许归档: " + batch.getStatus());
        }

        // 更新批次状态为已归档
        batch.setStatus(CollectionBatch.STATUS_ARCHIVED);
        batch.setLastModifiedTime(LocalDateTime.now());
        batch.setCompletedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        // 记录审计日志
        String operator = operatorId != null ? operatorId : "system";
        auditLogService.log(
            operator,
            operatorName != null ? operatorName : "system",
            "APPROVE_BATCH",
            "COLLECTION_BATCH",
            String.valueOf(batchId),
            "SUCCESS",
            "批准批次归档: " + batch.getBatchNo() + (comment != null ? ", 备注: " + comment : ""),
            null
        );

        log.info("批次归档批准成功: batchId={}, batchNo={}", batchId, batch.getBatchNo());
    }

    /**
     * 拒绝单个批次归档
     */
    private void rejectBatch(Long batchId, String operatorId, String operatorName, String comment) {
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }

        // 全宗校验
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) {
            throw new BusinessException(403, "越权操作：非当前全宗数据");
        }

        // 更新批次状态为失败
        batch.setStatus(CollectionBatch.STATUS_FAILED);
        batch.setErrorMessage(comment != null ? comment : "归档申请被拒绝");
        batch.setLastModifiedTime(LocalDateTime.now());
        batch.setCompletedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        // 记录审计日志
        String operator = operatorId != null ? operatorId : "system";
        auditLogService.log(
            operator,
            operatorName != null ? operatorName : "system",
            "REJECT_BATCH",
            "COLLECTION_BATCH",
            String.valueOf(batchId),
            "SUCCESS",
            "拒绝批次归档: " + batch.getBatchNo() + (comment != null ? ", 原因: " + comment : ""),
            null
        );

        log.info("批次归档拒绝成功: batchId={}, batchNo={}", batchId, batch.getBatchNo());
    }

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
            // 注意：batchId 外键指向 arc_archive_batch，不是 collection_batch
            // 采集阶段设为 null，归档时再关联到正确的 arc_archive_batch
            .batchId(null)
            .createdTime(LocalDateTime.now())
            .build();
    }
}
