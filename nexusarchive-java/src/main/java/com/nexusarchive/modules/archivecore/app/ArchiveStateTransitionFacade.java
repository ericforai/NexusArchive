// Input: ArchiveStatusChangeRequest、ArchiveStatus
// Output: ArchiveStateTransitionFacade 接口
// Pos: archivecore/app

package com.nexusarchive.modules.archivecore.app;

import com.nexusarchive.common.enums.ArchiveStatus;
import com.nexusarchive.modules.archivecore.api.dto.ArchiveStatusChangeRequest;

import java.util.List;

/**
 * 档案状态转换门面接口
 * <p>统一管理档案状态转换业务规则
 * <p>遵循 DA/T 94-2022 档案管理规范中的状态管理要求
 *
 * @see <a href="https://dat94.github.io/DA-T-94-2022">DA/T 94-2022 电子会计档案管理规范</a>
 */
public interface ArchiveStateTransitionFacade {

    /**
     * 转换档案状态
     *
     * @param archiveId 档案ID
     * @param request 状态变更请求
     * @param userId 操作人ID
     * @throws com.nexusarchive.common.exception.BusinessException 如果状态转换不合法或版本冲突
     */
    void transitionStatus(String archiveId, ArchiveStatusChangeRequest request, String userId);

    /**
     * 批量转换档案状态
     * <p>批量操作中单个失败不会中断整个操作，失败的档案会被跳过
     *
     * @param archiveIds 档案ID列表
     * @param targetStatus 目标状态
     * @param userId 操作人ID
     * @return 成功转换的数量
     */
    int batchTransitionStatus(List<String> archiveIds, ArchiveStatus targetStatus, String userId);

    /**
     * 检查状态转换是否合法
     *
     * @param archiveId 档案ID
     * @param targetStatus 目标状态
     * @return 是否可以转换
     */
    boolean canTransition(String archiveId, ArchiveStatus targetStatus);
}
