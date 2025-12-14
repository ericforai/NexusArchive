package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

import java.util.List;

@Data
public class YonCollectionFileRequest {
    private List<String> fileId;
}
