package com.nexusarchive.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioParamsUpdateRequest {

    @NotBlank(message = "场景参数不能为空")
    private String scenarioKey;

    @Pattern(regexp = "^(MANUAL|CRON|REALTIME)$", message = "同步策略必须是 MANUAL、CRON 或 REALTIME")
    private String syncStrategy;

    @Min(value = 1, message = "期间间隔天数必须大于0")
    @Max(value = 365, message = "期间间隔天数不能超过365")
    private Integer periodDays;

    private Map<String, Object> mapping;

    private Map<String, Object> filter;
}
