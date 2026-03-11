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
@TableName("document_reminders")
public class DocumentReminderEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("project_id")
    private String projectId;

    @TableField("section_id")
    private String sectionId;

    @TableField("message")
    private String message;

    @TableField("remind_at")
    private LocalDateTime remindAt;

    @TableField("recipient_id")
    private String recipientId;

    @TableField("recipient_name")
    private String recipientName;

    @TableField("created_by")
    private String createdBy;

    @TableField("delivered")
    private Boolean delivered;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
