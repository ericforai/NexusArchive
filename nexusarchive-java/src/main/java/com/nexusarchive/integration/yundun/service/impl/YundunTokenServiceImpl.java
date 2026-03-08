// Input: YunDun SDK 配置与 Facade
// Output: YundunTokenServiceImpl
// Pos: 云盾 SDK 业务服务层实现

package com.nexusarchive.integration.yundun.service.impl;

import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.integration.yundun.config.YundunSdkProperties;
import com.nexusarchive.integration.yundun.sdk.YundunSdkFacade;
import com.nexusarchive.integration.yundun.sdk.YundunSdkResult;
import com.nexusarchive.integration.yundun.service.YundunTokenService;
import com.nexusarchive.service.sso.SsoErrorCodes;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class YundunTokenServiceImpl implements YundunTokenService {

    private final YundunSdkProperties properties;
    private final YundunSdkFacade sdkFacade;

    @Override
    public String fetchAppToken() {
        if (!properties.isEnabled()) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_SDK_DISABLED, "云盾 SDK 集成未启用", 503);
        }

        String privateKey = StringUtils.trimToEmpty(properties.getPrivateKey());
        if (StringUtils.isBlank(privateKey)) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_SDK_CONFIG_INVALID, "云盾 SDK 私钥未配置", 500);
        }

        try {
            YundunSdkResult result = sdkFacade.applyAppToken(privateKey, properties.getIdpBaseUrl());
            if (result == null || result.code() != 0) {
                String msg = result == null ? "SDK 未返回结果" : result.message();
                throw new ErpSsoException(SsoErrorCodes.YUNDUN_SDK_TOKEN_FETCH_FAILED,
                        "获取云盾 appToken 失败: " + StringUtils.defaultIfBlank(msg, "unknown error"), 502);
            }

            String token = StringUtils.trimToEmpty(String.valueOf(result.content()));
            if (StringUtils.isBlank(token) || "null".equalsIgnoreCase(token)) {
                throw new ErpSsoException(SsoErrorCodes.YUNDUN_SDK_TOKEN_FETCH_FAILED, "云盾 SDK 返回空 token", 502);
            }
            return token;
        } catch (ErpSsoException e) {
            throw e;
        } catch (Exception e) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_SDK_TOKEN_FETCH_FAILED, "调用云盾 SDK 发生异常", 502);
        }
    }
}
