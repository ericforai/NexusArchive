// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: FileHashDedupScope 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件哈希去重范围配置实体
 * 对应表: file_hash_dedup_scope
 */
@Data
@TableName("file_hash_dedup_scope")
public class FileHashDedupScope {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 全宗号
     */
    private String fondsNo;

    /**
     * 去重范围: SAME_FONDS(同全宗), AUTHORIZED(授权范围), GLOBAL(全局)
     */
    private String scopeType;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建人ID
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}


