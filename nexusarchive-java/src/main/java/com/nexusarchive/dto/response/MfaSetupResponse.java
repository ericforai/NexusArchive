// Input: Java 标准库
// Output: MfaSetupResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;

import java.util.List;

/**
 * MFA 设置响应 DTO
 */
@Data
public class MfaSetupResponse {
    
    /**
     * 密钥（用于生成二维码）
     */
    private String secretKey;
    
    /**
     * 二维码URL（用于扫码）
     */
    private String qrCodeUrl;
    
    /**
     * 备用码列表
     */
    private List<String> backupCodes;
    
    /**
     * 说明文本
     */
    private String instructions;
}


