// Input: Jakarta Validation, Lombok
// Output: FolderMonitorRequest (文件夹监控请求DTO)
// Pos: DTO Layer

package com.nexusarchive.dto.scan;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文件夹监控请求DTO
 */
@Data
public class FolderMonitorRequest {

    /**
     * 监控文件夹路径
     */
    @NotBlank(message = "文件夹路径不能为空")
    private String folderPath;

    /**
     * 文件过滤器（如 *.pdf;*.jpg）
     */
    private String fileFilter = "*.pdf;*.jpg;*.jpeg;*.png";

    /**
     * 处理后是否自动删除源文件
     */
    private Boolean autoDelete = false;

    /**
     * 处理后移动到的目标路径
     */
    private String moveToPath;
}
