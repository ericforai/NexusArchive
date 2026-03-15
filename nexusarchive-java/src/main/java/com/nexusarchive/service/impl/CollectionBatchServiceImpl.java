// Input: Spring Framework, Lombok, Local Services
// Output: CollectionBatchServiceImpl
// Pos: Service Implementation

package com.nexusarchive.service.impl;

import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.dto.batch.BatchArchiveRequest;
import com.nexusarchive.dto.batch.BatchArchiveResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.entity.enums.PreArchiveStatus;
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
import com.nexusarchive.service.collection.CollectionMetadataInheritor;
import com.nexusarchive.service.collection.FourNatureCheckHelper;
import com.nexusarchive.service.collection.FourNatureCheckHelper.BatchCheckStatistics;
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
    private final CollectionMetadataInheritor metadataInheritor;
    private final com.nexusarchive.service.helper.CollectionBatchHelper helper;

    // ===== CollectionBatchService Implementation =====

    @Override
    @Transactional
    public BatchUploadResponse createBatch(BatchUploadRequest request, String userId) {
        log.info("创建上传批次: userId={}, request={}", userId, request);
        String batchNo = batchNumberGenerator.generateBatchNo();
        String currentFonds = FondsContext.requireCurrentFondsNo();
        
        if (request.getFondsCode() != null && !currentFonds.equals(request.getFondsCode())) {
            throw new BusinessException(403, "越权操作：无法在非当前全宗下创建批次");
        }

        var fonds = fondsMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.nexusarchive.entity.BasFonds>()
                .eq(com.nexusarchive.entity.BasFonds::getFondsCode, currentFonds));
        String fondsId = fonds != null ? fonds.getId() : OperationResult.UNKNOWN;

        CollectionBatch batch = helper.createInitialBatch(request, batchNo, fondsId, currentFonds, userId);
        batchMapper.insert(batch);

        auditLogService.log(userId, userId, "CREATE_BATCH", "COLLECTION_BATCH", String.valueOf(batch.getId()), OperationResult.SUCCESS, "创建上传批次: " + batchNo, null);

        return BatchUploadResponse.builder()
            .batchId(batch.getId()).batchNo(batchNo).status(batch.getStatus())
            .uploadToken(batchNumberGenerator.generateUploadToken(batch.getId(), userId))
            .totalFiles(request.getTotalFiles()).uploadedFiles(0).failedFiles(0).progress(0).build();
    }

    @Override
    @Transactional
    public FileUploadResult uploadFile(Long batchId, MultipartFile file, String userId) {
        log.info("上传文件: batchId={}, filename={}", batchId, file.getOriginalFilename());
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) return new FileUploadResult(null, file.getOriginalFilename(), "FAILED", "批次不存在");
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) throw new BusinessException(403, "越权操作");
        if (!batch.canUpload()) return new FileUploadResult(null, file.getOriginalFilename(), "FAILED", "状态错误");

        String fileHash;
        try {
            fileHash = fileHashUtil.calculateSHA256(file.getInputStream());
        } catch (Exception e) {
            return new FileUploadResult(null, file.getOriginalFilename(), OperationResult.FAIL, "哈希计算失败");
        }

        if (fileValidator.checkDuplicate(fileHash, batch.getFondsCode(), batch.getFiscalYear()) != null) {
            return new FileUploadResult(null, file.getOriginalFilename(), "DUPLICATE", "文件已存在");
        }

        if (batchFileMapper.exists(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CollectionBatchFile>()
                .eq(CollectionBatchFile::getBatchId, batchId).eq(CollectionBatchFile::getOriginalFilename, file.getOriginalFilename()))) {
            return new FileUploadResult(null, file.getOriginalFilename(), OperationResult.FAIL, "同名文件已存在");
        }

        int order = batchFileMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CollectionBatchFile>()
                .eq(CollectionBatchFile::getBatchId, batchId)).intValue();

        CollectionBatchFile batchFile = CollectionBatchFile.builder()
            .batchId(batchId).originalFilename(file.getOriginalFilename()).fileSizeBytes(file.getSize())
            .fileType(fileValidator.detectFileType(file.getOriginalFilename())).fileHash(fileHash)
            .hashAlgorithm("SHA-256").uploadStatus(CollectionBatchFile.STATUS_UPLOADING)
            .uploadOrder(order + 1).startedTime(LocalDateTime.now()).createdTime(LocalDateTime.now()).build();

        batchFileMapper.insert(batchFile);

        String fileId;
        try {
            String ext = fileValidator.getExtension(batchFile.getOriginalFilename());
            fileId = storageService.saveFile(file, batch.getFondsCode(), batch.getFiscalYear(), batch.getBatchNo(), ext);
        } catch (Exception e) {
            batchFile.setUploadStatus(CollectionBatchFile.STATUS_FAILED);
            batchFile.setErrorMessage(e.getMessage());
            batchFileMapper.updateById(batchFile);
            batchMapper.updateStatistics(batchId);
            return new FileUploadResult(null, file.getOriginalFilename(), OperationResult.FAIL, "保存失败");
        }

        String path = storageService.buildStoragePathString(batch.getFondsCode(), batch.getFiscalYear(), batch.getBatchNo(), fileId, fileValidator.getExtension(batchFile.getOriginalFilename()));
        ArcFileContent arcFile = helper.createArcFileContent(batchFile, batch, fileId, path);
        arcFileContentMapper.insert(arcFile);

        Archive archive = batchToArchiveService.createArchiveFromBatch(arcFile, batch);
        batchFile.setFileId(fileId);
        batchFile.setArchiveId(archive.getId());
        batchFile.setUploadStatus(CollectionBatchFile.STATUS_UPLOADED);
        batchFile.setCompletedTime(LocalDateTime.now());
        batchFileMapper.updateById(batchFile);
        batchMapper.updateStatistics(batchId);

        return new FileUploadResult(fileId, file.getOriginalFilename(), "UPLOADED", null);
    }

    @Override
    @Transactional
    public BatchCompleteResult completeBatch(Long batchId, String userId) {
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) throw new IllegalArgumentException("批次不存在");
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) throw new BusinessException(403, "越权操作");
        if (!batch.getCreatedBy().equals(userId)) throw new BusinessException(403, "无权操作");

        batch.setStatus(CollectionBatch.STATUS_UPLOADED);
        batchMapper.updateById(batch);

        List<CollectionBatchFile> files = batchFileMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CollectionBatchFile>()
                .eq(CollectionBatchFile::getBatchId, batchId).isNotNull(CollectionBatchFile::getFileId));

        BatchCheckStatistics checkResult = helper.executeBatchCheck(files, batch);
        auditLogService.log(userId, userId, "COMPLETE_BATCH", "COLLECTION_BATCH", String.valueOf(batchId), OperationResult.SUCCESS, "完成批次上传", null);

        return new BatchCompleteResult(batch.getId(), batch.getBatchNo(), batch.getStatus(), batch.getTotalFiles(),
            batch.getUploadedFiles(), batch.getFailedFiles(), checkResult.getCheckedCount(), checkResult.getPassedCount(), checkResult.getFailedFiles());
    }

    @Override
    @Transactional
    public void cancelBatch(Long batchId, String userId) {
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) throw new IllegalArgumentException("批次不存在");
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) throw new BusinessException(403, "越权操作");

        batch.setStatus(CollectionBatch.STATUS_FAILED);
        batch.setErrorMessage("用户取消");
        batch.setCompletedTime(LocalDateTime.now());
        batchMapper.updateById(batch);
        auditLogService.log(userId, userId, "CANCEL_BATCH", "COLLECTION_BATCH", String.valueOf(batchId), OperationResult.SUCCESS, "取消批次", null);
    }

    @Override
    public BatchDetailResponse getBatchDetail(Long batchId) {
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) throw new IllegalArgumentException("批次不存在");
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) throw new BusinessException(403, "越权操作");
        return helper.mapToDetail(batch);
    }

    @Override
    public List<BatchFileResponse> getBatchFiles(Long batchId) {
        return helper.mapToFiles(batchFileMapper.findByBatchId(batchId));
    }

    @Override
    @Transactional
    public BatchCheckResult runFourNatureCheck(Long batchId, String userId) {
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) throw new IllegalArgumentException("批次不存在");
        if (!FondsContext.requireCurrentFondsNo().equals(batch.getFondsCode())) throw new BusinessException(403, "越权操作");

        batch.setStatus(CollectionBatch.STATUS_VALIDATING);
        batchMapper.updateById(batch);

        List<CollectionBatchFile> files = batchFileMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CollectionBatchFile>()
                .eq(CollectionBatchFile::getBatchId, batchId).eq(CollectionBatchFile::getUploadStatus, CollectionBatchFile.STATUS_UPLOADED));

        BatchCheckStatistics stats = helper.executeBatchCheck(files, batch);
        batch.setStatus(stats.getFailedCount() == 0 ? CollectionBatch.STATUS_VALIDATED : CollectionBatch.STATUS_UPLOADED);
        batchMapper.updateById(batch);

        return new BatchCheckResult(batchId, files.size(), files.size(), stats.getPassedCount(), stats.getFailedCount(), "检测完成");
    }

    @Override
    @Transactional
    public BatchArchiveResponse batchApprove(BatchArchiveRequest request) {
        return processBatchAction(request, true);
    }

    @Override
    @Transactional
    public BatchArchiveResponse batchReject(BatchArchiveRequest request) {
        return processBatchAction(request, false);
    }

    private BatchArchiveResponse processBatchAction(BatchArchiveRequest request, boolean approve) {
        BatchArchiveResponse response = new BatchArchiveResponse();
        Set<Long> skipIds = request.getSkipBatchIds() != null ? new HashSet<>(request.getSkipBatchIds()) : new HashSet<>();
        for (Long id : request.getBatchIds()) {
            if (skipIds.contains(id)) continue;
            try {
                if (approve) approveBatch(id, request.getOperatorId(), request.getOperatorName(), request.getComment());
                else rejectBatch(id, request.getOperatorId(), request.getOperatorName(), request.getComment());
                response.incrementSuccess();
            } catch (Exception e) {
                CollectionBatch b = batchMapper.selectById(id);
                response.addError(id, b != null ? b.getBatchNo() : OperationResult.UNKNOWN, e.getMessage());
            }
        }
        return response;
    }

    private void approveBatch(Long id, String opId, String opName, String comment) {
        CollectionBatch b = batchMapper.selectById(id);
        if (b == null || !FondsContext.requireCurrentFondsNo().equals(b.getFondsCode())) throw new BusinessException(403, "越权或不存在");
        if (!CollectionBatch.STATUS_VALIDATED.equals(b.getStatus())) throw new IllegalStateException("状态错误");
        b.setStatus(CollectionBatch.STATUS_ARCHIVED);
        b.setCompletedTime(LocalDateTime.now());
        batchMapper.updateById(b);
        auditLogService.log(opId != null ? opId : "system", opName, "APPROVE_BATCH", "COLLECTION_BATCH", String.valueOf(id), OperationResult.SUCCESS, "批准归档", null);
    }

    private void rejectBatch(Long id, String opId, String opName, String comment) {
        CollectionBatch b = batchMapper.selectById(id);
        if (b == null || !FondsContext.requireCurrentFondsNo().equals(b.getFondsCode())) throw new BusinessException(403, "越权或不存在");
        b.setStatus(CollectionBatch.STATUS_FAILED);
        b.setErrorMessage(comment);
        b.setCompletedTime(LocalDateTime.now());
        batchMapper.updateById(b);
        auditLogService.log(opId != null ? opId : "system", opName, "REJECT_BATCH", "COLLECTION_BATCH", String.valueOf(id), OperationResult.SUCCESS, "拒绝归档", null);
    }

    @Override
    public List<BatchDetailResponse> listBatches(int limit, int offset, String userId) {
        String currentFonds = FondsContext.getCurrentFondsNo();
        if (currentFonds == null || currentFonds.isBlank()) return List.of();
        List<CollectionBatch> batches = batchMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CollectionBatch>()
                .eq(CollectionBatch::getFondsCode, currentFonds).orderByDesc(CollectionBatch::getCreatedTime)
                .last("LIMIT " + Math.max(0, Math.min(limit, 1000)) + " OFFSET " + Math.max(0, offset)));
        return batches.stream().map(helper::mapToDetail).toList();
    }
}
