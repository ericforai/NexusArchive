// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: SystemSetting 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统参数配置
 */
@Data
@TableName("sys_setting")
public class SystemSetting implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    private String description;

    /**
     * 分组/类别，如 system/storage/retention
     */
    private String category;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    private Integer deleted;
}
