package com.nexusarchive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 元数据更新请求 DTO
 * 用于预归档库「待补录」状态文件的元数据编辑
 */
public class MetadataUpdateDTO {

    /**
     * 文件ID
     */
    @NotBlank(message = "文件ID不能为空")
    private String id;

    /**
     * 会计年度 (4位数字)
     */
    @NotBlank(message = "会计年度不能为空")
    @Pattern(regexp = "^\\d{4}$", message = "会计年度必须为4位数字")
    private String fiscalYear;

    /**
     * 单据类型 (AC01-AC04)
     * AC01: 会计凭证
     * AC02: 会计账簿
     * AC03: 财务会计报告
     * AC04: 其他会计资料
     */
    @NotBlank(message = "单据类型不能为空")
    @Pattern(regexp = "^AC0[1-4]$", message = "单据类型必须为AC01-AC04")
    private String voucherType;

    /**
     * 责任者/创建人
     */
    @NotBlank(message = "责任者不能为空")
    private String creator;

    /**
     * 全宗号 (可选)
     */
    private String fondsCode;

    /**
     * 修改原因 (必填 - 合规要求 GB/T 39784-2021)
     */
    @NotBlank(message = "修改原因不能为空")
    private String modifyReason;

    /**
     * 备注 (可选)
     */
    private String remark;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }

    public String getVoucherType() { return voucherType; }
    public void setVoucherType(String voucherType) { this.voucherType = voucherType; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public String getFondsCode() { return fondsCode; }
    public void setFondsCode(String fondsCode) { this.fondsCode = fondsCode; }

    public String getModifyReason() { return modifyReason; }
    public void setModifyReason(String modifyReason) { this.modifyReason = modifyReason; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
