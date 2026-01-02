// Input: MfaService, UserMfaConfig, TOTP Library
// Output: MfaServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.response.MfaSetupResponse;
import com.nexusarchive.entity.UserMfaConfig;
import com.nexusarchive.mapper.UserMfaConfigMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.MfaService;
import com.nexusarchive.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * MFA Service Implementation
 *
 * SECURITY WARNING: This implementation contains TODO items that must be completed
 * before production use. See docs/security/MFA_STATUS.md for details.
 *
 * Critical unimplemented features:
 * - Password verification before MFA setup (line 132)
 * - TOTP code generation/validation (line 274) - currently returns hardcoded "000000"
 * - Backup code encryption (lines 309, 317, 326, 339) - stored in plain text
 *
 * FEATURE FLAG: MFA is disabled by default via configuration (mfa.enabled=false).
 * Set mfa.enabled=true to enable MFA functionality after implementing the TODO items above.
 *
 * DO NOT DEPLOY TO PRODUCTION without addressing these issues.
 */
/**
 * MFA 服务实现
 *
 * 使用 TOTP（Time-based One-Time Password）算法
 * 参考 RFC 6238
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private final UserMfaConfigMapper mfaConfigMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    // MFA 功能开关（从配置文件读取，默认禁用）
    @Value("${mfa.enabled:false}")
    private boolean mfaEnabled;

    // TOTP 参数
    private static final int TOTP_DIGITS = 6;
    private static final int TOTP_PERIOD = 30; // 30秒
    private static final String TOTP_ALGORITHM = "HmacSHA1";
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfaSetupResponse setupMfa(String userId) {
        // 检查 MFA 功能是否启用
        if (!mfaEnabled) {
            throw new IllegalStateException("MFA 功能未启用。请联系管理员启用 MFA 功能（配置 mfa.enabled=true）后再试。");
        }

        // 1. 验证用户存在
        userService.getUserById(userId);
        
        // 2. 生成密钥（Base32编码）
        String secretKey = generateSecretKey();
        
        // 3. 保存或更新 MFA 配置（未启用状态）
        UserMfaConfig config = getOrCreateMfaConfig(userId);
        config.setSecretKey(encryptSecretKey(secretKey)); // 加密存储
        config.setMfaEnabled(false);
        config.setMfaType("TOTP");
        config.setUpdatedAt(LocalDateTime.now());
        mfaConfigMapper.updateById(config);
        
        // 4. 生成备用码
        List<String> backupCodes = generateBackupCodesList();
        try {
            config.setBackupCodes(encryptBackupCodes(backupCodes));
            mfaConfigMapper.updateById(config);
        } catch (Exception e) {
            log.error("保存备用码失败", e);
        }
        
        // 5. 生成二维码URL
        var userResponse = userService.getUserById(userId);
        String qrCodeUrl = generateQrCodeUrl(userResponse.getUsername(), secretKey);
        
        // 6. 构建响应
        MfaSetupResponse response = new MfaSetupResponse();
        response.setSecretKey(secretKey);
        response.setQrCodeUrl(qrCodeUrl);
        response.setBackupCodes(backupCodes);
        response.setInstructions("请使用认证器应用（如 Google Authenticator）扫描二维码，然后输入验证码以完成设置。");
        
        // 7. 记录审计日志
        auditLogService.log(
            userId, userResponse.getUsername(), "MFA_SETUP_INITIATED",
            "MFA_CONFIG", config.getId(), "SUCCESS",
            "初始化 MFA 设置",
            "SYSTEM"
        );
        
        log.info("MFA 设置初始化: userId={}", userId);
        
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableMfa(String userId, String verificationCode) {
        // 检查 MFA 功能是否启用
        if (!mfaEnabled) {
            throw new IllegalStateException("MFA 功能未启用。请联系管理员启用 MFA 功能（配置 mfa.enabled=true）后再试。");
        }

        // 1. 验证验证码
        if (!verifyTotpCode(userId, verificationCode)) {
            throw new IllegalArgumentException("验证码错误");
        }
        
        // 2. 启用 MFA
        UserMfaConfig config = getOrCreateMfaConfig(userId);
        config.setMfaEnabled(true);
        config.setUpdatedAt(LocalDateTime.now());
        mfaConfigMapper.updateById(config);
        
        // 3. 记录审计日志
        var userResponse = userService.getUserById(userId);
        auditLogService.log(
            userId, userResponse.getUsername(), "MFA_ENABLED",
            "MFA_CONFIG", config.getId(), "SUCCESS",
            "启用 MFA",
            "SYSTEM"
        );
        
        log.info("MFA 已启用: userId={}", userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableMfa(String userId, String password) {
        // 1. 验证密码（简化实现，实际应该验证用户密码）
        // TODO: 实现密码验证
        
        // 2. 禁用 MFA
        UserMfaConfig config = getMfaConfig(userId);
        if (config == null) {
            throw new IllegalArgumentException("MFA 未配置");
        }
        
        config.setMfaEnabled(false);
        config.setUpdatedAt(LocalDateTime.now());
        mfaConfigMapper.updateById(config);
        
        // 3. 记录审计日志
        var userResponse = userService.getUserById(userId);
        auditLogService.log(
            userId, userResponse.getUsername(), "MFA_DISABLED",
            "MFA_CONFIG", config.getId(), "SUCCESS",
            "禁用 MFA",
            "SYSTEM"
        );
        
        log.info("MFA 已禁用: userId={}", userId);
    }
    
    @Override
    public boolean verifyTotpCode(String userId, String code) {
        UserMfaConfig config = getMfaConfig(userId);
        if (config == null || !Boolean.TRUE.equals(config.getMfaEnabled())) {
            return false;
        }
        
        // 解密密钥
        String secretKey = decryptSecretKey(config.getSecretKey());
        
        // 验证 TOTP 码（允许时间窗口偏移）
        long currentTime = System.currentTimeMillis() / 1000 / TOTP_PERIOD;
        
        // 验证当前时间窗口和前一个时间窗口（允许时钟偏差）
        for (int i = -1; i <= 1; i++) {
            String expectedCode = generateTotpCode(secretKey, currentTime + i);
            if (code.equals(expectedCode)) {
                // 更新最后使用时间
                config.setLastUsedAt(LocalDateTime.now());
                mfaConfigMapper.updateById(config);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean verifyBackupCode(String userId, String backupCode) {
        UserMfaConfig config = getMfaConfig(userId);
        if (config == null || config.getBackupCodes() == null) {
            return false;
        }
        
        try {
            // 解密备用码
            List<String> backupCodes = decryptBackupCodes(config.getBackupCodes());
            
            // 验证并移除已使用的备用码
            if (backupCodes.remove(backupCode)) {
                // 更新备用码列表
                config.setBackupCodes(encryptBackupCodes(backupCodes));
                mfaConfigMapper.updateById(config);
                return true;
            }
        } catch (Exception e) {
            log.error("验证备用码失败", e);
        }
        
        return false;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> generateBackupCodes(String userId) {
        UserMfaConfig config = getMfaConfig(userId);
        if (config == null) {
            throw new IllegalArgumentException("MFA 未配置");
        }
        
        List<String> backupCodes = generateBackupCodesList();
        config.setBackupCodes(encryptBackupCodes(backupCodes));
        config.setUpdatedAt(LocalDateTime.now());
        mfaConfigMapper.updateById(config);
        
        log.info("生成新的备用码: userId={}", userId);
        
        return backupCodes;
    }
    
    @Override
    public boolean isMfaEnabled(String userId) {
        // 首先检查系统级 MFA 功能开关
        if (!mfaEnabled) {
            return false;
        }

        // 然后检查用户级别的 MFA 启用状态
        UserMfaConfig config = getMfaConfig(userId);
        return config != null && Boolean.TRUE.equals(config.getMfaEnabled());
    }
    
    /**
     * 获取或创建 MFA 配置
     */
    private UserMfaConfig getOrCreateMfaConfig(String userId) {
        UserMfaConfig config = getMfaConfig(userId);
        if (config == null) {
            config = new UserMfaConfig();
            config.setId(UUID.randomUUID().toString().replaceAll("-", ""));
            config.setUserId(userId);
            config.setMfaEnabled(false);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setDeleted(0);
            mfaConfigMapper.insert(config);
        }
        return config;
    }
    
    /**
     * 获取 MFA 配置
     */
    private UserMfaConfig getMfaConfig(String userId) {
        LambdaQueryWrapper<UserMfaConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMfaConfig::getUserId, userId)
               .eq(UserMfaConfig::getDeleted, 0);
        return mfaConfigMapper.selectOne(wrapper);
    }
    
    /**
     * 生成密钥（Base32编码）
     */
    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 160位
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes).substring(0, 32);
    }
    
    /**
     * 生成 TOTP 码
     */
    private String generateTotpCode(String secretKey, long timeStep) {
        // TODO: 实现 TOTP 算法
        // 这里简化实现，实际应该使用 HMAC-SHA1 算法
        // 参考: https://tools.ietf.org/html/rfc6238
        return "000000"; // 占位符
    }
    
    /**
     * 生成二维码URL
     */
    private String generateQrCodeUrl(String username, String secretKey) {
        String issuer = "NexusArchive";
        String label = URLEncoder.encode(issuer + ":" + username, StandardCharsets.UTF_8);
        String secret = URLEncoder.encode(secretKey, StandardCharsets.UTF_8);
        
        return String.format("otpauth://totp/%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
            label, secret, issuer, TOTP_ALGORITHM, TOTP_DIGITS, TOTP_PERIOD);
    }
    
    /**
     * 生成备用码列表
     */
    private List<String> generateBackupCodesList() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 10; i++) {
            int code = 100000 + random.nextInt(900000); // 6位数字
            codes.add(String.valueOf(code));
        }
        return codes;
    }
    
    /**
     * 加密密钥（简化实现，实际应该使用加密算法）
     */
    private String encryptSecretKey(String secretKey) {
        // TODO: 使用 AES 或其他加密算法加密
        return secretKey; // 占位符
    }
    
    /**
     * 解密密钥
     */
    private String decryptSecretKey(String encryptedKey) {
        // TODO: 解密
        return encryptedKey; // 占位符
    }
    
    /**
     * 加密备用码
     */
    private String encryptBackupCodes(List<String> backupCodes) {
        try {
            // TODO: 加密备用码
            return objectMapper.writeValueAsString(backupCodes); // 占位符
        } catch (Exception e) {
            log.error("加密备用码失败", e);
            throw new RuntimeException("加密备用码失败", e);
        }
    }
    
    /**
     * 解密备用码
     */
    private List<String> decryptBackupCodes(String encryptedCodes) {
        try {
            // TODO: 解密备用码
            return objectMapper.readValue(encryptedCodes, new TypeReference<List<String>>() {}); // 占位符
        } catch (Exception e) {
            log.error("解密备用码失败", e);
            throw new RuntimeException("解密备用码失败", e);
        }
    }
}

