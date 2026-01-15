// Input: Lombok、Jakarta EE、Java 标准库
// Output: AccountingSipDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.sip;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * SIP (Submission Information Package) 接收 DTO
 * Reference: DA/T 104-2024 ERP 接口规范
 * 
 * 核心业务规则 (Critical Rules):
 * 1. Integrity: header.attachmentCount == attachments.size()
 * 2. Balance: sum(entries.amount) == header.totalAmount
 * 3. Mandatory Metadata: fonds_code, voucher_number, issuer, posting_date 必填
 */
public class AccountingSipDto {
    
    @NotBlank(message = "请求 ID 不能为空")
    private String requestId;
    
    @NotBlank(message = "来源系统不能为空")
    @Size(max = 50, message = "来源系统名称长度不能超过50字符")
    private String sourceSystem;
    
    private String batchNo;
    
    @NotNull(message = "凭证头信息不能为空")
    @Valid
    private VoucherHeadDto header;
    
    @NotEmpty(message = "凭证分录不能为空")
    @Size(min = 1, message = "至少需要一条凭证分录")
    @Valid
    private List<VoucherEntryDto> entries;
    
    @Valid
    private List<AttachmentDto> attachments;

    public AccountingSipDto() {}

    public AccountingSipDto(String requestId, String sourceSystem, String batchNo, VoucherHeadDto header, List<VoucherEntryDto> entries, List<AttachmentDto> attachments) {
        this.requestId = requestId;
        this.sourceSystem = sourceSystem;
        this.batchNo = batchNo;
        this.header = header;
        this.entries = entries;
        this.attachments = attachments;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public VoucherHeadDto getHeader() { return header; }
    public void setHeader(VoucherHeadDto header) { this.header = header; }

    public List<VoucherEntryDto> getEntries() { return entries; }
    public void setEntries(List<VoucherEntryDto> entries) { this.entries = entries; }

    public List<AttachmentDto> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentDto> attachments) { this.attachments = attachments; }
    
    /**
     * 业务规则验证方法
     * 在 Controller 接收后调用
     */
    @AssertTrue(message = "附件数量不匹配：header.attachmentCount 必须等于 attachments.size()")
    public boolean isAttachmentCountValid() {
        if (header == null) {
            return false;
        }
        int actualCount = (attachments == null) ? 0 : attachments.size();
        return header.getAttachmentCount().equals(actualCount);
    }

    public static AccountingSipDtoBuilder builder() {
        return new AccountingSipDtoBuilder();
    }

    public static class AccountingSipDtoBuilder {
        private AccountingSipDto dto = new AccountingSipDto();

        public AccountingSipDtoBuilder requestId(String requestId) { dto.setRequestId(requestId); return this; }
        public AccountingSipDtoBuilder sourceSystem(String sourceSystem) { dto.setSourceSystem(sourceSystem); return this; }
        public AccountingSipDtoBuilder batchNo(String batchNo) { dto.setBatchNo(batchNo); return this; }
        public AccountingSipDtoBuilder header(VoucherHeadDto header) { dto.setHeader(header); return this; }
        public AccountingSipDtoBuilder entries(List<VoucherEntryDto> entries) { dto.setEntries(entries); return this; }
        public AccountingSipDtoBuilder attachments(List<AttachmentDto> attachments) { dto.setAttachments(attachments); return this; }
        
        public AccountingSipDto build() { return dto; }
    }
}
