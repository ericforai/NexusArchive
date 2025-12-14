package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

import java.util.List;

@Data
public class YonCollectionFileResponse {
    private String code;
    private String message;
    private List<FileItem> data;

    @Data
    public static class FileItem {
        private String downLoadUrl;
        private String fileName;
        private String id;
    }
}
