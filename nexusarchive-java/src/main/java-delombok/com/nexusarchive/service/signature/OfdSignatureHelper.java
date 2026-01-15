// Input: Lombok、org.bouncycastle、org.ofdrw、Spring Framework、等
// Output: OfdSignatureHelper 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.signature;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.OFDSigner;
import org.ofdrw.sign.SignMode;
import org.ofdrw.sign.verify.OFDValidator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Security;

/**
 * OFD 电子签章助手
 * 基于 ofdrw-sign 和 ofdrw-gm 实现国密 SM2 自动签章
 */
@Slf4j
@Component
public class OfdSignatureHelper {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 对 OFD 文件进行电子签名
     * @param src 源文件
     * @param dest 目标文件
     * @param keyPath P12证书路径
     * @param password 证书密码
     */
    public void signOfd(Path src, Path dest, String keyPath, String password) throws IOException, GeneralSecurityException {
        // 使用 OFDRW 提供的 P12 容器实现
        // 注意: ofdrw-sign 默认使用 SM2/SM3 国密算法。
        // 如果证书不是 EC/SM2 类型（而是 RSA），签名可能失败。
        // 请确保 keystore 使用椭圆曲线 (EC) 密钥，例如使用 secp256k1 或 SM2 曲线。
        
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, dest)) {
            
            // 加载 P12 证书
            java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");
            try (java.io.InputStream fis = java.nio.file.Files.newInputStream(java.nio.file.Paths.get(keyPath))) {
                ks.load(fis, password.toCharArray());
            }
            
            String alias = ks.aliases().nextElement();
            java.security.PrivateKey privateKey = (java.security.PrivateKey) ks.getKey(alias, password.toCharArray());
            java.security.cert.Certificate certificate = ks.getCertificate(alias);

            String keyAlgorithm = privateKey.getAlgorithm();
            log.info("Keystore loaded. Key algorithm: {}", keyAlgorithm);
            
            if ("RSA".equalsIgnoreCase(keyAlgorithm)) {
                log.warn("RSA key detected. DigitalSignContainer in ofdrw-sign may not support RSA. Please use an EC/SM2 keystore.");
            }

            // DigitalSignContainer (PrivateKey) - attempts to auto-detect algorithm
            org.ofdrw.sign.signContainer.DigitalSignContainer signContainer = 
                new org.ofdrw.sign.signContainer.DigitalSignContainer(privateKey);
            
            // 设置签名模式
            signer.setSignMode(SignMode.WholeProtected);
            // 设置签名容器
            signer.setSignContainer(signContainer);
            
            // 执行签名
            signer.exeSign();
            
            log.info("OFD 签名成功: {}", dest);
        }
    }

    /**
     * 验证 OFD 签名
     * @param src OFD 文件路径
     * @return 是否验证通过
     */
    public boolean verifyOfd(Path src) {
        try (OFDReader reader = new OFDReader(src);
             OFDValidator validator = new OFDValidator(reader)) {
            
            // 执行验证
            validator.exeValidate();
            
            // 检查验证结果
            // validator.exeValidate() 如果失败通常会抛出异常
            // 只要没有抛出异常，视为通过
            return true;
            
        } catch (Exception e) {
            log.error("OFD 验签失败: {}", e.getMessage());
            return false;
        }
    }
}
