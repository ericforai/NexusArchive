// Input: cn.hutool、Lombok、Spring Framework、Java 标准库、等
// Output: GenericErpAdapter 类
// Pos: 集成模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.impl;

import cn.hutool.core.util.StrUtil;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.dto.AttachmentDTO;
import com.nexusarchive.integration.erp.dto.ConnectionTestResult;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Component("genericAdapter")
@Slf4j
public class GenericErpAdapter implements ErpAdapter {

    private static final String IDENTIFIER = "GENERIC";
    private static final String NAME = "通用ERP集成";

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        String url = config.getBaseUrl();
        if (StrUtil.isNotBlank(url)) {
            return ConnectionTestResult.success("URL Configured", 100L);
        }
        return ConnectionTestResult.fail("Base URL is empty", "ERR_Config_001");
    }

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("Pulling vouchers from Generic ERP: {}", config.getBaseUrl());
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
}
