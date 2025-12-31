// Input: OFD/PDF 签名文件
// Output: 数字签名验证结果
// Pos: NexusCore compliance/signature
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.nio.file.Path;

/**
 * 数字签名验证器接口
 * 
 * PRD 来源: PRD 3.1 - 文件哈希 + 数字签名校验（支持 SM2/SM3）
 */
public interface DigitalSignatureVerifier {
    /**
     * 验证文件数字签名
     * 
     * @param filePath 签名文件路径
     * @return 签名验证结果
     */
    SignatureVerifyResult verify(Path filePath);
}
