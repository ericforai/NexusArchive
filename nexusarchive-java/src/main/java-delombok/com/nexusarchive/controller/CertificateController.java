// Input: io.swagger、Lombok、Spring Framework、Spring Security、Swagger/OpenAPI、Java 标准库
// Output: CertificateController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * PRD 来源: 电子签名模块
 * 提供证书的查询和管理功能
 */
@Tag(name = "证书管理", description = """
    数字证书管理接口。

    **功能说明:**
    - 查询密钥库中的所有证书
    - 查询单个证书详情
    - 验证证书有效性

    **证书类型:**
    - RSA: RSA 非对称加密证书
    - SM2: 国密非对称加密证书

    **密钥库格式:**
    - PKCS12: 标准 PKCS#12 格式（.p12/.pfx）

    **证书信息包括:**
    - alias: 证书别名
    - subject: 证书主体（持有者）
    - issuer: 证书颁发者
    - serialNumber: 序列号
    - validFrom: 生效日期
    - validTo: 过期日期
    - expired: 是否已过期
    - algorithm: 签名算法

    **验证结果:**
    - valid: 是否有效
    - expired: 是否过期
    - message: 状态描述

    **配置要求:**
    - signature.keystore.path: 密钥库路径
    - signature.keystore.password: 密钥库密码

    **使用场景:**
    - 证书有效期监控
    - 签名配置管理
    - 证书到期提醒

    **权限要求:**
    - SYSTEM_ADMIN 角色
    - super_admin 角色
    - SECURITY_ADMIN 角色
    """)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/admin/certificates")
@RequiredArgsConstructor
@Tag(name = "证书管理", description = "证书查询和管理功能")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'SECURITY_ADMIN')")
public class CertificateController {

    @Value("${signature.keystore.path:#{null}}")
    private String keystorePath;

    @Value("${signature.keystore.password:}")
    private String keystorePassword;

    /**
     * 查询所有证书
     */
    @GetMapping
    @Operation(
        summary = "查询所有证书",
        description = """
            查询密钥库中的所有证书。

            **返回数据包括:**
            - alias: 证书别名
            - subject: 证书主体（DN）
            - issuer: 证书颁发者（DN）
            - serialNumber: 序列号（16进制）
            - validFrom: 生效日期
            - validTo: 过期日期
            - expired: 是否已过期
            - algorithm: 签名算法（如 SHA256withRSA）

            **业务规则:**
            - 读取密钥库配置
            - 列出所有别名
            - 提取每个证书的信息

            **错误处理:**
            - 密钥库未配置时返回错误提示
            - 密钥库密码错误时返回错误

            **使用场景:**
            - 证书列表展示
            - 证书到期监控
            """,
        operationId = "listCertificates",
        tags = {"证书管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "密钥库未配置或读取失败")
    })
    public Result<List<CertificateInfo>> listCertificates() {
        try {
            KeyStore keyStore = loadKeyStore();
            if (keyStore == null) {
                return Result.fail("密钥库未配置，请在 application-dev.yml 中配置 signature.keystore.path 参数");
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
    @Operation(
        summary = "查询证书详情",
        description = """
            根据别名查询单个证书的详细信息。

            **路径参数:**
            - alias: 证书别名

            **返回数据包括:**
            完整的证书信息

            **业务规则:**
            - 别名不存在时返回错误

            **使用场景:**
            - 证书详情查看
            - 单个证书状态检查
            """,
        operationId = "getCertificate",
        tags = {"证书管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "证书不存在"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<CertificateInfo> getCertificate(
            @Parameter(description = "证书别名", required = true)
            @PathVariable String alias) {

        try {
            KeyStore keyStore = loadKeyStore();
            if (keyStore == null) {
                return Result.fail("密钥库未配置，请在 application-dev.yml 中配置 signature.keystore.path 参数");
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
    @Operation(
        summary = "验证证书有效性",
        description = """
            验证指定证书是否有效、是否过期。

            **路径参数:**
            - alias: 证书别名

            **返回数据包括:**
            - alias: 证书别名
            - valid: 是否有效
            - expired: 是否已过期
            - message: 状态描述

            **验证规则:**
            - 检查证书是否过期
            - 检查证书是否尚未生效
            - 验证证书链完整性

            **使用场景:**
            - 证书到期前检查
            - 签名前验证
            - 定期证书监控
            """,
        operationId = "verifyCertificate",
        tags = {"证书管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证完成"),
        @ApiResponse(responseCode = "400", description = "证书不存在"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<CertificateVerifyResult> verifyCertificate(
            @Parameter(description = "证书别名", required = true)
            @PathVariable String alias) {

        try {
            KeyStore keyStore = loadKeyStore();
            if (keyStore == null) {
                return Result.fail("密钥库未配置，请在 application-dev.yml 中配置 signature.keystore.path 参数");
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










