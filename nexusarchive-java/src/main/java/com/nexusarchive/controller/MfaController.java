// Input: Spring Framework、Lombok、Java 标准库、本地模块
// Output: MfaController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.DisableMfaRequest;
import com.nexusarchive.dto.request.EnableMfaRequest;
import com.nexusarchive.dto.request.VerifyBackupCodeRequest;
import com.nexusarchive.dto.request.VerifyTotpRequest;
import com.nexusarchive.dto.response.MfaSetupResponse;
import com.nexusarchive.security.CustomUserDetails;
import com.nexusarchive.service.MfaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MFA（多因素认证）控制器
 *
 * 路径: /mfa
 *
 * 功能：
 * 1. 查询 MFA 状态
 * 2. 初始化 MFA 设置
 * 3. 启用/禁用 MFA
 * 4. 验证 TOTP 码和备用码
 * 5. 生成备用码
 */
@RestController
@RequestMapping("/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;

    /**
     * 检查 MFA 是否启用
     *
     * GET /mfa/status
     */
    @GetMapping("/status")
    public Result<Boolean> getMfaStatus(@AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean enabled = mfaService.isMfaEnabled(userDetails.getId());
        return Result.success(enabled);
    }

    /**
     * 初始化 MFA（生成密钥和二维码）
     *
     * POST /mfa/setup
     */
    @PostMapping("/setup")
    @ArchivalAudit(operationType = "MFA_SETUP", resourceType = "MFA", description = "初始化 MFA 设置")
    @PreAuthorize("isAuthenticated()")
    public Result<MfaSetupResponse> setupMfa(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MfaSetupResponse response = mfaService.setupMfa(userDetails.getId());
        return Result.success("MFA 初始化成功", response);
    }

    /**
     * 启用 MFA
     *
     * POST /mfa/enable
     */
    @PostMapping("/enable")
    @ArchivalAudit(operationType = "MFA_ENABLE", resourceType = "MFA", description = "启用 MFA")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> enableMfa(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody EnableMfaRequest request) {
        mfaService.enableMfa(userDetails.getId(), request.getCode());
        return Result.success("MFA 已启用", null);
    }

    /**
     * 禁用 MFA
     *
     * POST /mfa/disable
     */
    @PostMapping("/disable")
    @ArchivalAudit(operationType = "MFA_DISABLE", resourceType = "MFA", description = "禁用 MFA")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> disableMfa(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DisableMfaRequest request) {
        mfaService.disableMfa(userDetails.getId(), request.getPassword());
        return Result.success("MFA 已禁用", null);
    }

    /**
     * 验证 TOTP 码
     *
     * POST /mfa/verify
     */
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> verifyTotpCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VerifyTotpRequest request) {
        boolean valid = mfaService.verifyTotpCode(userDetails.getId(), request.getCode());
        return Result.success(valid);
    }

    /**
     * 验证备用码
     *
     * POST /mfa/verify-backup
     */
    @PostMapping("/verify-backup")
    @ArchivalAudit(operationType = "MFA_BACKUP_VERIFY", resourceType = "MFA", description = "验证备用码")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> verifyBackupCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VerifyBackupCodeRequest request) {
        boolean valid = mfaService.verifyBackupCode(userDetails.getId(), request.getBackupCode());
        return Result.success(valid);
    }

    /**
     * 获取备用码
     *
     * POST /mfa/backup-codes
     */
    @PostMapping("/backup-codes")
    @ArchivalAudit(operationType = "MFA_BACKUP_GENERATE", resourceType = "MFA", description = "生成备用码")
    @PreAuthorize("isAuthenticated()")
    public Result<List<String>> generateBackupCodes(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<String> codes = mfaService.generateBackupCodes(userDetails.getId());
        return Result.success("备用码已生成", codes);
    }
}
