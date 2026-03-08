// Input: Spring Boot ConfigurationProperties、Lombok
// Output: YundunOidcProperties 配置类
// Pos: 云盾 OIDC 集成配置层

package com.nexusarchive.integration.yundun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.yundun.oidc")
public class YundunOidcProperties {

    /**
     * 云盾 OIDC 回调桥接开关
     */
    private boolean enabled = false;

    /**
     * 对应 erp_sso_client.client_id，用于读取 client_secret 和用户映射
     */
    private String clientId;

    /**
     * 云盾 OIDC code 换 token 接口（完整 URL）
     */
    private String accessTokenUrl;

    /**
     * 云盾 OIDC userInfo 接口（完整 URL）
     */
    private String userInfoUrl;

    /**
     * 可选：与云盾侧登记的回调地址一致时可传，参与 code 换 token 请求
     */
    private String redirectUri;

    /**
     * access token 字段名（当 content 为对象时使用）
     */
    private String accessTokenField = "access_token";

    /**
     * 用户标识字段（用于映射 erp_user_mapping.erp_user_job_no）
     */
    private String userIdField = "sub";

    /**
     * 可选：Authorization 头前缀，默认空（云盾示例为裸 token）
     */
    private String authorizationPrefix = "";

    /**
     * 是否强制校验 state（防登录 CSRF）
     */
    private boolean requireState = true;

    /**
     * state 最小长度
     */
    private int stateMinLength = 16;

    /**
     * state HMAC 签名密钥（生产必须配置）
     */
    private String stateSigningKey;

    /**
     * state 有效期（秒）
     */
    private long stateTtlSeconds = 300;

    /**
     * state nonce 绑定 Cookie 名称
     */
    private String stateCookieName = "YUNDUN_OIDC_STATE_NONCE";

    /**
     * state nonce 绑定 Cookie 路径
     */
    private String stateCookiePath = "/api/integration/yundun/oidc";

    /**
     * state nonce 绑定 Cookie 是否仅 HTTPS 传输
     */
    private boolean stateCookieSecure = false;
}
