package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 证书管理控制器
 * 
 * 提供证书的查询和管理功能
 * 
 * @author Agent B - 合规开发工程师
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/certificates")
@RequiredArgsConstructor
@Tag(name = "证书管理", description = "证书查询和管理功能")
public class CertificateController {

    @Value("${signature.keystore.path:#{null}}")
    private String keystorePath;

    @Value("${signature.keystore.password:changeit}")
    private String keystorePassword;

    /**
     * 查询所有证书
     */
    @GetMapping
    @Operation(summary = "查询所有证书", description = "查询密钥库中的所有证书")
    public Result<List<CertificateInfo>> listCertificates() {
        try {
            KeyStore keyStore = loadKeyStore();
            if (keyStore == null) {
                return Result.fail("密钥库未配置");
            }

            List<CertificateInfo> certificates = new ArrayList<>();
            Enumeration<String> aliases = keyStore.aliases();

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

                if (cert != null) {
                    CertificateInfo info = new CertificateInfo();
                    info.setAlias(alias);
                    info.setSubject(cert.getSubjectX500Principal().getName());
                    info.setIssuer(cert.getIssuerX500Principal().getName());
                    info.setSerialNumber(cert.getSerialNumber().toString(16));
                    info.setValidFrom(cert.getNotBefore().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                    info.setValidTo(cert.getNotAfter().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                    info.setExpired(cert.getNotAfter().before(new java.util.Date()));
                    info.setAlgorithm(cert.getSigAlgName());

                    certificates.add(info);
                }
            }

            return Result.success(certificates);
        } catch (Exception e) {
            log.error("查询证书列表异常: {}", e.getMessage(), e);
            return Result.fail("查询证书列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询单个证书详情
     */
    @GetMapping("/{alias}")
    @Operation(summary = "查询证书详情", description = "根据别名查询证书详情")
    public Result<CertificateInfo> getCertificate(
            @Parameter(description = "证书别名", required = true) @PathVariable String alias) {
        
        try {
            KeyStore keyStore = loadKeyStore();
            if (keyStore == null) {
                return Result.fail("密钥库未配置");
            }

            if (!keyStore.containsAlias(alias)) {
                return Result.fail("证书不存在: " + alias);
            }

            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            if (cert == null) {
                return Result.fail("无法加载证书: " + alias);
            }

            CertificateInfo info = new CertificateInfo();
            info.setAlias(alias);
            info.setSubject(cert.getSubjectX500Principal().getName());
            info.setIssuer(cert.getIssuerX500Principal().getName());
            info.setSerialNumber(cert.getSerialNumber().toString(16));
            info.setValidFrom(cert.getNotBefore().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            info.setValidTo(cert.getNotAfter().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            info.setExpired(cert.getNotAfter().before(new java.util.Date()));
            info.setAlgorithm(cert.getSigAlgName());

            return Result.success(info);
        } catch (Exception e) {
            log.error("查询证书详情异常: {}", e.getMessage(), e);
            return Result.fail("查询证书详情失败: " + e.getMessage());
        }
    }

    /**
     * 验证证书有效性
     */
    @PostMapping("/{alias}/verify")
    @Operation(summary = "验证证书有效性", description = "验证证书是否有效、是否过期")
    public Result<CertificateVerifyResult> verifyCertificate(
            @Parameter(description = "证书别名", required = true) @PathVariable String alias) {
        
        try {
            KeyStore keyStore = loadKeyStore();
            if (keyStore == null) {
                return Result.fail("密钥库未配置");
            }

            if (!keyStore.containsAlias(alias)) {
                return Result.fail("证书不存在: " + alias);
            }

            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            if (cert == null) {
                return Result.fail("无法加载证书: " + alias);
            }

            CertificateVerifyResult result = new CertificateVerifyResult();
            result.setAlias(alias);

            try {
                cert.checkValidity();
                result.setValid(true);
                result.setExpired(false);
                result.setMessage("证书有效");
            } catch (java.security.cert.CertificateExpiredException e) {
                result.setValid(false);
                result.setExpired(true);
                result.setMessage("证书已过期");
            } catch (java.security.cert.CertificateNotYetValidException e) {
                result.setValid(false);
                result.setExpired(false);
                result.setMessage("证书尚未生效");
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("验证证书异常: {}", e.getMessage(), e);
            return Result.fail("验证证书失败: " + e.getMessage());
        }
    }

    /**
     * 加载密钥库
     */
    private KeyStore loadKeyStore() {
        if (keystorePath == null || keystorePath.isEmpty()) {
            return null;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = getClass().getResourceAsStream(keystorePath)) {
                if (is == null) {
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(keystorePath)) {
                        keyStore.load(fis, keystorePassword.toCharArray());
                    }
                } else {
                    keyStore.load(is, keystorePassword.toCharArray());
                }
            }
            return keyStore;
        } catch (Exception e) {
            log.error("加载密钥库失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 证书信息 DTO
     */
    public static class CertificateInfo {
        private String alias;
        private String subject;
        private String issuer;
        private String serialNumber;
        private LocalDateTime validFrom;
        private LocalDateTime validTo;
        private boolean expired;
        private String algorithm;

        // Getters and Setters
        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public LocalDateTime getValidFrom() {
            return validFrom;
        }

        public void setValidFrom(LocalDateTime validFrom) {
            this.validFrom = validFrom;
        }

        public LocalDateTime getValidTo() {
            return validTo;
        }

        public void setValidTo(LocalDateTime validTo) {
            this.validTo = validTo;
        }

        public boolean isExpired() {
            return expired;
        }

        public void setExpired(boolean expired) {
            this.expired = expired;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
    }

    /**
     * 证书验证结果 DTO
     */
    public static class CertificateVerifyResult {
        private String alias;
        private boolean valid;
        private boolean expired;
        private String message;

        // Getters and Setters
        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public boolean isExpired() {
            return expired;
        }

        public void setExpired(boolean expired) {
            this.expired = expired;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}


