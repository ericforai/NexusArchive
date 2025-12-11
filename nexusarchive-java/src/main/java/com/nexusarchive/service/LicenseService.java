package com.nexusarchive.service;

import com.nexusarchive.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;

/**
 * License 校验服务 (支持文件持久化)
 * License 保存到文件系统，服务重启后自动加载。
 */
@Slf4j
@Service
public class LicenseService {

    @Value("${license.public-key:}")
    private String licensePublicKey; // Base64 编码的 RSA 公钥

    @Value("${archive.root.path:./data/archives}")
    private String archiveRootPath;

    private volatile LicenseInfo cached;

    private static final String LICENSE_FILENAME = "license.json";

    /**
     * 服务启动时自动加载已持久化的 License
     */
    @PostConstruct
    public void init() {
        Path licensePath = getLicensePath();
        if (Files.exists(licensePath)) {
            try {
                String licenseText = Files.readString(licensePath);
                log.info("[License] 正在加载已保存的 License: {}", licensePath);
                validate(licenseText, false); // 加载但不重复保存
                log.info("[License] ✓ License 加载成功, 有效期至: {}", cached.getExpireAt());
            } catch (IOException e) {
                log.warn("[License] 无法读取 License 文件: {}", e.getMessage());
            } catch (BusinessException e) {
                log.warn("[License] License 已失效或损坏, 需要重新激活: {}", e.getMessage());
            }
        } else {
            log.info("[License] 未找到已保存的 License 文件, 系统需要激活");
        }
    }

    /**
     * 获取 License 文件存储路径
     */
    private Path getLicensePath() {
        Path dataDir = Paths.get(archiveRootPath).getParent();
        if (dataDir == null) {
            dataDir = Paths.get("./data");
        }
        return dataDir.resolve(LICENSE_FILENAME);
    }

    /**
     * 验证并加载 License
     */
    public LicenseInfo validate(String licenseText) {
        return validate(licenseText, true);
    }

    /**
     * 验证并加载 License
     * @param persist 是否持久化到文件
     */
    private LicenseInfo validate(String licenseText, boolean persist) {
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

            // 持久化到文件
            if (persist) {
                saveLicenseToFile(licenseText);
            }

            return info;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("License 无效");
        }
    }

    /**
     * 保存 License 到文件
     */
    private void saveLicenseToFile(String licenseText) {
        try {
            Path licensePath = getLicensePath();
            Files.createDirectories(licensePath.getParent());
            Files.writeString(licensePath, licenseText);
            log.info("[License] ✓ License 已保存到: {}", licensePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("[License] 保存 License 文件失败: {}", e.getMessage());
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

