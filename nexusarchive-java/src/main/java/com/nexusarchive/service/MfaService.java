// Input: UserMfaConfig Entity
// Output: MfaService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.response.MfaSetupResponse;

/**
 * MFA（多因素认证）服务
 * 
 * 功能：
 * 1. 启用/禁用 MFA
 * 2. TOTP 密钥生成和验证
 * 3. 备用码生成和管理
 * 
 * PRD 来源: Section 7.1 - 身份与账号生命周期
 */
public interface MfaService {
    
    /**
     * 初始化 MFA（生成密钥和二维码）
     * 
     * @param userId 用户ID
     * @return MFA 设置响应（包含密钥、二维码URL、备用码）
     */
    MfaSetupResponse setupMfa(String userId);
    
    /**
     * 启用 MFA
     * 
     * @param userId 用户ID
     * @param verificationCode 验证码（用于确认设置）
     */
    void enableMfa(String userId, String verificationCode);
    
    /**
     * 禁用 MFA
     * 
     * @param userId 用户ID
     * @param password 用户密码（用于确认）
     */
    void disableMfa(String userId, String password);
    
    /**
     * 验证 TOTP 码
     * 
     * @param userId 用户ID
     * @param code TOTP 码
     * @return 是否验证通过
     */
    boolean verifyTotpCode(String userId, String code);
    
    /**
     * 验证备用码
     * 
     * @param userId 用户ID
     * @param backupCode 备用码
     * @return 是否验证通过
     */
    boolean verifyBackupCode(String userId, String backupCode);
    
    /**
     * 生成新的备用码
     * 
     * @param userId 用户ID
     * @return 备用码列表
     */
    java.util.List<String> generateBackupCodes(String userId);
    
    /**
     * 检查用户是否已启用 MFA
     * 
     * @param userId 用户ID
     * @return 是否已启用
     */
    boolean isMfaEnabled(String userId);
}


