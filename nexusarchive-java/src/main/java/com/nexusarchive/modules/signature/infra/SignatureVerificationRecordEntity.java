package com.nexusarchive.modules.signature.infra;

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
@TableName(value = "arc_signature_verification", autoResultMap = true)
public class SignatureVerificationRecordEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("archive_id")
    private String archiveId;

    @TableField("file_id")
    private String fileId;

    @TableField("file_name")
    private String fileName;

    @TableField("document_type")
    private String documentType;

    @TableField("trigger_source")
    private String triggerSource;

    @TableField("provider_code")
    private String providerCode;

    @TableField("provider_version")
    private String providerVersion;

    @TableField("verification_status")
    private String verificationStatus;

    @TableField("signature_count")
    private Integer signatureCount;

    @TableField("valid_signature_count")
    private Integer validSignatureCount;

    @TableField("invalid_signature_count")
    private Integer invalidSignatureCount;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("verified_at")
    private LocalDateTime verifiedAt;

    @TableField(value = "result_payload", typeHandler = PostgresJsonTypeHandler.class)
    private String resultPayload;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
