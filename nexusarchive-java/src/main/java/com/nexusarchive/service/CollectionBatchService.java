// Input: Spring Framework, Local DTOs and Entities
// Output: CollectionBatchService Interface
// Pos: Service Interface Layer

package com.nexusarchive.service;

import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
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
    BatchUploadResponse createBatch(BatchUploadRequest request, Long userId);

    /**
     * 上传单个文件到批次
     *
     * @param batchId 批次ID
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 文件上传结果
     */
    FileUploadResult uploadFile(Long batchId, MultipartFile file, Long userId);

    /**
     * 完成批次上传
     *
     * @param batchId 批次ID
     * @param userId 用户ID
     * @return 批次完成结果
     */
    BatchCompleteResult completeBatch(Long batchId, Long userId);

    /**
     * 取消批次
     *
     * @param batchId 批次ID
     * @param userId 用户ID
     */
    void cancelBatch(Long batchId, Long userId);

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
    BatchCheckResult runFourNatureCheck(Long batchId, Long userId);

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
     * 批次完成结果记录
     */
    record BatchCompleteResult(
        Long batchId,
        String batchNo,
        String status,
        int totalFiles,
        int uploadedFiles,
        int failedFiles
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
}
