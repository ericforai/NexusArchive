// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: EmployeeLifecycleEvent 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工生命周期事件实体
 * 对应表: employee_lifecycle_event
 */
@Data
@TableName("employee_lifecycle_event")
public class EmployeeLifecycleEvent {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 员工ID
     */
    private String employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 事件类型: ONBOARD(入职), OFFBOARD(离职), TRANSFER(调岗)
     */
    private String eventType;

    /**
     * 事件日期
     */
    private LocalDate eventDate;

    /**
     * 组织ID（入职/调岗时使用）
     * 数据库列名：previous_dept_id
     */
    @TableField(value = "previous_dept_id")
    private String organizationId;

    /**
     * 目标组织ID（调岗时使用）
     * 数据库列名：new_dept_id
     */
    @TableField(value = "new_dept_id")
    private String toOrganizationId;

    /**
     * 原角色ID列表（JSON格式）
     */
    private String previousRoleIds;

    /**
     * 新角色ID列表（JSON格式）
     */
    private String newRoleIds;

    /**
     * 原因说明
     */
    private String reason;

    /**
     * 是否已处理
     */
    private Boolean processed;

    /**
     * 处理时间
     */
    private LocalDateTime processedAt;

    /**
     * 处理人ID
     */
    private String processedBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}

