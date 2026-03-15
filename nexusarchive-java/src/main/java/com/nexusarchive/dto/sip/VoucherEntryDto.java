// Input: Lombok、Jakarta EE、Java 标准库、本地模块
// Output: VoucherEntryDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.sip;

import com.nexusarchive.common.enums.DirectionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 会计分录 DTO
 * Reference: DA/T 94-2022 会计凭证分录结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherEntryDto {
    
    @NotNull(message = "分录行号不能为空")
    @Min(value = 1, message = "分录行号必须从1开始")
    private Integer lineNo;
    
    @NotBlank(message = "摘要不能为空")
    @Size(max = 200, message = "摘要长度不能超过200字符")
    private String summary;
    
    @NotBlank(message = "会计科目代码不能为空")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "会计科目代码格式错误，应为4-6位数字")
    private String subjectCode;
    
    private String subjectName;
    
    @NotNull(message = "借贷方向不能为空")
    private DirectionType direction;
    
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.00", message = "金额不能为负数")
    @Digits(integer = 18, fraction = 2, message = "金额格式错误：最多18位整数，2位小数")
    private BigDecimal amount;
    
    private String auxiliaryInfo;

    private String currencyCode;
    private String currencyName;
    private BigDecimal debitOriginal;
    private BigDecimal creditOriginal;
    private BigDecimal exchangeRate;
}
