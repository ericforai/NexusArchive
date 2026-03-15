// Input: cn.hutool、Lombok、Spring Framework、Javax、等
// Output: YonAuthService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nexusarchive.common.constants.HttpConstants;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YonSuite Token 服务
 * 实现 access_token 的获取和缓存
 */
@Service
@Slf4j
public class YonAuthService {

    private static final String TOKEN_URL = "/iuap-api-auth/open-auth/selfAppAuth/base/v1/getAccessToken";

    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com}")
    private String baseUrl;

    @Value("${yonsuite.app-key:}")
    private String appKey;

    @Value("${yonsuite.app-secret:}")
    private String appSecret;

    // Token 缓存: key -> TokenInfo
    private final ConcurrentHashMap<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    /**
     * 获取 access_token (自动刷新)
     */
    public String getAccessToken() {
        return getAccessToken(appKey, appSecret);
    }

    /**
     * 获取 access_token (指定 appKey/appSecret)
     */
    public String getAccessToken(String appKey, String appSecret) {
        // ✅ P0 修复: 防御性检查,确保参数非空且非空白
        if (appKey == null || appKey.trim().isEmpty()) {
            log.error("YonAuthService: appKey is null or empty");
            throw new IllegalArgumentException("YonSuite appKey 未配置,请检查 ERP 配置");
        }
        if (appSecret == null || appSecret.trim().isEmpty()) {
            log.error("YonAuthService: appSecret is null or empty");
            throw new IllegalArgumentException("YonSuite appSecret 未配置,请检查 ERP 配置");
        }
        
        // ✅ 使用 trim() 后的值作为缓存键,避免空格导致的问题
        String cacheKey = appKey.trim();
        TokenInfo cached = tokenCache.get(cacheKey);

        // 检查缓存是否有效 (提前60秒刷新)
        if (cached != null && cached.getExpiresAt().isAfter(LocalDateTime.now().plusSeconds(60))) {
            log.debug("Using cached token for appKey: {}", cacheKey);
            return cached.getAccessToken();
        }

        // 重新获取 token
        log.info("Fetching new access_token for appKey: {}", cacheKey);
        TokenInfo newToken = fetchToken(cacheKey, appSecret.trim());
        tokenCache.put(cacheKey, newToken);
        return newToken.getAccessToken();
    }

    /**
     * 从用友 API 获取 token
     */
    private TokenInfo fetchToken(String appKey, String appSecret) {
        long timestamp = System.currentTimeMillis();

        // 计算签名
        String signature = calculateSignature(appKey, timestamp, appSecret);

        // Token URL 使用认证地址 (不是 API Gateway)
        String authBaseUrl = "https://dbox.yonyoucloud.com";
        String url = authBaseUrl + TOKEN_URL
                + "?appKey=" + URLEncoder.encode(appKey, StandardCharsets.UTF_8)
                + "&timestamp=" + timestamp
                + "&signature=" + signature;

        log.debug("Token request URL: {}", url);

        try {
            // 构建HTTP请求 - 直接连接，不自动检测代理
            cn.hutool.http.HttpRequest request = HttpRequest.get(url)
                    .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
                    .timeout(15_000);
            
            // 仅当明确配置了代理环境变量时才使用代理
            String proxyHost = System.getenv("YONSUITE_PROXY_HOST");
            String proxyPort = System.getenv("YONSUITE_PROXY_PORT");
            if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null) {
                try {
                    request.setHttpProxy(proxyHost, Integer.parseInt(proxyPort));
                    log.info("Using configured proxy: {}:{}", proxyHost, proxyPort);
                } catch (Exception pe) {
                    log.warn("Failed to configure proxy: {}", pe.getMessage());
                }
            }
            // 不自动检测代理，直接连接
            
            String respStr = request.execute().body();

            log.debug("Token response: {}", respStr);

            JSONObject resp = JSONUtil.parseObj(respStr);

            if (!"00000".equals(resp.getStr("code"))) {
                throw new RuntimeException("Failed to get token: " + resp.getStr("message"));
            }

            JSONObject data = resp.getJSONObject("data");
            String accessToken = data.getStr("access_token");
            int expireSeconds = data.getInt("expire", 7200);

            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setAccessToken(accessToken);
            tokenInfo.setExpiresAt(LocalDateTime.now().plusSeconds(expireSeconds));

            log.info("Successfully obtained access_token, expires in {} seconds", expireSeconds);
            return tokenInfo;

        } catch (Exception e) {
            log.error("Failed to fetch token from YonSuite", e);
            throw new RuntimeException("Failed to fetch token: " + e.getMessage(), e);
        }
    }

    /**
     * 计算 HmacSHA256 签名
     * 格式: URLEncode(Base64(HmacSHA256(parameterMap)))
     */
    private String calculateSignature(String appKey, long timestamp, String appSecret) {
        try {
            // 按参数名排序拼接: appKey{value}timestamp{value}
            String parameterMap = "appKey" + appKey + "timestamp" + timestamp;

            // HmacSHA256
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(parameterMap.getBytes(StandardCharsets.UTF_8));

            // Base64
            String base64Signature = Base64.getEncoder().encodeToString(hash);

            // URLEncode
            String urlEncodedSignature = URLEncoder.encode(base64Signature, StandardCharsets.UTF_8);

            log.debug("Signature calculated: parameterMap={}, signature={}", parameterMap, urlEncodedSignature);
            return urlEncodedSignature;

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    @Data
    private static class TokenInfo {
        private String accessToken;
        private LocalDateTime expiresAt;
    }
}
