// Input: Spring Framework, Local Services
// Output: BatchFileProcessor 类
// Pos: Service Layer

package com.nexusarchive.service.collection;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.mapper.CollectionBatchMapper;
import com.nexusarchive.service.BatchToArchiveService;
import com.nexusarchive.util.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批次文件处理器
 *
 * 负责文件上传的核心处理逻辑
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BatchFileProcessor {

    private final CollectionBatchFileMapper batchFileMapper;
    private final CollectionBatchMapper batchMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final FileHashUtil fileHashUtil;
    private final BatchFileValidator fileValidator;
    private final BatchFileStorageService storageService;
    private final BatchToArchiveService batchToArchiveService;

    /**
     * 文件上传处理结果
     */
    public record FileProcessResult(
            String fileId,
            String filename,
            String status,
            String errorMessage
    ) {}

    /**
     * 处理文件上传
     *
     * @param batch 批次信息
     * @param file 上传的文件
     * @return 处理结果
     */
    @Transactional
    public FileProcessResult processUpload(CollectionBatch batch, MultipartFile file) {
        if (file.isEmpty()) {
            return new FileProcessResult(null, file.getOriginalFilename(),
                    "FAILED", "上传文件为空");
        }

        // 1. 计算文件哈希 (幂等性控制)
        String fileHash;
        try {
            fileHash = fileHashUtil.calculateSHA256(file.getInputStream());
        } catch (Exception e) {
            log.error("计算文件哈希失败", e);
            return new FileProcessResult(null, file.getOriginalFilename(),
                    "FAILED", "文件处理失败: " + e.getMessage());
        }

        // 2. 检查重复文件
        CollectionBatchFile duplicate = fileValidator.checkDuplicate(
                fileHash, batch.getFondsCode(), batch.getFiscalYear()
        );
        if (duplicate != null) {
            log.warn("检测到重复文件: hash={}", fileHash);
            return new FileProcessResult(null, file.getOriginalFilename(),
                    "DUPLICATE", "文件已存在 (相同哈希值)");
        }

        // 3. 确定文件类型
        String fileType = fileValidator.detectFileType(file.getOriginalFilename());

        // 4. 创建批次文件记录
        AtomicInteger uploadOrder = new AtomicInteger(
                batchFileMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CollectionBatchFile>()
                                .eq(CollectionBatchFile::getBatchId, batch.getId())
                ).intValue()
        );

        CollectionBatchFile batchFile = CollectionBatchFile.builder()
                .batchId(batch.getId())
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

        // 5. 保存文件到存储
        String fileId;
        try {
            String fileExtension = fileValidator.getExtension(batchFile.getOriginalFilename());
            fileId = storageService.saveFile(file, batch.getFondsCode(), batch.getFiscalYear(),
                    batch.getBatchNo(), fileExtension);
        } catch (Exception e) {
            log.error("保存文件失败", e);
            markUploadFailed(batchFile, e.getMessage());
            batchMapperUpdateStatistics(batch.getId());
            return new FileProcessResult(null, file.getOriginalFilename(),
                    "FAILED", "文件保存失败: " + e.getMessage());
        }

        // 6. 创建 arc_file_content 记录
        ArcFileContent arcFile = createArcFileContent(batchFile, batch, fileId);
        arcFileContentMapper.insert(arcFile);

        // 7. 创建档案记录
        Archive archive = batchToArchiveService.createArchiveFromBatch(arcFile, batch);

        // 8. 更新批次文件状态
        batchFile.setFileId(fileId);
        batchFile.setArchiveId(archive.getId());
        batchFile.setUploadStatus(CollectionBatchFile.STATUS_UPLOADED);
        batchFile.setCompletedTime(LocalDateTime.now());
        batchFileMapper.updateById(batchFile);

        // 9. 更新批次统计
        batchMapperUpdateStatistics(batch.getId());

        log.info("文件上传成功: fileId={}, batchFileId={}", fileId, batchFile.getId());
        return new FileProcessResult(fileId, file.getOriginalFilename(),
                "UPLOADED", null);
    }

    /**
     * 标记上传失败
     */
    private void markUploadFailed(CollectionBatchFile batchFile, String errorMessage) {
        batchFile.setUploadStatus(CollectionBatchFile.STATUS_FAILED);
        batchFile.setErrorMessage(errorMessage);
        batchFile.setCompletedTime(LocalDateTime.now());
        batchFileMapper.updateById(batchFile);
    }

    /**
     * 更新批次统计
     */
    private void batchMapperUpdateStatistics(Long batchId) {
        batchMapper.updateStatistics(batchId);
    }

    /**
     * 创建 ArcFileContent 记录
     */
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
