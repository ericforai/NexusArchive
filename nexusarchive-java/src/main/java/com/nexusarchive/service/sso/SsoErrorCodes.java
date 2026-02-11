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
}
