// Input: Lombok, Java Standard Library
// Output: BatchUploadResponse DTO
// Pos: DTO Layer

package com.nexusarchive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量上传响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResponse {

    private Long batchId;
    private String batchNo;
    private String status;
    private String uploadToken;
    private Integer totalFiles;
    private Integer uploadedFiles;
    private Integer failedFiles;
    private Integer progress;
    private List<FileInfo> recentFiles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private String originalFilename;
        private String uploadStatus;
        private Long fileSizeBytes;
        private String errorMessage;
    }
}
