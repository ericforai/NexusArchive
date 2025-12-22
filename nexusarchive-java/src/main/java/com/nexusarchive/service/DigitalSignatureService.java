// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: DigitalSignatureService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.ArcFileContent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.cert.*;
import java.util.Date;

/**
 * 电子签名验证服务
 */
@Slf4j
@Service
public class DigitalSignatureService {
    
    /**
     * 验证文件的电子签名
     * @param file 包含签名信息的文件
     * @return 验证结果
     */
    public VerificationResult verifySignature(ArcFileContent file) {
        try {
            // 获取文件内容和签名值
            byte[] fileContent = getFileContent(file);
            byte[] signatureBytes = file.getSignValue(); // parseSignatureValue(file.getSignValue());
            
            // 获取签名证书
            X509Certificate cert = parseCertificate(file.getCertificate());
            
            // 验证签名
            boolean isValid = verifyDigitalSignature(fileContent, signatureBytes, cert);
            
            // 检查证书有效性
            boolean certValid = verifyCertificate(cert);
            boolean certExpired = isCertificateExpired(cert);
            
            return new VerificationResult(
                isValid && certValid && !certExpired,
                isValid,
                certValid,
                certExpired,
                cert.getSubjectX500Principal().getName(),
                cert.getNotAfter()
            );
            
        } catch (Exception e) {
            log.error("验证电子签名失败: {}", e.getMessage(), e);
            return new VerificationResult(false, false, false, true, 
                "验证失败", new Date());
        }
    }
    
    @org.springframework.beans.factory.annotation.Value("${archive.root.path}")
    private String archiveRootPath;

    /**
     * 获取文件内容
     */
    private byte[] getFileContent(ArcFileContent file) throws Exception {
        if (file.getStoragePath() == null) {
            throw new IllegalArgumentException("文件存储路径为空");
        }
        
        java.nio.file.Path path;
        // 如果StoragePath已经是绝对路径，则直接使用
        if (java.nio.file.Paths.get(file.getStoragePath()).isAbsolute()) {
            path = java.nio.file.Paths.get(file.getStoragePath());
        } else {
            // 否则拼接RootPath
            path = java.nio.file.Paths.get(archiveRootPath, file.getStoragePath());
        }
        
        if (!java.nio.file.Files.exists(path)) {
            throw new java.io.FileNotFoundException("物理文件不存在: " + path.toAbsolutePath());
        }
        
        return java.nio.file.Files.readAllBytes(path);
    }
    
    /**
     * 解析签名值
     */
    private byte[] parseSignatureValue(String signValue) throws Exception {
        // 实际实现应解析Base64编码的签名值
        // 这里简化处理
        return java.util.Base64.getDecoder().decode(signValue);
    }
    
    /**
     * 解析证书
     */
    private X509Certificate parseCertificate(String certificate) throws Exception {
        // 实际实现应解析Base64编码的证书
        // 这里简化处理
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] certBytes = java.util.Base64.getDecoder().decode(certificate);
        return (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
    }
    
    /**
     * 验证数字签名
     */
    private boolean verifyDigitalSignature(byte[] data, byte[] signature, X509Certificate cert) throws Exception {
        try {
            // 获取公钥
            PublicKey publicKey = cert.getPublicKey();
            
            // 根据签名算法选择签名验证器
            Signature sig;
            if (cert.getSigAlgName().contains("SHA256withRSA")) {
                sig = Signature.getInstance("SHA256withRSA");
            } else if (cert.getSigAlgName().contains("SHA1withRSA")) {
                sig = Signature.getInstance("SHA1withRSA");
            } else if (cert.getSigAlgName().contains("SM3withSM2")) {
                sig = Signature.getInstance("SM3withSM2");
            } else {
                throw new NoSuchAlgorithmException("不支持的签名算法: " + cert.getSigAlgName());
            }
            
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (Exception e) {
            log.error("签名验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证证书有效性
     */
    private boolean verifyCertificate(X509Certificate cert) throws Exception {
        try {
            // 检查证书有效期
            cert.checkValidity();
            
            // 这里可以添加证书链验证、CRL检查等
            
            return true;
        } catch (CertificateExpiredException e) {
            log.warn("证书已过期");
            return false;
        } catch (CertificateNotYetValidException e) {
            log.warn("证书尚未生效");
            return false;
        } catch (Exception e) {
            log.error("证书验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查证书是否过期
     */
    private boolean isCertificateExpired(X509Certificate cert) {
        try {
            cert.checkValidity();
            return false;
        } catch (CertificateExpiredException e) {
            return true;
        } catch (Exception e) {
            return true; // 其他验证问题也视为无效
        }
    }
    
    /**
     * 验证结果
     */
    @Data
    public static class VerificationResult {
        private boolean valid;
        private boolean signatureValid;
        private boolean certificateValid;
        private boolean certificateExpired;
        private String certificateSubject;
        private Date certificateExpiryDate;
        private String errorMessage;
        
        // 构造函数
        public VerificationResult(boolean valid, boolean signatureValid, boolean certificateValid, 
                               boolean certificateExpired, String certificateSubject, Date certificateExpiryDate) {
            this.valid = valid;
            this.signatureValid = signatureValid;
            this.certificateValid = certificateValid;
            this.certificateExpired = certificateExpired;
            this.certificateSubject = certificateSubject;
            this.certificateExpiryDate = certificateExpiryDate;
        }
        
        // 构造函数（错误情况）
        public VerificationResult(boolean valid, boolean signatureValid, boolean certificateValid, 
                               boolean certificateExpired, String errorMessage) {
            this.valid = valid;
            this.signatureValid = signatureValid;
            this.certificateValid = certificateValid;
            this.certificateExpired = certificateExpired;
            this.errorMessage = errorMessage;
        }
        
        /**
         * 获取错误信息
         */
        public String getErrorMessage() {
            if (errorMessage != null) {
                return errorMessage;
            }
            
            if (!signatureValid) {
                return "数字签名无效";
            }
            
            if (!certificateValid) {
                if (certificateExpired) {
                    return "数字证书已过期";
                } else {
                    return "数字证书无效";
                }
            }
            
            return null;
        }
    }
}