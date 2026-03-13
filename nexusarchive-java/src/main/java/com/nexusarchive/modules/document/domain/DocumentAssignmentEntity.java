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
@TableName("document_assignments")
public class DocumentAssignmentEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("project_id")
    private String projectId;

    @TableField("section_id")
    private String sectionId;

    @TableField("assignee_id")
    private String assigneeId;

    @TableField("assignee_name")
    private String assigneeName;

    @TableField("assigned_by")
    private String assignedBy;

    @TableField("note")
    private String note;

    @TableField("active")
    private Boolean active;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
