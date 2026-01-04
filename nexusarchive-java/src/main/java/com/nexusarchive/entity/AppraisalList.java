// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: AppraisalList 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 鉴定清单实体
 * 对应表: biz_appraisal_list
 */
@Data
@TableName("biz_appraisal_list")
public class AppraisalList {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 全宗号
     */
    private String fondsNo;

    /**
     * 归档年度
     */
    private Integer archiveYear;

    /**
     * 鉴定人ID
     */
    private String appraiserId;

    /**
     * 鉴定人姓名
     */
    private String appraiserName;

    /**
     * 鉴定日期
     */
    private LocalDate appraisalDate;

    /**
     * 待鉴定档案ID列表 (JSON数组)
     */
    private String archiveIds;

    /**
     * 档案元数据快照 (JSON格式)
     */
    private String archiveSnapshot;

    /**
     * 状态: PENDING(待提交), SUBMITTED(已提交)
     */
    private String status;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    private Integer deleted;
}



