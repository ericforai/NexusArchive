// Input: Jakarta Validation, Lombok
// Output: BatchUploadRequest DTO
// Pos: DTO Layer

package com.nexusarchive.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 批量上传请求 DTO
 */
@Data
public class BatchUploadRequest {

    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    @NotBlank(message = "全宗代码不能为空")
    private String fondsCode;

    @NotBlank(message = "会计年度不能为空")
    @Pattern(regexp = "^\\d{4}$", message = "会计年度格式错误，应为4位数字")
    private String fiscalYear;

    private String fiscalPeriod;

    @NotBlank(message = "档案门类不能为空")
    @Pattern(regexp = "^(VOUCHER|AC01|AC02|AC03|AC04|LEDGER|REPORT|OTHER)$",
             message = "档案门类必须为 VOUCHER/AC01/AC02/AC03/AC04/LEDGER/REPORT/OTHER")
    private String archivalCategory;

    @NotNull(message = "预计文件数量不能为空")
    @Min(value = 1, message = "文件数量至少为1")
    private Integer totalFiles;

    private Boolean autoCheck = true;
}
