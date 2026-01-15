// Input: Lombok、匹配引擎枚举
// Output: VoucherData DTO
// Pos: 匹配引擎/DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import com.nexusarchive.engine.matching.enums.AccountRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 凭证数据 DTO（用于匹配）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherData {
    
    private String voucherId;
    private String voucherNo;
    private String voucherWord;  // 收/付/转
    private String summary;
    private BigDecimal amount;
    private LocalDate docDate;
    
    // 科目角色
    private Set<AccountRole> debitRoles;
    private Set<AccountRole> creditRoles;
    
    // 辅助核算
    private String counterpartyId;
    private String counterpartyName;
    private String employeeId;
    private String contractNo;
    
    // 公司信息
    private Long companyId;
    private String bookCode;

    // Manual Getters
    public String getSummary() { return summary; }
}
