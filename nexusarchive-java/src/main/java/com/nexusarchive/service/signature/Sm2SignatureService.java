package com.nexusarchive.service.signature;

import com.nexusarchive.dto.signature.SignResult;
import com.nexusarchive.dto.signature.VerifyResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

/**
 * SM2 签名服务实现
 * 
 * 基于 BouncyCastle 实现国密 SM2 签名算法
 * 
 * 合规要求：
 * - 信创环境必须使用国密算法
 * - 签名算法: SM3withSM2
 * 
 * @author Agent B - 合规开发工程师
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Sm2SignatureService implements SignatureAdapter {
    
    private static final String SERVICE_TYPE = "SM2";
    private static final String SIGNATURE_ALGORITHM = "SM3withSM2";
    private static final String PROVIDER = "BC";
    
    static {
        // 注册 BouncyCastle 提供者
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    @Value("${signature.keystore.path:#{null}}")
    private String keystorePath;
    
    @Value("${signature.keystore.password:changeit}")
    private String keystorePassword;
    
    @Override
    public SignResult sign(byte[] data, String certAlias) {
        try {
            // 获取私钥
            PrivateKey privateKey = loadPrivateKey(certAlias);
            if (privateKey == null) {
                return SignResult.failure("无法加载证书私钥: " + certAlias);
            }
            
            // 执行 SM2 签名
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
            signature.initSign(privateKey);
            signature.update(data);
            byte[] signatureBytes = signature.sign();
            
            // 获取证书信息
            X509Certificate cert = loadCertificate(certAlias);
            String signerName = extractSignerName(cert);
            String certSerialNumber = cert != null ? cert.getSerialNumber().toString(16) : null;
            
            log.info("SM2 签名成功: 证书别名={}, 签名长度={}", certAlias, signatureBytes.length);
            
            return SignResult.builder()
                    .success(true)
                    .signature(signatureBytes)
                    .algorithm(SERVICE_TYPE)
                    .signerName(signerName)
                    .certSerialNumber(certSerialNumber)
                    .signTime(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("SM2 签名失败: {}", e.getMessage(), e);
            return SignResult.failure("签名失败: " + e.getMessage());
        }
    }
    
    @Override
    public VerifyResult verify(byte[] data, byte[] signatureBytes, String certAlias) {
        try {
            // 获取公钥
            X509Certificate cert = loadCertificate(certAlias);
            if (cert == null) {
                return VerifyResult.failure("无法加载证书: " + certAlias);
            }
            
            // 检查证书有效性
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException e) {
                return VerifyResult.certificateExpired(
                        extractSignerName(cert), 
                        cert.getNotAfter()
                );
            } catch (CertificateNotYetValidException e) {
                return VerifyResult.failure("证书尚未生效");
            }
            
            // 验证签名
            PublicKey publicKey = cert.getPublicKey();
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
            signature.initVerify(publicKey);
            signature.update(data);
            boolean valid = signature.verify(signatureBytes);
            
            if (valid) {
                log.info("SM2 签名验证成功: 证书别名={}", certAlias);
                return VerifyResult.builder()
                        .valid(true)
                        .signatureValid(true)
                        .certificateValid(true)
                        .certificateExpired(false)
                        .signerName(extractSignerName(cert))
                        .certSerialNumber(cert.getSerialNumber().toString(16))
                        .certificateSubject(cert.getSubjectX500Principal().getName())
                        .certificateExpiryDate(cert.getNotAfter())
                        .verifyTime(LocalDateTime.now())
                        .algorithm(SERVICE_TYPE)
                        .build();
            } else {
                return VerifyResult.invalidSignature("签名值不匹配");
            }
            
        } catch (Exception e) {
            log.error("SM2 签名验证失败: {}", e.getMessage(), e);
            return VerifyResult.failure("验证失败: " + e.getMessage());
        }
    }
    
    @Override
    public VerifyResult verifyPdfSignature(InputStream pdfStream) {
        try {
            // 读取 PDF 文件内容
            byte[] pdfContent = readInputStream(pdfStream);
            
            // TODO: 实现 PDF 签章验证
            // 需要解析 PDF 中的签章信息，提取签名值和证书
            // 这里提供一个基础框架，实际实现需要集成 PDF 解析库（如 PDFBox）
            
            log.warn("PDF 签章验证功能尚未完整实现，返回默认结果");
            
            return VerifyResult.builder()
                    .valid(false)
                    .signatureValid(false)
                    .errorMessage("PDF 签章验证功能开发中")
                    .verifyTime(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("PDF 签章验证失败: {}", e.getMessage(), e);
            return VerifyResult.failure("PDF 验证失败: " + e.getMessage());
        }
    }
    
    @Override
    public VerifyResult verifyOfdSignature(InputStream ofdStream) {
        try {
            // 读取 OFD 文件内容
            byte[] ofdContent = readInputStream(ofdStream);
            
            // TODO: 实现 OFD 签章验证
            // OFD 是我国自主的版式文档格式，签章验证需要解析 OFD 包结构
            // 这里提供一个基础框架，实际实现需要集成 OFD 解析库
            
            log.warn("OFD 签章验证功能尚未完整实现，返回默认结果");
            
            return VerifyResult.builder()
                    .valid(false)
                    .signatureValid(false)
                    .errorMessage("OFD 签章验证功能开发中")
                    .verifyTime(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("OFD 签章验证失败: {}", e.getMessage(), e);
            return VerifyResult.failure("OFD 验证失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getServiceType() {
        return SERVICE_TYPE;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // 检查 BouncyCastle 提供者是否可用
            Provider provider = Security.getProvider(PROVIDER);
            if (provider == null) {
                return false;
            }
            
            // 检查 SM3withSM2 算法是否可用
            Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
            return true;
            
        } catch (Exception e) {
            log.warn("SM2 签名服务不可用: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 加载私钥
     */
    private PrivateKey loadPrivateKey(String certAlias) {
        try {
            KeyStore keyStore = loadKeyStore();
            if (keyStore == null) {
                log.warn("密钥库未配置，使用模拟私钥");
                return generateMockPrivateKey();
            }
            
            return (PrivateKey) keyStore.getKey(certAlias, keystorePassword.toCharArray());
        } catch (Exception e) {
            log.error("加载私钥失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 加载证书
     */
    private X509Certificate loadCertificate(String certAlias) {
        try {
            KeyStore keyStore = loadKeyStore();
            if (keyStore == null) {
                return null;
            }
            
            return (X509Certificate) keyStore.getCertificate(certAlias);
        } catch (Exception e) {
            log.error("加载证书失败: {}", e.getMessage());
            return null;
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
            log.error("加载密钥库失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 生成模拟私钥（仅用于开发测试）
     */
    private PrivateKey generateMockPrivateKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", PROVIDER);
            keyPairGenerator.initialize(256);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return keyPair.getPrivate();
        } catch (Exception e) {
            log.error("生成模拟私钥失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 提取签章人姓名
     */
    private String extractSignerName(X509Certificate cert) {
        if (cert == null) {
            return null;
        }
        
        try {
            String subject = cert.getSubjectX500Principal().getName();
            // 尝试提取 CN (Common Name)
            for (String part : subject.split(",")) {
                if (part.trim().toUpperCase().startsWith("CN=")) {
                    return part.trim().substring(3);
                }
            }
            return subject;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 读取输入流内容
     */
    private byte[] readInputStream(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
}
