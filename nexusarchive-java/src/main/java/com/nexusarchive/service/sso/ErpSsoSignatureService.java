// Input: Java 标准库
// Output: ErpSsoSignatureService 接口
// Pos: SSO 服务层

package com.nexusarchive.service.sso;

public interface ErpSsoSignatureService {

    String sign(String payload, String secret);

    boolean verify(String payload, String secret, String signature);

    void validateTimestamp(Long timestampSeconds, long nowSeconds, long allowedSkewSeconds);
}
