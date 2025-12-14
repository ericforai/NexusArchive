package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * YonSuite 附件列表响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YonAttachmentListResponse {

    private String code;
    private String message;
    private List<AttachmentData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttachmentData {
        private String id;
        private String fileName;
        private String fileExtension;
        private Long fileSize;
        private String filePath;
        private String url;
    }
}
