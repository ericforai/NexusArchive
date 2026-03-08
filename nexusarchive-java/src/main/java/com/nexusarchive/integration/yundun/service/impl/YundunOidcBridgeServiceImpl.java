// Input: 云盾 OIDC 配置、HTTP Facade、用户映射、认证服务
// Output: YundunOidcBridgeServiceImpl
// Pos: 云盾 OIDC 服务层实现

package com.nexusarchive.integration.yundun.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.dto.sso.YundunOidcCallbackResponse;
import com.nexusarchive.entity.ErpSsoClient;
import com.nexusarchive.entity.ErpUserMapping;
import com.nexusarchive.exception.ErpSsoException;
import com.nexusarchive.integration.yundun.config.YundunOidcProperties;
import com.nexusarchive.integration.yundun.sdk.YundunOidcHttpFacade;
import com.nexusarchive.integration.yundun.service.YundunOidcBridgeService;
import com.nexusarchive.integration.yundun.service.YundunOidcStateService;
import com.nexusarchive.mapper.ErpSsoClientMapper;
import com.nexusarchive.mapper.ErpUserMappingMapper;
import com.nexusarchive.service.AuthService;
import com.nexusarchive.service.sso.SsoErrorCodes;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class YundunOidcBridgeServiceImpl implements YundunOidcBridgeService {

    private static final List<String> TOKEN_FALLBACK_KEYS = List.of("access_token", "accessToken", "token");

    private final YundunOidcProperties properties;
    private final YundunOidcHttpFacade oidcHttpFacade;
    private final ErpSsoClientMapper erpSsoClientMapper;
    private final ErpUserMappingMapper erpUserMappingMapper;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final YundunOidcStateService oidcStateService;

    @Override
    public YundunOidcCallbackResponse consumeAuthCode(String code, String state, String cookieNonce) {
        if (!properties.isEnabled()) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_DISABLED, "云盾 OIDC 集成未启用", 503);
        }
        validateConfig();
        oidcStateService.validateAndConsume(state, cookieNonce);

        ErpSsoClient client = erpSsoClientMapper.findByClientId(properties.getClientId());
        if (client == null || !"ACTIVE".equalsIgnoreCase(client.getStatus())) {
            throw new ErpSsoException(SsoErrorCodes.CLIENT_NOT_FOUND, "SSO 客户端不存在或未启用", 401);
        }

        String accessToken = exchangeAccessToken(code, client.getClientSecret());
        String externalUserId = fetchExternalUserId(accessToken);

        ErpUserMapping mapping = erpUserMappingMapper.findActive(properties.getClientId(), externalUserId);
        if (mapping == null) {
            throw new ErpSsoException(SsoErrorCodes.USER_MAPPING_NOT_FOUND,
                    "云盾用户未映射 NexusArchive 用户", 400);
        }

        LoginResponse loginResponse = authService.issueTokenByUserId(mapping.getNexusUserId());
        return YundunOidcCallbackResponse.builder()
                .token(loginResponse.getToken())
                .user(loginResponse.getUser())
                .provider("YUNDUN_OIDC")
                .externalUserId(externalUserId)
                .build();
    }

    private void validateConfig() {
        if (StringUtils.isBlank(properties.getClientId())
                || StringUtils.isBlank(properties.getAccessTokenUrl())
                || StringUtils.isBlank(properties.getUserInfoUrl())
                || StringUtils.isBlank(properties.getUserIdField())) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_CONFIG_INVALID, "云盾 OIDC 配置不完整", 500);
        }
    }

    private String exchangeAccessToken(String code, String clientSecret) {
        String tokenJson;
        try {
            tokenJson = oidcHttpFacade.requestAccessToken(
                    properties.getAccessTokenUrl(),
                    code,
                    properties.getClientId(),
                    clientSecret,
                    properties.getRedirectUri());
        } catch (Exception e) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_TOKEN_EXCHANGE_FAILED, "调用云盾 OIDC token 接口异常", 502);
        }

        JsonNode payload = parseInvokeContent(tokenJson,
                SsoErrorCodes.YUNDUN_OIDC_TOKEN_EXCHANGE_FAILED,
                "获取 accessToken 失败");
        String token = resolveToken(payload);
        if (StringUtils.isBlank(token)) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_TOKEN_EXCHANGE_FAILED, "云盾 OIDC 返回空 accessToken", 502);
        }
        return token;
    }

    private String fetchExternalUserId(String accessToken) {
        String authHeaderValue = StringUtils.defaultString(properties.getAuthorizationPrefix()) + accessToken;
        String userInfoJson;
        try {
            userInfoJson = oidcHttpFacade.requestUserInfo(properties.getUserInfoUrl(), authHeaderValue);
        } catch (Exception e) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_USERINFO_FETCH_FAILED, "调用云盾 OIDC userInfo 接口异常", 502);
        }

        JsonNode payload = parseUserInfoPayload(userInfoJson);
        String externalUserId = readByPath(payload, properties.getUserIdField());
        if (StringUtils.isBlank(externalUserId)) {
            throw new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_USERINFO_INVALID,
                    "userInfo 缺少用户标识字段: " + properties.getUserIdField(), 502);
        }
        return externalUserId;
    }

    private JsonNode parseInvokeContent(String rawJson, String errorCode, String fallbackMessage) {
        JsonNode root = parseJson(rawJson, errorCode, fallbackMessage);
        int code = root.path("code").asInt(Integer.MIN_VALUE);
        if (code != 0) {
            String msg = StringUtils.defaultIfBlank(root.path("msg").asText(), fallbackMessage);
            throw new ErpSsoException(errorCode, msg, 502);
        }
        JsonNode content = root.get("content");
        if (content == null || content.isNull()) {
            throw new ErpSsoException(errorCode, fallbackMessage, 502);
        }
        return unwrapTextJson(content, errorCode, fallbackMessage);
    }

    private JsonNode parseUserInfoPayload(String rawJson) {
        JsonNode root = parseJson(rawJson, SsoErrorCodes.YUNDUN_OIDC_USERINFO_FETCH_FAILED, "获取 userInfo 失败");
        if (root.has("code")) {
            int code = root.path("code").asInt(Integer.MIN_VALUE);
            if (code != 0) {
                String msg = StringUtils.defaultIfBlank(root.path("msg").asText(), "获取 userInfo 失败");
                throw new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_USERINFO_FETCH_FAILED, msg, 502);
            }
            JsonNode content = root.get("content");
            if (content == null || content.isNull()) {
                throw new ErpSsoException(SsoErrorCodes.YUNDUN_OIDC_USERINFO_INVALID, "云盾 OIDC 返回空 userInfo", 502);
            }
            return unwrapTextJson(content, SsoErrorCodes.YUNDUN_OIDC_USERINFO_INVALID, "userInfo 解析失败");
        }
        return root;
    }

    private JsonNode parseJson(String rawJson, String errorCode, String fallbackMessage) {
        if (StringUtils.isBlank(rawJson)) {
            throw new ErpSsoException(errorCode, fallbackMessage, 502);
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new ErpSsoException(errorCode, fallbackMessage, 502);
        }
    }

    private JsonNode unwrapTextJson(JsonNode node, String errorCode, String fallbackMessage) {
        if (!node.isTextual()) {
            return node;
        }
        String text = StringUtils.trimToEmpty(node.asText());
        if (!(text.startsWith("{") || text.startsWith("["))) {
            return node;
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            throw new ErpSsoException(errorCode, fallbackMessage, 502);
        }
    }

    private String resolveToken(JsonNode payload) {
        if (payload.isTextual()) {
            return StringUtils.trimToEmpty(payload.asText());
        }

        if (!payload.isObject()) {
            return "";
        }

        List<String> keys = new ArrayList<>();
        keys.add(properties.getAccessTokenField());
        keys.addAll(TOKEN_FALLBACK_KEYS);
        for (String key : keys) {
            if (StringUtils.isBlank(key)) {
                continue;
            }
            JsonNode tokenNode = payload.get(key);
            if (tokenNode != null && !tokenNode.isNull()) {
                String token = StringUtils.trimToEmpty(tokenNode.asText());
                if (StringUtils.isNotBlank(token)) {
                    return token;
                }
            }
        }
        return "";
    }

    private String readByPath(JsonNode node, String path) {
        JsonNode current = node;
        for (String part : path.split("\\.")) {
            if (current == null || current.isNull()) {
                return "";
            }
            current = current.get(part);
        }
        if (current == null || current.isNull()) {
            return "";
        }
        return StringUtils.trimToEmpty(current.asText());
    }
}
