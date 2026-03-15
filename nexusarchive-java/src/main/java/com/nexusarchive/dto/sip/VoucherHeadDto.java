// Input: Jackson、Lombok、Jakarta EE、Java 标准库、等
// Output: VoucherHeadDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.sip;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nexusarchive.common.enums.VoucherType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 凭证头信息 DTO
 * Reference: DA/T 94-2022 会计凭证元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherHeadDto {
    
    @NotBlank(message = "全宗号不能为空 (参考: DA/T 94-4.1)")
    @Size(max = 50, message = "全宗号长度不能超过50字符")
    private String fondsCode;
    
    @NotBlank(message = "会计期间不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "会计期间格式错误，应为 YYYY-MM")
    private String accountPeriod;
    
    @NotNull(message = "凭证类型不能为空")
    private VoucherType voucherType;
    
    @NotBlank(message = "凭证号不能为空 (参考: DA/T 94-4.2)")
    @Size(max = 50, message = "凭证号长度不能超过50字符")
    private String voucherNumber;
    
    @NotNull(message = "凭证日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate voucherDate;
    
    @NotNull(message = "凭证总金额不能为空")
    @DecimalMin(value = "0.00", message = "凭证总金额不能为负数")
    @Digits(integer = 18, fraction = 2, message = "凭证总金额格式错误：最多18位整数，2位小数")
    private BigDecimal totalAmount;
    
    @NotBlank(message = "币种代码不能为空")
    @Pattern(regexp = "^[A-Z]{3}$", message = "币种代码格式错误，应为3位大写字母（如 CNY）")
    private String currencyCode;
    
    @NotNull(message = "附件数量不能为空")
    @Min(value = 0, message = "附件数量不能为负数")
    private Integer attachmentCount;
    
    @NotBlank(message = "制单人不能为空 (参考: DA/T 94-4.3)")
    @Size(max = 50, message = "制单人名称长度不能超过50字符")
    private String issuer;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate postingDate;
    
    private String reviewer;
    
    private String remark;
}
