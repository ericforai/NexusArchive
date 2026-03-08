// Input: Java 标准库
// Output: SsoErrorCodes 常量
// Pos: SSO 服务层

package com.nexusarchive.service.sso;

public final class SsoErrorCodes {

    private SsoErrorCodes() {
    }

    public static final String INVALID_SIGNATURE = "INVALID_SIGNATURE";
    public static final String TIMESTAMP_EXPIRED = "TIMESTAMP_EXPIRED";
    public static final String NONCE_REPLAYED = "NONCE_REPLAYED";
    public static final String CLIENT_NOT_FOUND = "CLIENT_NOT_FOUND";
    public static final String USER_MAPPING_NOT_FOUND = "USER_MAPPING_NOT_FOUND";
    public static final String ACCBOOK_MAPPING_NOT_FOUND = "ACCBOOK_MAPPING_NOT_FOUND";
    public static final String ACCBOOK_MAPPING_DUPLICATE = "ACCBOOK_MAPPING_DUPLICATE";
    public static final String TICKET_NOT_FOUND = "TICKET_NOT_FOUND";
    public static final String TICKET_EXPIRED = "TICKET_EXPIRED";
    public static final String TICKET_ALREADY_USED = "TICKET_ALREADY_USED";
    public static final String SSO_TOKEN_INVALID = "SSO_TOKEN_INVALID";
    public static final String SSO_TOKEN_EXPIRED = "SSO_TOKEN_EXPIRED";
    public static final String SSO_TOKEN_ALREADY_USED = "SSO_TOKEN_ALREADY_USED";
    public static final String YUNDUN_SDK_DISABLED = "YUNDUN_SDK_DISABLED";
    public static final String YUNDUN_SDK_CONFIG_INVALID = "YUNDUN_SDK_CONFIG_INVALID";
    public static final String YUNDUN_SDK_TOKEN_FETCH_FAILED = "YUNDUN_SDK_TOKEN_FETCH_FAILED";
    public static final String YUNDUN_OIDC_DISABLED = "YUNDUN_OIDC_DISABLED";
    public static final String YUNDUN_OIDC_CONFIG_INVALID = "YUNDUN_OIDC_CONFIG_INVALID";
    public static final String YUNDUN_OIDC_TOKEN_EXCHANGE_FAILED = "YUNDUN_OIDC_TOKEN_EXCHANGE_FAILED";
    public static final String YUNDUN_OIDC_USERINFO_FETCH_FAILED = "YUNDUN_OIDC_USERINFO_FETCH_FAILED";
    public static final String YUNDUN_OIDC_USERINFO_INVALID = "YUNDUN_OIDC_USERINFO_INVALID";
}
