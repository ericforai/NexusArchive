// Input: ErpAdapter, ErpScenario
// Output: VoucherFetcher
// Pos: Service Layer
// 负责从 ERP 适配器获取凭证数据

package com.nexusarchive.service.erp;

import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 凭证获取器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>根据场景类型调用不同的适配器方法</li>
 *   <li>封装适配器特定的获取逻辑</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VoucherFetcher {

    // 场景类型常量
    private static final String SCENARIO_COLLECTION_FILE_SYNC = "COLLECTION_FILE_SYNC";
    private static final String SCENARIO_PAYMENT_FILE_SYNC = "PAYMENT_FILE_SYNC";
    private static final String SCENARIO_REFUND_FILE_SYNC = "REFUND_FILE_SYNC";
    private static final String SCENARIO_PAYMENT_APPLY_SYNC = "PAYMENT_APPLY_SYNC";
    private static final String SCENARIO_PAYMENT_APPLY_FILE = "PAYMENT_APPLY_FILE";
    private static final String SCENARIO_ORG_SYNC = "ORG_SYNC";

    /**
     * 获取凭证数据
     *
     * @param adapter ERP 适配器
     * @param dtoConfig ERP 配置 DTO
     * @param scenario 场景
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 凭证列表
     */
    public List<VoucherDTO> fetchVouchers(ErpAdapter adapter,
                                           ErpConfig dtoConfig,
                                           ErpScenario scenario,
                                           LocalDate startDate,
                                           LocalDate endDate) {
        String scenarioKey = scenario.getScenarioKey();
        log.debug("获取凭证数据: scenarioKey={}, startDate={}, endDate={}", scenarioKey, startDate, endDate);

        // 根据场景类型调用不同的适配器方法
        if (isYonSuiteCollectionSync(scenarioKey, adapter)) {
            return getYonSuiteAdapter(adapter).syncCollectionFiles(dtoConfig, startDate, endDate);
        }

        if (isYonSuitePaymentSync(scenarioKey, adapter)) {
            return getYonSuiteAdapter(adapter).syncPaymentFiles(dtoConfig, startDate, endDate);
        }

        if (isYonSuiteRefundSync(scenarioKey, adapter)) {
            return getYonSuiteAdapter(adapter).syncRefundFiles(dtoConfig, startDate, endDate);
        }

        if (isYonSuitePaymentApplySync(scenarioKey, adapter)) {
            return getYonSuiteAdapter(adapter).syncPaymentApplyFiles(dtoConfig, startDate, endDate);
        }

        if (isYonSuiteOrgSync(scenarioKey, adapter)) {
            return getYonSuiteAdapter(adapter).syncOrg(dtoConfig, startDate, endDate);
        }

        // 默认：调用通用凭证同步
        return adapter.syncVouchers(dtoConfig, startDate, endDate);
    }

    /**
     * 判断是否为用友收款单同步
     */
    private boolean isYonSuiteCollectionSync(String scenarioKey, ErpAdapter adapter) {
        return SCENARIO_COLLECTION_FILE_SYNC.equals(scenarioKey)
            && adapter instanceof YonSuiteErpAdapter;
    }

    /**
     * 判断是否为用友付款单同步
     */
    private boolean isYonSuitePaymentSync(String scenarioKey, ErpAdapter adapter) {
        return SCENARIO_PAYMENT_FILE_SYNC.equals(scenarioKey)
            && adapter instanceof YonSuiteErpAdapter;
    }

    /**
     * 判断是否为用友退款单同步
     */
    private boolean isYonSuiteRefundSync(String scenarioKey, ErpAdapter adapter) {
        return SCENARIO_REFUND_FILE_SYNC.equals(scenarioKey)
            && adapter instanceof YonSuiteErpAdapter;
    }

    /**
     * 判断是否为用友付款申请单同步
     */
    private boolean isYonSuitePaymentApplySync(String scenarioKey, ErpAdapter adapter) {
        return (SCENARIO_PAYMENT_APPLY_SYNC.equals(scenarioKey)
                || SCENARIO_PAYMENT_APPLY_FILE.equals(scenarioKey))
            && adapter instanceof YonSuiteErpAdapter;
    }

    /**
     * 判断是否为用友组织架构同步
     */
    private boolean isYonSuiteOrgSync(String scenarioKey, ErpAdapter adapter) {
        return SCENARIO_ORG_SYNC.equals(scenarioKey)
            && adapter instanceof YonSuiteErpAdapter;
    }

    /**
     * 获取用友适配器
     */
    private YonSuiteErpAdapter getYonSuiteAdapter(ErpAdapter adapter) {
        return (YonSuiteErpAdapter) adapter;
    }
}
