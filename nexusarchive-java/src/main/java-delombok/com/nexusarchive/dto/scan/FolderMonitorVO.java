// Input: Lombok, Java Time
// Output: FolderMonitorVO (文件夹监控响应VO)
// Pos: DTO Layer

package com.nexusarchive.dto.scan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件夹监控响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderMonitorVO {
    private Long id;
    private String folderPath;
    private Boolean isActive;
    private String fileFilter;
    private Boolean autoDelete;
    private String moveToPath;
    private LocalDateTime createdAt;
}
