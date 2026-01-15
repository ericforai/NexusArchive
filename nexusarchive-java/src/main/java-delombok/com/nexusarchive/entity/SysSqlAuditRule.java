// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: SysSqlAuditRule 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SQL 审计规则字典
 * 对应表: sys_sql_audit_rule
 */
@Data
@TableName("sys_sql_audit_rule")
public class SysSqlAuditRule implements Serializable {

    @TableId(value = "rule_key", type = IdType.INPUT)
    private String ruleKey;

    @TableField("rule_value")
    private String ruleValue;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;
}
