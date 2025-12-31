// Input: 四性检测记录实体
// Output: 领域对象
// Pos: NexusCore domain
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 长期保存四性检测记录
 */
@Data
@TableName("arc_preservation_audit")
public class PreservationAudit implements Serializable {
    @TableId
    private String id;

    @TableField("archive_id")
    private String archiveId; // 关联档案 ID

    @TableField("action_type")
    private String actionType; // ARCHIVE, PRESERVATION_CHECK

    @TableField("overall_status")
    private String overallStatus; // PASS, FAIL

    @TableField("check_result_json")
    private String checkResultJson; // 详细检测结果 (JSON)

    @TableField("operator")
    private String operator;

    @TableField("check_time")
    private LocalDateTime checkTime;
}
