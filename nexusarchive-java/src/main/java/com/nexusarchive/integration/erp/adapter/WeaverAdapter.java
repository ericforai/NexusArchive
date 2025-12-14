package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.integration.erp.dto.AttachmentDTO;
import com.nexusarchive.integration.erp.dto.ConnectionTestResult;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 泛微 OA (Weaver Ecology) 适配器
 * 实现报销单据同步流程
 */
@Slf4j
@Service("weaver")
public class WeaverAdapter implements ErpAdapter {

    @Override
    public String getIdentifier() {
        return "weaver";
    }

    @Override
    public String getName() {
        return "泛微OA";
    }

    @Override
    public String getDescription() {
        return "泛微 Ecology OA 系统集成，支持流程表单转凭证归档";
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        log.info("Testing connection to Weaver OA at {}", config.getBaseUrl());
        // Mock connection success
        return ConnectionTestResult.success("连接泛微OA (Ecology9) 成功", 45L);
    }

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("从泛微OA同步报销单据: {} to {}", startDate, endDate);
        // FIXME: 实现实际的 Ecology API 调用
        return Collections.emptyList();
    }

    @Override
    public VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo) {
        return null;
    }

    @Override
    public List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo) {
        return Collections.emptyList();
    }

    @Override
    public List<ErpScenario> getAvailableScenarios() {
        List<ErpScenario> scenarios = new ArrayList<>();

        ErpScenario s1 = new ErpScenario();
        s1.setScenarioKey("EXPENSE_SYNC");
        s1.setName("报销单据同步");
        s1.setDescription("自动同步已归档的费用报销流程，生成并归档原始凭证");
        s1.setSyncStrategy("CRON");
        s1.setCronExpression("0 0 2 * * ?"); // 每日凌晨2点
        s1.setIsActive(true); // 默认启用，方便用户查看效果

        scenarios.add(s1);
        return scenarios;
    }
}
