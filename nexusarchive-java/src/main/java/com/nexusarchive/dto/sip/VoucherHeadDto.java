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

    public VoucherHeadDto() {}

    public VoucherHeadDto(String fondsCode, String accountPeriod, VoucherType voucherType, String voucherNumber, LocalDate voucherDate, BigDecimal totalAmount, String currencyCode, Integer attachmentCount, String issuer, LocalDate postingDate, String reviewer, String remark) {
        this.fondsCode = fondsCode;
        this.accountPeriod = accountPeriod;
        this.voucherType = voucherType;
        this.voucherNumber = voucherNumber;
        this.voucherDate = voucherDate;
        this.totalAmount = totalAmount;
        this.currencyCode = currencyCode;
        this.attachmentCount = attachmentCount;
        this.issuer = issuer;
        this.postingDate = postingDate;
        this.reviewer = reviewer;
        this.remark = remark;
    }

    public String getFondsCode() { return fondsCode; }
    public void setFondsCode(String fondsCode) { this.fondsCode = fondsCode; }

    public String getAccountPeriod() { return accountPeriod; }
    public void setAccountPeriod(String accountPeriod) { this.accountPeriod = accountPeriod; }

    public VoucherType getVoucherType() { return voucherType; }
    public void setVoucherType(VoucherType voucherType) { this.voucherType = voucherType; }

    public String getVoucherNumber() { return voucherNumber; }
    public void setVoucherNumber(String voucherNumber) { this.voucherNumber = voucherNumber; }

    public LocalDate getVoucherDate() { return voucherDate; }
    public void setVoucherDate(LocalDate voucherDate) { this.voucherDate = voucherDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public Integer getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(Integer attachmentCount) { this.attachmentCount = attachmentCount; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public LocalDate getPostingDate() { return postingDate; }
    public void setPostingDate(LocalDate postingDate) { this.postingDate = postingDate; }

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public static VoucherHeadDtoBuilder builder() {
        return new VoucherHeadDtoBuilder();
    }

    public static class VoucherHeadDtoBuilder {
        private VoucherHeadDto dto = new VoucherHeadDto();

        public VoucherHeadDtoBuilder fondsCode(String fondsCode) { dto.setFondsCode(fondsCode); return this; }
        public VoucherHeadDtoBuilder accountPeriod(String accountPeriod) { dto.setAccountPeriod(accountPeriod); return this; }
        public VoucherHeadDtoBuilder voucherType(VoucherType voucherType) { dto.setVoucherType(voucherType); return this; }
        public VoucherHeadDtoBuilder voucherNumber(String voucherNumber) { dto.setVoucherNumber(voucherNumber); return this; }
        public VoucherHeadDtoBuilder voucherDate(LocalDate voucherDate) { dto.setVoucherDate(voucherDate); return this; }
        public VoucherHeadDtoBuilder totalAmount(BigDecimal totalAmount) { dto.setTotalAmount(totalAmount); return this; }
        public VoucherHeadDtoBuilder currencyCode(String currencyCode) { dto.setCurrencyCode(currencyCode); return this; }
        public VoucherHeadDtoBuilder attachmentCount(Integer attachmentCount) { dto.setAttachmentCount(attachmentCount); return this; }
        public VoucherHeadDtoBuilder issuer(String issuer) { dto.setIssuer(issuer); return this; }
        public VoucherHeadDtoBuilder postingDate(LocalDate postingDate) { dto.setPostingDate(postingDate); return this; }
        public VoucherHeadDtoBuilder reviewer(String reviewer) { dto.setReviewer(reviewer); return this; }
        public VoucherHeadDtoBuilder remark(String remark) { dto.setRemark(remark); return this; }
        
        public VoucherHeadDto build() { return dto; }
    }
}
