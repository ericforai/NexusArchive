package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.sso.YonSuiteSsoTokenRequest;
import com.nexusarchive.dto.sso.YonSuiteSsoTokenResponse;
import com.nexusarchive.dto.sso.YonSuiteSsoUrlRequest;
import com.nexusarchive.dto.sso.YonSuiteSsoUrlResponse;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.service.sso.YonSuiteSsoBridgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/integration/yonsuite/sso")
@RequiredArgsConstructor
public class YonSuiteSsoController {

    private final YonSuiteSsoBridgeService yonSuiteSsoBridgeService;

    @PostMapping("/token")
    public Result<YonSuiteSsoTokenResponse> issueToken(@Valid @RequestBody YonSuiteSsoTokenRequest request) {
        try {
            return Result.success(yonSuiteSsoBridgeService.issueToken(request));
        } catch (ErpSsoException e) {
            log.warn("YonSuite SSO token failed: code={}, message={}", e.getErrorCode(), e.getMessage());
            return Result.error(e.getHttpStatus(), e.getErrorCode() + ": " + e.getMessage());
        }
    }

    @PostMapping("/url")
    public Result<YonSuiteSsoUrlResponse> buildUrl(@Valid @RequestBody YonSuiteSsoUrlRequest request) {
        try {
            return Result.success(yonSuiteSsoBridgeService.buildLaunchUrl(request));
        } catch (ErpSsoException e) {
            log.warn("YonSuite SSO url failed: code={}, message={}", e.getErrorCode(), e.getMessage());
            return Result.error(e.getHttpStatus(), e.getErrorCode() + ": " + e.getMessage());
        }
    }
}
