// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: EntityConfig 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 法人配置表
 * 对应表: sys_entity_config
 * 
 * 说明: 每个法人可以有自己的独立配置（ERP接口、业务规则、合规策略等）
 */
@Data
@TableName("sys_entity_config")
public class EntityConfig {

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 法人ID
     */
    private String entityId;

    /**
     * 配置类型: ERP_INTEGRATION, BUSINESS_RULE, COMPLIANCE_POLICY
     */
    private String configType;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值（JSON格式）
     */
    private String configValue;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 创建人ID
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}



