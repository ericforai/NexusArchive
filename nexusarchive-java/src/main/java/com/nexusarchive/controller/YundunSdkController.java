// Input: Spring Web、云盾 Token 服务
// Output: YundunSdkController
// Pos: 接口层 Controller

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.sso.YundunAppTokenResponse;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.integration.yundun.service.YundunTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/integration/yundun/sdk")
@RequiredArgsConstructor
public class YundunSdkController {

    private final YundunTokenService yundunTokenService;

    @PostMapping("/token")
    public Result<YundunAppTokenResponse> token() {
        try {
            String token = yundunTokenService.fetchAppToken();
            return Result.success(YundunAppTokenResponse.builder()
                    .token(token)
                    .provider("YUNDUN_SDK")
                    .issuedAt(Instant.now().getEpochSecond())
                    .build());
        } catch (ErpSsoException e) {
            log.warn("Yundun SDK token failed: code={}, message={}", e.getErrorCode(), e.getMessage());
            return Result.error(e.getHttpStatus(), e.getErrorCode() + ": " + e.getMessage());
        }
    }
}
