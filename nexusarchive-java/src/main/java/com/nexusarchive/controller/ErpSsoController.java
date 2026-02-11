// Input: Spring Web、SSO 服务
// Output: ErpSsoController
// Pos: 接口层 Controller

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.sso.ConsumeTicketResponse;
import com.nexusarchive.dto.sso.ErpLaunchRequest;
import com.nexusarchive.dto.sso.ErpLaunchResponse;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.service.sso.ErpSsoLaunchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/erp/sso")
@RequiredArgsConstructor
public class ErpSsoController {

    private final ErpSsoLaunchService erpSsoLaunchService;

    @PostMapping("/launch")
    public Result<ErpLaunchResponse> launch(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("X-Signature") String signature,
            @Valid @RequestBody ErpLaunchRequest request) {
        try {
            return Result.success(erpSsoLaunchService.launch(clientId, signature, request));
        } catch (ErpSsoException e) {
            log.warn("ERP SSO launch failed: code={}, message={}", e.getErrorCode(), e.getMessage());
            return Result.error(e.getHttpStatus(), e.getErrorCode() + ": " + e.getMessage());
        }
    }

    @PostMapping("/consume")
    public Result<ConsumeTicketResponse> consume(@RequestParam("ticket") String ticket) {
        try {
            return Result.success(erpSsoLaunchService.consume(ticket));
        } catch (ErpSsoException e) {
            log.warn("ERP SSO consume failed: code={}, message={}", e.getErrorCode(), e.getMessage());
            return Result.error(e.getHttpStatus(), e.getErrorCode() + ": " + e.getMessage());
        }
    }
}
