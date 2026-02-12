// Input: Spring Framework, Local DTOs and Entities
// Output: CollectionBatchService Interface
// Pos: Service Interface Layer

package com.nexusarchive.service;

import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.dto.batch.BatchArchiveRequest;
import com.nexusarchive.dto.batch.BatchArchiveResponse;
import com.nexusarchive.entity.CollectionBatch;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资料收集批次服务接口
 */
public interface CollectionBatchService {

    /**
     * 创建上传批次
     *
     * @param request 批次创建请求
     * @param userId 用户ID
     * @return 批次上传响应
     */
    BatchUploadResponse createBatch(BatchUploadRequest request, String userId);

    /**
     * 上传单个文件到批次
     *
     * @param batchId 批次ID
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 文件上传结果
     */
    FileUploadResult uploadFile(Long batchId, MultipartFile file, String userId);

    /**
     * 完成批次上传
     *
     * @param batchId 批次ID
     * @param userId 用户ID
     * @return 批次完成结果
     */
    BatchCompleteResult completeBatch(Long batchId, String userId);

    /**
     * 取消批次
     *
     * @param batchId 批次ID
     * @param userId 用户ID
     */
    void cancelBatch(Long batchId, String userId);

    /**
     * 获取批次详情
     *
     * @param batchId 批次ID
     * @return 批次详情响应
     */
    BatchDetailResponse getBatchDetail(Long batchId);

    /**
     * 获取批次文件列表
     *
     * @param batchId 批次ID
     * @return 批次文件列表
     */
    List<BatchFileResponse> getBatchFiles(Long batchId);

    /**
     * 执行批次四性检测
     *
     * @param batchId 批次ID
     * @param userId 用户ID
     * @return 四性检测结果
     */
    BatchCheckResult runFourNatureCheck(Long batchId, String userId);

    /**
     * 文件上传结果记录
     */
    record FileUploadResult(
        String fileId,
        String originalFilename,
        String status,
        String errorMessage
    ) {}

    /**
     * 批次完成结果记录（包含四性检测统计）
     */
    record BatchCompleteResult(
        Long batchId,
        String batchNo,
        String status,
        int totalFiles,
        int uploadedFiles,
        int failedFiles,
        int checkedFiles,      // 新增：已检测文件数
        int passedFiles,       // 新增：检测通过数
        List<FailedFileInfo> failedFileList  // 新增：失败文件详情
    ) {
        /**
         * 兼容旧构造函数（向后兼容）
         */
        public BatchCompleteResult(Long batchId, String batchNo, String status,
                                   int totalFiles, int uploadedFiles, int failedFiles) {
            this(batchId, batchNo, status, totalFiles, uploadedFiles, failedFiles,
                 0, 0, List.of());
        }
    }

    /**
     * 失败文件信息记录
     */
    record FailedFileInfo(
        String fileId,
        String fileName,
        String reason
    ) {}

    /**
     * 批次详情响应记录
     */
    record BatchDetailResponse(
        Long id,
        String batchNo,
        String batchName,
        String fondsCode,
        String fiscalYear,
        String archivalCategory,
        String status,
        int totalFiles,
        int uploadedFiles,
        int failedFiles,
        long totalSizeBytes,
        int progress
    ) {}

    /**
     * 批次文件响应记录
     */
    record BatchFileResponse(
        Long id,
        String fileId,
        String originalFilename,
        String uploadStatus,
        Long fileSizeBytes,
        String errorMessage
    ) {}

    /**
     * 批次检测结果记录
     */
    record BatchCheckResult(
        Long batchId,
        int totalFiles,
        int checkedFiles,
        int passedFiles,
        int failedFiles,
        String summary
    ) {}

    /**
     * 批量批准批次归档
     *
     * @param request 批量批准请求
     * @return 批量操作响应
     */
    BatchArchiveResponse batchApprove(BatchArchiveRequest request);

    /**
     * 批量拒绝批次归档
     *
     * @param request 批量拒绝请求
     * @return 批量操作响应
     */
    BatchArchiveResponse batchReject(BatchArchiveRequest request);

    /**
     * 获取批次列表
     *
     * @param limit 限制数量
     * @param offset 偏移量
     * @param userId 用户ID
     * @return 批次列表
     */
    List<BatchDetailResponse> listBatches(int limit, int offset, String userId);
}
