// Input: ErpScenario
// Output: SyncDateRangeExtractor
// Pos: Service Layer
// 负责从场景参数中提取同步日期范围

package com.nexusarchive.service.erp;

import com.nexusarchive.entity.ErpScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 同步日期范围提取器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>从场景参数中解析日期范围</li>
 *   <li>提供默认日期范围</li>
 * </ul>
 */
@Service
@Slf4j
public class SyncDateRangeExtractor {

    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2020, 1, 1);
    private static final String PARAM_START_DATE = "startDate";
    private static final String PARAM_END_DATE = "endDate";

    /**
     * 提取同步日期范围
     *
     * @param scenario ERP 场景
     * @return 日期范围
     */
    public DateRange extractDateRange(ErpScenario scenario) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = DEFAULT_START_DATE;

        if (scenario.getParamsJson() != null) {
            try {
                cn.hutool.json.JSONObject params = cn.hutool.json.JSONUtil.parseObj(scenario.getParamsJson());
                String startStr = params.getStr(PARAM_START_DATE);
                String endStr = params.getStr(PARAM_END_DATE);

                if (cn.hutool.core.util.StrUtil.isNotEmpty(startStr)) {
                    startDate = LocalDate.parse(startStr);
                }
                if (cn.hutool.core.util.StrUtil.isNotEmpty(endStr)) {
                    endDate = LocalDate.parse(endStr);
                }
            } catch (Exception paramEx) {
                log.warn("解析场景参数失败，使用默认值: {}", paramEx.getMessage());
            }
        }

        return new DateRange(startDate, endDate);
    }

    /**
     * 日期范围值对象
     */
    public record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
