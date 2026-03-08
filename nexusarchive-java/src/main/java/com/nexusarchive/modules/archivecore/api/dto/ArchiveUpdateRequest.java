// Input: Lombok、Java 标准库
// Output: ArchiveUpdateRequest 类
// Pos: archivecore/api/dto
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.archivecore.api.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ArchiveUpdateRequest {

    private String fondsNo;

    private String archiveCode;

    private String categoryCode;

    private String title;

    @Pattern(regexp = "^\\d{4}$", message = "年度格式必须为4位数字")
    private String fiscalYear;

    private String fiscalPeriod;

    private String retentionPeriod;

    private LocalDate retentionStartDate;

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

    private String erpVoucherNo;

    private String voucherWord;
}
