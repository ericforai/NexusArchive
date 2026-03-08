// Input: Spring Web、云盾 OIDC 桥接服务
// Output: YundunOidcController
// Pos: 接口层 Controller

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.sso.YundunOidcCallbackResponse;
import com.nexusarchive.dto.sso.YundunOidcStateResponse;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.integration.yundun.service.YundunOidcBridgeService;
import com.nexusarchive.integration.yundun.service.YundunOidcStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/integration/yundun/oidc")
@RequiredArgsConstructor
public class YundunOidcController {

    private final YundunOidcBridgeService yundunOidcBridgeService;
    private final YundunOidcStateService yundunOidcStateService;

    @GetMapping("/state")
    public Result<YundunOidcStateResponse> issueState(HttpServletResponse response) {
        try {
            return Result.success(yundunOidcStateService.issueState(response));
        } catch (ErpSsoException e) {
            log.warn("Yundun OIDC state issue failed: code={}, message={}", e.getErrorCode(), e.getMessage());
            return Result.error(e.getHttpStatus(), e.getErrorCode() + ": " + e.getMessage());
        }
    }

    @GetMapping("/callback")
    public Result<YundunOidcCallbackResponse> callback(@RequestParam("code") String code,
                                                        @RequestParam(value = "state", required = false) String state,
                                                        HttpServletRequest request,
                                                        HttpServletResponse response) {
        try {
            String cookieNonce = yundunOidcStateService.readNonceFromCookie(request);
            Result<YundunOidcCallbackResponse> result = Result.success(
                    yundunOidcBridgeService.consumeAuthCode(code, state, cookieNonce));
            yundunOidcStateService.clearNonceCookie(response);
            return result;
        } catch (ErpSsoException e) {
            log.warn("Yundun OIDC callback failed: code={}, message={}", e.getErrorCode(), e.getMessage());
            return Result.error(e.getHttpStatus(), e.getErrorCode() + ": " + e.getMessage());
        }
    }
}
