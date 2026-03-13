package com.nexusarchive.modules.document.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("document_locks")
public class DocumentLockEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("project_id")
    private String projectId;

    @TableField("section_id")
    private String sectionId;

    @TableField("locked_by")
    private String lockedBy;

    @TableField("locked_by_name")
    private String lockedByName;

    @TableField("reason")
    private String reason;

    @TableField("active")
    private Boolean active;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
