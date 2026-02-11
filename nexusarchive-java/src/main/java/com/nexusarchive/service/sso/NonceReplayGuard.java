// Input: Java 标准库
// Output: NonceReplayGuard 接口
// Pos: SSO 服务层

package com.nexusarchive.service.sso;

public interface NonceReplayGuard {

    boolean tryAcquire(String clientId, String nonce, long ttlSeconds);
}
