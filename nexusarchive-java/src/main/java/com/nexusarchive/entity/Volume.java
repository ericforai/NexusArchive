// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: Volume 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 案卷实体
 * 符合 DA/T 104-2024 组卷规范
 */
@Data
@TableName("acc_archive_volume")
public class Volume {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 案卷号 (格式: 全宗号-分类号-案卷号)
     * 例: BR01-AC01-0001
     */
    @jakarta.validation.constraints.NotBlank(message = "案卷号不能为空")
    private String volumeCode;

    /**
     * 案卷标题 (格式: 责任者+年度+月度+业务子系统+业务单据名称)
     * 例: 泊冉演示集团2025年08月会计凭证
     */
    private String title;

    /**
     * 全宗号
     */
    private String fondsNo;

    /**
     * 会计年度
     */
    private String fiscalYear;

    /**
     * 会计期间 (YYYY-MM)
     */
    private String fiscalPeriod;

    /**
     * 分类代号 (AC01=会计凭证, AC02=会计账簿, AC03=财务报告)
     */
    private String categoryCode;

    /**
     * 卷内文件数
     */
    private Integer fileCount;

    /**
     * 保管期限 (取卷内最长: 10Y, 30Y, PERMANENT)
     */
    private String retentionPeriod;

    /**
     * 状态: draft(草稿), pending(待审核), archived(已归档)
     */
    private String status;

    /**
     * 审核人ID
     */
    private String reviewedBy;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 归档时间
     */
    private LocalDateTime archivedAt;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("last_modified_time")
    private LocalDateTime lastModifiedTime;

    /**
     * 当前保管部门: ACCOUNTING(会计), ARCHIVES(档案)
     */
    @TableField("custodian_dept")
    private String custodianDept;
}
