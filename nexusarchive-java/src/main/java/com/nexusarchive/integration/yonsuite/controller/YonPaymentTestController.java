package com.nexusarchive.integration.yonsuite.controller;

import cn.hutool.json.JSONObject;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.integration.yonsuite.service.YonPaymentFileService;
import com.nexusarchive.mapper.ErpConfigMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * YonBIP Payment File Test Controller
 * Dedicated for verifying the "AI Integration Pilot" result.
 */
@RestController
@RequestMapping("/api/integration/yon/payment")
@Slf4j
@RequiredArgsConstructor
public class YonPaymentTestController {

    private final YonPaymentFileService paymentFileService;
    private final ErpConfigMapper erpConfigMapper;

    @PostMapping("/url/batch")
    public List<JSONObject> getFileUrls(@RequestBody PaymentFileRequest request) {
        log.info("Test Request: Get Payment File URLs for Config ID: {}, File IDs: {}", request.getConfigId(),
                request.getFileIds());

        // 1. Fetch Config
        ErpConfig entityConfig = erpConfigMapper.selectById(request.getConfigId());
        if (entityConfig == null) {
            throw new BusinessException("Config not found: " + request.getConfigId());
        }

        // 2. Convert to DTO (Manual mapping for testing simplicity)
        com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig = new com.nexusarchive.integration.erp.dto.ErpConfig();

        // Parse configJson from Entity
        if (cn.hutool.core.util.StrUtil.isNotEmpty(entityConfig.getConfigJson())) {
            JSONObject json = cn.hutool.json.JSONUtil.parseObj(entityConfig.getConfigJson());
            dtoConfig.setBaseUrl(json.getStr("baseUrl"));
            dtoConfig.setAppKey(json.getStr("appKey"));
            dtoConfig.setAppSecret(json.getStr("appSecret"));
            dtoConfig.setExtraConfig(json.getStr("extraConfig"));
        }

        // 3. Invoke Generated Service
        return paymentFileService.syncPaymentDetailsAndGeneratePdfs(dtoConfig, request.getFileIds());
    }

    @Data
    public static class PaymentFileRequest {
        private Long configId;
        private List<String> fileIds;
    }
}
