// Input: Jakarta Validation、Lombok、Java 标准库
// Output: ArchiveCreateRequest 类
// Pos: archivecore/api/dto
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.archivecore.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ArchiveCreateRequest {

    @NotBlank(message = "全宗号不能为空")
    @Size(max = 50, message = "全宗号长度不能超过50")
    private String fondsNo;

    private String archiveCode;

    private String categoryCode;

    @NotBlank(message = "题名不能为空")
    private String title;

    @NotBlank(message = "年度不能为空")
    @Pattern(regexp = "^\\d{4}$", message = "年度格式必须为4位数字")
    private String fiscalYear;

    private String fiscalPeriod;

    @NotBlank(message = "保管期限不能为空")
    private String retentionPeriod;

    private LocalDate retentionStartDate;

    @NotBlank(message = "立档单位名称不能为空")
    private String orgName;

    private String creator;

    private String summary;

    private String status;

    private String standardMetadata;

    private String customMetadata;

    private String securityLevel;

    private String location;

    private String departmentId;

    private String fixityValue;

    private String fixityAlgo;

    private String uniqueBizId;

    private BigDecimal amount;

    private LocalDate docDate;

    private String volumeId;

    private String paperRefLink;

    private Boolean destructionHold;

    private String holdReason;

    private Integer matchScore;

    private String destructionStatus;

    private String matchMethod;
}
