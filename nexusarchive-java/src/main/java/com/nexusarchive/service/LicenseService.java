package com.nexusarchive.service;

import com.nexusarchive.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;

/**
 * 简化版 License 校验服务
 * License 内容采用 Base64(JSON) 格式，含到期日与允许的节点数。
 */
@Service
public class LicenseService {

    @Value("${license.public-key:}")
    private String licensePublicKey; // Base64 编码的 RSA 公钥

    private volatile LicenseInfo cached;

    public LicenseInfo validate(String licenseText) {
        try {
            // License 格式：Base64(JSON) + 签名，形如 {"payload":"...","sig":"..."}
            Map<String, Object> wrapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(licenseText, Map.class);
            String payloadB64 = (String) wrapper.get("payload");
            String sigB64 = (String) wrapper.get("sig");
            if (payloadB64 == null || sigB64 == null) {
                throw new BusinessException("License 格式错误");
            }
            byte[] payloadBytes = Base64.getDecoder().decode(payloadB64);
            byte[] sigBytes = Base64.getDecoder().decode(sigB64);

            // 验签
            if (licensePublicKey != null && !licensePublicKey.isEmpty()) {
                KeyFactory kf = KeyFactory.getInstance("RSA");
                var pubKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(licensePublicKey)));
                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initVerify(pubKey);
                signature.update(payloadBytes);
                if (!signature.verify(sigBytes)) {
                    throw new BusinessException("License 签名校验失败");
                }
            }

            String json = new String(payloadBytes);
            Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
            String exp = (String) map.get("expireAt"); // yyyy-MM-dd
            Integer seats = map.get("maxUsers") != null ? Integer.parseInt(map.get("maxUsers").toString()) : null;
            Integer nodeLimit = map.get("nodeLimit") != null ? Integer.parseInt(map.get("nodeLimit").toString()) : null;

            LocalDate expireDate = LocalDate.parse(exp);
            if (expireDate.isBefore(LocalDate.now())) {
                throw new BusinessException("License 已过期");
            }

            LicenseInfo info = new LicenseInfo();
            info.setExpireAt(expireDate);
            info.setMaxUsers(seats);
            info.setNodeLimit(nodeLimit);
            info.setRaw(licenseText);
            this.cached = info;
            return info;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("License 无效");
        }
    }

    public LicenseInfo current() {
        return cached;
    }

    public void assertValid(int activeUsers) {
        LicenseInfo info = current();
        if (info == null) {
            throw new BusinessException("License 未加载");
        }
        if (info.isExpired()) {
            throw new BusinessException("License 已过期");
        }
        if (info.getMaxUsers() != null && activeUsers > info.getMaxUsers()) {
            throw new BusinessException("License 用户数超限");
        }
    }

    public static class LicenseInfo {
        private LocalDate expireAt;
        private Integer maxUsers;
        private Integer nodeLimit;
        private String raw;

        public LocalDate getExpireAt() { return expireAt; }
        public void setExpireAt(LocalDate expireAt) { this.expireAt = expireAt; }
        public Integer getMaxUsers() { return maxUsers; }
        public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
        public Integer getNodeLimit() { return nodeLimit; }
        public void setNodeLimit(Integer nodeLimit) { this.nodeLimit = nodeLimit; }
        public String getRaw() { return raw; }
        public void setRaw(String raw) { this.raw = raw; }

        public boolean isExpired() {
            return expireAt != null && expireAt.isBefore(LocalDate.now());
        }
    }
}
