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

    public VoucherEntryDto() {}

    public VoucherEntryDto(Integer lineNo, String summary, String subjectCode, String subjectName, DirectionType direction, BigDecimal amount, String auxiliaryInfo, String currencyCode, String currencyName, BigDecimal debitOriginal, BigDecimal creditOriginal, BigDecimal exchangeRate) {
        this.lineNo = lineNo;
        this.summary = summary;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.direction = direction;
        this.amount = amount;
        this.auxiliaryInfo = auxiliaryInfo;
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.debitOriginal = debitOriginal;
        this.creditOriginal = creditOriginal;
        this.exchangeRate = exchangeRate;
    }

    public Integer getLineNo() { return lineNo; }
    public void setLineNo(Integer lineNo) { this.lineNo = lineNo; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public DirectionType getDirection() { return direction; }
    public void setDirection(DirectionType direction) { this.direction = direction; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getAuxiliaryInfo() { return auxiliaryInfo; }
    public void setAuxiliaryInfo(String auxiliaryInfo) { this.auxiliaryInfo = auxiliaryInfo; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getCurrencyName() { return currencyName; }
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }

    public BigDecimal getDebitOriginal() { return debitOriginal; }
    public void setDebitOriginal(BigDecimal debitOriginal) { this.debitOriginal = debitOriginal; }

    public BigDecimal getCreditOriginal() { return creditOriginal; }
    public void setCreditOriginal(BigDecimal creditOriginal) { this.creditOriginal = creditOriginal; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public static VoucherEntryDtoBuilder builder() {
        return new VoucherEntryDtoBuilder();
    }

    public static class VoucherEntryDtoBuilder {
        private VoucherEntryDto dto = new VoucherEntryDto();

        public VoucherEntryDtoBuilder lineNo(Integer lineNo) { dto.setLineNo(lineNo); return this; }
        public VoucherEntryDtoBuilder summary(String summary) { dto.setSummary(summary); return this; }
        public VoucherEntryDtoBuilder subjectCode(String subjectCode) { dto.setSubjectCode(subjectCode); return this; }
        public VoucherEntryDtoBuilder subjectName(String subjectName) { dto.setSubjectName(subjectName); return this; }
        public VoucherEntryDtoBuilder direction(DirectionType direction) { dto.setDirection(direction); return this; }
        public VoucherEntryDtoBuilder amount(BigDecimal amount) { dto.setAmount(amount); return this; }
        public VoucherEntryDtoBuilder auxiliaryInfo(String auxiliaryInfo) { dto.setAuxiliaryInfo(auxiliaryInfo); return this; }
        public VoucherEntryDtoBuilder currencyCode(String currencyCode) { dto.setCurrencyCode(currencyCode); return this; }
        public VoucherEntryDtoBuilder currencyName(String currencyName) { dto.setCurrencyName(currencyName); return this; }
        public VoucherEntryDtoBuilder debitOriginal(BigDecimal debitOriginal) { dto.setDebitOriginal(debitOriginal); return this; }
        public VoucherEntryDtoBuilder creditOriginal(BigDecimal creditOriginal) { dto.setCreditOriginal(creditOriginal); return this; }
        public VoucherEntryDtoBuilder exchangeRate(BigDecimal exchangeRate) { dto.setExchangeRate(exchangeRate); return this; }
        
        public VoucherEntryDto build() { return dto; }
    }
}
