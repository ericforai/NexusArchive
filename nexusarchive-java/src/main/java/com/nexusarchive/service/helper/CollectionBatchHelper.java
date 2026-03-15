// Input: MyBatis-Plus, Local DTOs/Entities, MetadataInheritor
// Output: CollectionBatchHelper (采集批次辅助类)
// Pos: Service Helper Layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.service.CollectionBatchService.BatchDetailResponse;
import com.nexusarchive.service.CollectionBatchService.BatchFileResponse;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.service.PreArchiveCheckService;
import com.nexusarchive.service.collection.CollectionMetadataInheritor;
import com.nexusarchive.service.collection.FourNatureCheckHelper;
import com.nexusarchive.service.collection.FourNatureCheckHelper.BatchCheckStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionBatchHelper {

    private final ArcFileContentMapper arcFileContentMapper;
    private final CollectionMetadataInheritor metadataInheritor;
    private final PreArchiveCheckService preArchiveCheckService;
    private final CollectionBatchFileMapper batchFileMapper;

    public BatchCheckStatistics executeBatchCheck(List<CollectionBatchFile> batchFiles, CollectionBatch batch) {
        BatchCheckStatistics result = new BatchCheckStatistics();
        for (CollectionBatchFile bf : batchFiles) {
            try {
                ArcFileContent file = arcFileContentMapper.selectById(bf.getFileId());
                if (file == null) continue;

                if (metadataInheritor.inheritMissingMetadata(file, batch)) {
                    arcFileContentMapper.updateById(file);
                }

                preArchiveCheckService.checkSingleFile(file.getId());
                ArcFileContent updated = arcFileContentMapper.selectById(file.getId());
                String status = (updated != null) ? updated.getPreArchiveStatus() : file.getPreArchiveStatus();

                if (PreArchiveStatus.READY_TO_ARCHIVE.getCode().equals(status)) {
                    result.addPassed(file.getId(), file.getFileName());
                } else {
                    result.addFailed(file.getId(), file.getFileName(), "检测未通过");
                }
            } catch (Exception e) {
                log.error("Check failed: {}", bf.getFileId(), e);
                result.addFailed(bf.getFileId(), "未知", "检测异常: " + e.getMessage());
            }
        }
        return result;
    }

    public ArcFileContent createArcFileContent(CollectionBatchFile bf, CollectionBatch b, String fileId, String storagePath) {
        return ArcFileContent.builder()
            .id(fileId).archivalCode(b.getBatchNo() + "-" + bf.getUploadOrder())
            .fileName(bf.getOriginalFilename()).fileType(bf.getFileType())
            .fileSize(bf.getFileSizeBytes()).fileHash(bf.getFileHash())
            .hashAlgorithm(bf.getHashAlgorithm()).storagePath(storagePath)
            .fiscalYear(b.getFiscalYear()).voucherType(b.getArchivalCategory())
            .fondsCode(b.getFondsCode()).sourceSystem(com.nexusarchive.common.constants.ArchiveConstants.SourceChannel.WEB_UPLOAD)
            .preArchiveStatus(com.nexusarchive.common.constants.StatusConstants.PreArchive.PENDING_CHECK).batchId(null).createdTime(LocalDateTime.now())
            .build();
    }

    public BatchDetailResponse mapToDetail(CollectionBatch b) {
        return new BatchDetailResponse(
            b.getId(), b.getBatchNo(), b.getBatchName(), b.getFondsCode(),
            b.getFiscalYear(), b.getArchivalCategory(), b.getStatus(),
            b.getTotalFiles(), b.getUploadedFiles(), b.getFailedFiles(),
            b.getTotalSizeBytes(), b.getProgress()
        );
    }

    public List<BatchFileResponse> mapToFiles(List<CollectionBatchFile> files) {
        return files.stream().map(f -> new BatchFileResponse(
            f.getId(), f.getFileId(), f.getOriginalFilename(),
            f.getUploadStatus(), f.getFileSizeBytes(), f.getErrorMessage()
        )).toList();
    }
    
    public CollectionBatch createInitialBatch(BatchUploadRequest req, String batchNo, String fondsId, String currentFonds, String userId) {
        return CollectionBatch.builder()
            .batchNo(batchNo).batchName(req.getBatchName())
            .fondsId(fondsId).fondsCode(currentFonds)
            .fiscalYear(req.getFiscalYear()).fiscalPeriod(req.getFiscalPeriod())
            .archivalCategory(req.getArchivalCategory()).sourceChannel(com.nexusarchive.common.constants.ArchiveConstants.SourceChannel.WEB_UPLOAD)
            .status(CollectionBatch.STATUS_UPLOADING).totalFiles(req.getTotalFiles())
            .uploadedFiles(0).failedFiles(0).totalSizeBytes(0L)
            .createdBy(userId).createdTime(LocalDateTime.now())
            .lastModifiedTime(LocalDateTime.now()).build();
    }
}
