package com.nexusarchive.modules.document.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexusarchive.config.PostgresJsonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "document_versions", autoResultMap = true)
public class DocumentVersionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("project_id")
    private String projectId;

    @TableField("version_name")
    private String versionName;

    @TableField("description")
    private String description;

    @TableField("created_by")
    private String createdBy;

    @TableField("rolled_back_by")
    private String rolledBackBy;

    @TableField("rolled_back_at")
    private LocalDateTime rolledBackAt;

    @TableField(value = "snapshot_payload", typeHandler = PostgresJsonTypeHandler.class)
    private String snapshotPayload;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
