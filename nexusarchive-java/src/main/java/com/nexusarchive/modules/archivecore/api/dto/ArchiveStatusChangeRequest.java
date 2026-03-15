// Input: Jakarta Validation、Lombok、ArchiveStatus
// Output: ArchiveStatusChangeRequest 类
// Pos: archivecore/api/dto

package com.nexusarchive.modules.archivecore.api.dto;

import com.nexusarchive.common.enums.ArchiveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 档案状态变更请求 DTO
 */
@Data
public class ArchiveStatusChangeRequest {

    /**
     * 目标状态
     */
    @NotNull(message = "目标状态不能为空")
    private ArchiveStatus targetStatus;

    /**
     * 变更原因（可选，用于审计）
     */
    private String reason;

    /**
     * 预期的版本号（用于乐观锁校验）
     * <p>如果提供，将与档案当前版本比较，不匹配时抛出异常
     */
    private Integer expectedVersion;
}
