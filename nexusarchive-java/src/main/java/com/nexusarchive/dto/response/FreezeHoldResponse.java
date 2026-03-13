// Input: Lombok
// Output: FreezeHoldResponse 类
// Pos: 响应 DTO 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 冻结保全响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreezeHoldResponse {

    /**
     * 冻结记录ID
     */
    private String id;

    /**
     * 档案ID
     */
    private String archiveId;

    /**
     * 档案编号
     */
    private String archiveCode;

    /**
     * 档案标题
     */
    private String archiveTitle;

    /**
     * 冻结状态 (FROZEN-冻结, HOLD-保全)
     */
    private String status;

    /**
     * 冻结原因
     */
    private String reason;

    /**
     * 冻结到期日期
     */
    private LocalDate expireDate;

    /**
     * 冻结人ID
     */
    private String frozenBy;

    /**
     * 冻结人姓名
     */
    private String frozenByName;

    /**
     * 冻结时间
     */
    private LocalDateTime frozenAt;

    /**
     * 解除人ID
     */
    private String releasedBy;

    /**
     * 解除人姓名
     */
    private String releasedByName;

    /**
     * 解除时间
     */
    private LocalDateTime releasedAt;

    /**
     * 解除原因
     */
    private String releaseReason;

    /**
     * 是否已解除
     */
    private Boolean isReleased;
}
