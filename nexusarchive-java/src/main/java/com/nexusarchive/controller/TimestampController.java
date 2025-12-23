// Input: io.swagger、Lombok、Spring Security、Spring Framework、等
// Output: TimestampController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.service.TimestampService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * 时间戳控制器
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
@RestController
@RequestMapping("/api/timestamp")
@RequiredArgsConstructor
@Tag(name = "时间戳服务", description = "时间戳请求和验证功能")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','SECURITY_ADMIN')")
public class TimestampController {

    private final TimestampService timestampService;

    /**
     * 请求时间戳
     */
    @PostMapping("/request")
    @Operation(summary = "请求时间戳", description = "为数据请求时间戳")
    public Result<TimestampService.TimestampResult> requestTimestamp(
            @Parameter(description = "待加时间戳的数据（Base64编码）", required = true) @RequestParam String data,
            @Parameter(description = "关联的档案ID") @RequestParam(required = false) String archiveId) {
        
        try {
            byte[] dataBytes = Base64.getDecoder().decode(data);
            TimestampService.TimestampResult result = timestampService.requestTimestamp(dataBytes);

            if (result.isSuccess()) {
                log.info("时间戳请求成功: 档案ID={}, 时间={}", archiveId, result.getTimestamp());
            } else {
                log.warn("时间戳请求失败: 档案ID={}, 错误={}", archiveId, result.getErrorMessage());
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("时间戳请求异常: {}", e.getMessage(), e);
            return Result.fail("时间戳请求失败: " + e.getMessage());
        }
    }

    /**
     * 验证时间戳
     */
    @PostMapping("/verify")
    @Operation(summary = "验证时间戳", description = "验证时间戳令牌")
    public Result<TimestampService.TimestampVerifyResult> verifyTimestamp(
            @Parameter(description = "原始数据（Base64编码）", required = true) @RequestParam String data,
            @Parameter(description = "时间戳令牌（Base64编码）", required = true) @RequestParam String timestampToken) {
        
        try {
            byte[] dataBytes = Base64.getDecoder().decode(data);
            TimestampService.TimestampVerifyResult result = timestampService.verifyTimestamp(dataBytes, timestampToken);

            return Result.success(result);
        } catch (Exception e) {
            log.error("时间戳验证异常: {}", e.getMessage(), e);
            return Result.fail("时间戳验证失败: " + e.getMessage());
        }
    }

    /**
     * 检查时间戳服务状态
     */
    @GetMapping("/status")
    @Operation(summary = "检查时间戳服务状态", description = "检查时间戳服务是否可用")
    public Result<ServiceStatus> getServiceStatus() {
        boolean available = timestampService.isAvailable();
        
        ServiceStatus status = new ServiceStatus();
        status.setAvailable(available);
        status.setMessage(available ? "时间戳服务可用" : "时间戳服务不可用，请检查配置");
        
        return Result.success(status);
    }

    /**
     * 服务状态 DTO
     */
    public static class ServiceStatus {
        private boolean available;
        private String message;

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}











