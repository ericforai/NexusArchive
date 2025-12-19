package com.nexusarchive.dto.reconciliation;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * 对账触发请求参数
 * 用于防范SQL注入和参数篡改 (Critical Bug #2修复)
 */
@Data
public class ReconciliationRequest {

    @NotNull(message = "ERP配置ID不能为空")
    private Long configId;

    @NotBlank(message = "科目代码不能为空")
    @Pattern(regexp = "^[A-Z0-9_.]{4,50}$", message = "科目代码格式非法(仅允许字母数字下划线及点)")
    private String subjectCode;

    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // operatorId 不再从前端接收，改为从 SecurityContext 获取
}
