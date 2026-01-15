// Input: Spring Framework、Lombok、YonSuiteClient
// Output: YonSuiteAuthClient 类
// Pos: 集成模块 - ERP 适配器客户端
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter.client;

import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * YonSuite 认证客户端
 * 负责处理认证相关的操作：登录、登出、token 验证
 *
 * @author Agent D (基础设施工程师)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YonSuiteAuthClient {

    private final YonSuiteClient yonSuiteClient;

    /**
     * 使用 appKey/appSecret 获取访问令牌
     *
     * @param appKey    应用 Key
     * @param appSecret 应用密钥
     * @return 访问令牌，失败返回 null
     */
    public String getAccessToken(String appKey, String appSecret) {
        if (appKey == null || appKey.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            log.debug("appKey 或 appSecret 为空，无法获取 token");
            return null;
        }

        try {
            String token = yonSuiteClient.getTokenWithCredentials(appKey, appSecret);
            log.info("使用配置中的 appKey 获取 token 成功");
            return token;
        } catch (Exception e) {
            log.warn("使用配置中的 appKey 获取 token 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取访问令牌（从配置中提取认证信息）
     *
     * @param appKey    应用 Key（可为空）
     * @param appSecret 应用密钥（可为空）
     * @return 访问令牌，失败返回 null
     */
    public String getAccessTokenOrNull(String appKey, String appSecret) {
        return getAccessToken(appKey, appSecret);
    }

    /**
     * 验证令牌是否有效
     *
     * @param accessToken 访问令牌
     * @return true 如果令牌有效，false 否则
     */
    public boolean validateToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }
        // YonSuite API 会在调用时验证 token
        // 这里做基本检查
        return accessToken.length() > 10;
    }
}
