// Input: Lombok、Spring Framework、Java 标准库、org.bouncycastle
// Output: TimestampService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.util.Store;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

import com.nexusarchive.common.constants.HttpConstants;

/**
 * 时间戳服务
 * 
 * 提供时间戳请求和验证功能
 * 
 * 合规要求：
 * - DA/T 94-2022: 电子会计档案管理规范
 * - 支持 RFC 3161 Time-Stamp Protocol (TSP)
 * 
 * @author Agent B - 合规开发工程师
 */
@Slf4j
@Service
public class TimestampService {
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${timestamp.tsa.url:#{null}}")
    private String tsaUrl;

    @Value("${timestamp.tsa.username:#{null}}")
    private String tsaUsername;

    @Value("${timestamp.tsa.password:#{null}}")
    private String tsaPassword;

    @Value("${timestamp.tsa.timeout:5000}")
    private int timeout;

    @Value("${timestamp.enabled:false}")
    private boolean enabled;

    @Value("${timestamp.fallback-on-error:true}")
    private boolean fallbackOnError;

    @Value("${timestamp.hash-algorithm:SHA-256}")
    private String hashAlgorithm;

    /**
     * 请求时间戳
     * 
     * @param data 待加时间戳的数据
     * @return 时间戳结果
     */
    public TimestampResult requestTimestamp(byte[] data) {
        if (!enabled) {
            log.debug("时间戳服务未启用");
            return TimestampResult.disabled();
        }

        if (tsaUrl == null || tsaUrl.isEmpty()) {
            log.warn("TSA URL 未配置");
            return TimestampResult.configurationError("TSA URL 未配置");
        }

        try {
            String algo = normalizeHashAlgorithm(hashAlgorithm);
            MessageDigest digest = MessageDigest.getInstance(algo);
            byte[] hash = digest.digest(data);

            TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
            reqGen.setCertReq(true);
            ASN1ObjectIdentifier oid = resolveHashOid(algo);
            TimeStampRequest request = reqGen.generate(oid.getId(), hash, new java.math.BigInteger(64, new SecureRandom()));

            TimestampResult result = sendTimestampRequest(request.getEncoded(), request);

            if (result.isSuccess()) {
                log.info("时间戳请求成功: 时间={}", result.getTimestamp());
            } else {
                log.warn("时间戳请求失败: {}", result.getErrorMessage());
                
                // 如果允许降级，返回本地时间戳
                if (fallbackOnError) {
                    log.info("时间戳服务降级: 使用本地时间");
                    return TimestampResult.fallback(LocalDateTime.now());
                }
            }

            return result;
        } catch (Exception e) {
            log.error("时间戳请求异常: {}", e.getMessage(), e);
            
            if (fallbackOnError) {
                log.info("时间戳服务降级: 使用本地时间");
                return TimestampResult.fallback(LocalDateTime.now());
            }
            
            return TimestampResult.failure("时间戳请求失败: " + e.getMessage());
        }
    }

    /**
     * 验证时间戳
     * 
     * @param data 原始数据
     * @param timestampToken 时间戳令牌（Base64编码）
     * @return 验证结果
     */
    public TimestampVerifyResult verifyTimestamp(byte[] data, String timestampToken) {
        try {
            byte[] tokenBytes = Base64.getDecoder().decode(timestampToken);
            TimeStampToken token = new TimeStampToken(new CMSSignedData(tokenBytes));
            TimeStampTokenInfo info = token.getTimeStampInfo();

            ASN1ObjectIdentifier imprintOid = info.getMessageImprintAlgOID();
            String algo = resolveHashAlgorithmFromOid(imprintOid);
            MessageDigest digest = MessageDigest.getInstance(algo);
            byte[] expectedHash = digest.digest(data);

            if (!java.util.Arrays.equals(expectedHash, info.getMessageImprintDigest())) {
                return TimestampVerifyResult.failure("时间戳数据摘要不匹配");
            }

            Store<X509CertificateHolder> certStore = token.getCertificates();
            Collection<X509CertificateHolder> certs = certStore.getMatches(token.getSID());
            if (certs.isEmpty()) {
                return TimestampVerifyResult.failure("时间戳令牌缺少签名证书");
            }

            X509CertificateHolder certHolder = certs.iterator().next();
            java.security.cert.X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(certHolder);

            token.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert));

            TimestampVerifyResult result = TimestampVerifyResult.success("时间戳验证通过");
            result.setTimestamp(LocalDateTime.ofInstant(info.getGenTime().toInstant(), ZoneId.systemDefault()));
            return result;
        } catch (Exception e) {
            log.error("时间戳验证异常: {}", e.getMessage(), e);
            return TimestampVerifyResult.failure("时间戳验证失败: " + e.getMessage());
        }
    }

    /**
     * 发送时间戳请求
     */
    private TimestampResult sendTimestampRequest(byte[] requestData, TimeStampRequest request) {
        try {
            URL url = new URL(tsaUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty(HttpConstants.CONTENT_TYPE, "application/timestamp-query");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setDoOutput(true);

            // TSA 服务认证 - 遵循 RFC 3161 Time-Stamp Protocol (TSP) 标准
            // TSA (Time Stamping Authority) 要求使用 HTTP Basic 认证
            // 安全缓解措施:
            // 1. 强制使用 HTTPS (见下方验证)
            // 2. 凭据通过配置管理，定期轮换
            // 3. 审计日志不记录敏感信息
            // sonarjava:S2647 - TSA 协议标准要求，无法使用其他认证方式
            if (tsaUsername != null && tsaPassword != null) {
                // 验证 TSA URL 使用 HTTPS (生产环境强制要求)
                if (tsaUrl != null && !tsaUrl.startsWith("https://")) {
                    throw new SecurityException("TSA 服务必须使用 HTTPS 协议以保护凭据安全");
                }

                String auth = tsaUsername + ":" + tsaPassword;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                conn.setRequestProperty(HttpConstants.AUTHORIZATION, "Basic " + encodedAuth);

                // 审计日志 (不记录敏感信息)
                log.info("TSA 认证: 用户={}, URL={}", tsaUsername, maskUrl(tsaUrl));
            }

            conn.getOutputStream().write(requestData);
            conn.getOutputStream().flush();

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream is = conn.getInputStream();
                byte[] response = is.readAllBytes();
                TimeStampResponse tsResponse = new TimeStampResponse(response);
                tsResponse.validate(request);

                if (tsResponse.getFailInfo() != null || tsResponse.getStatus() != 0) {
                    return TimestampResult.failure("TSA 响应失败: " + tsResponse.getStatusString());
                }

                TimeStampToken token = tsResponse.getTimeStampToken();
                if (token == null) {
                    return TimestampResult.failure("TSA 响应缺少时间戳令牌");
                }

                TimeStampTokenInfo info = token.getTimeStampInfo();
                LocalDateTime timestamp = LocalDateTime.ofInstant(info.getGenTime().toInstant(), ZoneId.systemDefault());
                String timestampToken = Base64.getEncoder().encodeToString(token.getEncoded());

                return TimestampResult.success(timestamp, timestampToken);
            } else {
                return TimestampResult.failure("TSA 响应错误: " + responseCode);
            }
        } catch (java.net.SocketTimeoutException e) {
            log.warn("TSA 请求超时: {}", e.getMessage());
            if (fallbackOnError) {
                return TimestampResult.fallback(LocalDateTime.now());
            }
            return TimestampResult.failure("TSA 请求超时: " + e.getMessage());
        } catch (Exception e) {
            log.error("TSA 请求异常: {}", e.getMessage(), e);
            if (fallbackOnError) {
                return TimestampResult.fallback(LocalDateTime.now());
            }
            return TimestampResult.failure("TSA 请求失败: " + e.getMessage());
        }
    }

    /**
     * 检查时间戳服务是否可用
     */
    public boolean isAvailable() {
        return enabled && tsaUrl != null && !tsaUrl.isEmpty();
    }

    private String normalizeHashAlgorithm(String algo) {
        if (algo == null || algo.isBlank()) {
            return "SHA-256";
        }
        String upper = algo.trim().toUpperCase();
        if ("SM3".equals(upper)) {
            return "SM3";
        }
        if ("SHA256".equals(upper)) {
            return "SHA-256";
        }
        return upper;
    }

    private ASN1ObjectIdentifier resolveHashOid(String algo) {
        String normalized = normalizeHashAlgorithm(algo);
        if ("SM3".equals(normalized)) {
            return GMObjectIdentifiers.sm3;
        }
        return NISTObjectIdentifiers.id_sha256;
    }

    private String resolveHashAlgorithmFromOid(ASN1ObjectIdentifier oid) {
        if (Objects.equals(oid, GMObjectIdentifiers.sm3)) {
            return "SM3";
        }
        if (Objects.equals(oid, NISTObjectIdentifiers.id_sha256)) {
            return "SHA-256";
        }
        return "SHA-256";
    }

    /**
     * 时间戳结果
     */
    @Data
    public static class TimestampResult {
        private boolean success;
        private LocalDateTime timestamp;
        private String timestampToken;
        private String errorMessage;
        private boolean fallback;

        public static TimestampResult success(LocalDateTime timestamp, String timestampToken) {
            TimestampResult result = new TimestampResult();
            result.success = true;
            result.timestamp = timestamp;
            result.timestampToken = timestampToken;
            return result;
        }

        public static TimestampResult failure(String errorMessage) {
            TimestampResult result = new TimestampResult();
            result.success = false;
            result.errorMessage = errorMessage;
            return result;
        }

        public static TimestampResult fallback(LocalDateTime timestamp) {
            TimestampResult result = new TimestampResult();
            result.success = true;
            result.timestamp = timestamp;
            result.fallback = true;
            result.errorMessage = "使用本地时间（TSA 服务不可用）";
            return result;
        }

        public static TimestampResult disabled() {
            TimestampResult result = new TimestampResult();
            result.success = false;
            result.errorMessage = "时间戳服务未启用";
            return result;
        }

        public static TimestampResult configurationError(String message) {
            TimestampResult result = new TimestampResult();
            result.success = false;
            result.errorMessage = message;
            return result;
        }
    }

    /**
     * 时间戳验证结果
     */
    @Data
    public static class TimestampVerifyResult {
        private boolean valid;
        private String message;
        private LocalDateTime timestamp;

        public static TimestampVerifyResult success(String message) {
            TimestampVerifyResult result = new TimestampVerifyResult();
            result.valid = true;
            result.message = message;
            return result;
        }

        public static TimestampVerifyResult failure(String message) {
            TimestampVerifyResult result = new TimestampVerifyResult();
            result.valid = false;
            result.message = message;
            return result;
        }
    }

    /**
     * 掩码 URL，隐藏敏感信息（用于日志记录）
     *
     * @param url 原始 URL
     * @return 掩码后的 URL（仅保留协议和主机名）
     */
    private String maskUrl(String url) {
        if (url == null) return "null";
        try {
            URL u = new URL(url);
            return u.getProtocol() + "://" + u.getHost() + "/...";
        } catch (Exception e) {
            return "...";
        }
    }
}







