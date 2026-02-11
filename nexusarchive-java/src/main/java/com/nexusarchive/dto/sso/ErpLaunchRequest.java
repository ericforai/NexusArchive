// Input: Lombok、Jakarta Validation
// Output: ErpLaunchRequest DTO
// Pos: SSO DTO

package com.nexusarchive.dto.sso;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ErpLaunchRequest {

    @NotBlank(message = "账套编码不能为空")
    private String accbookCode;

    @NotBlank(message = "ERP 用户工号不能为空")
    private String erpUserJobNo;

    @NotBlank(message = "凭证号不能为空")
    private String voucherNo;

    private Long timestamp;

    @NotBlank(message = "nonce 不能为空")
    private String nonce;
}
