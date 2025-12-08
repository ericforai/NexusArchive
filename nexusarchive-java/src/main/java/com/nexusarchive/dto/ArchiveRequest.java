package com.nexusarchive.dto;

import lombok.Data;
import java.util.List;

/**
 * 归档请求 DTO
 */
@Data
public class ArchiveRequest {
    /**
     * 凭证池记录 ID 列表
     */
    private List<String> poolItemIds;
}
