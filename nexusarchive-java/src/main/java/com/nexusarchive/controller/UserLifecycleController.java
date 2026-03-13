// Input: UserLifecycleService, OnboardEmployeeRequest, OffboardEmployeeRequest, TransferEmployeeRequest
// Output: UserLifecycleController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.OffboardEmployeeRequest;
import com.nexusarchive.dto.request.OnboardEmployeeRequest;
import com.nexusarchive.dto.request.TransferEmployeeRequest;
import com.nexusarchive.service.UserLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户生命周期控制器
 *
 * 路径: /user-lifecycle
 * 功能：处理用户入职、离职、调岗等生命周期事件
 */
@RestController
@RequestMapping("/user-lifecycle")
@RequiredArgsConstructor
public class UserLifecycleController {

    private final UserLifecycleService userLifecycleService;

    /**
     * 用户入职处理
     *
     * POST /user-lifecycle/onboard
     * 自动创建账号、分配权限、发送临时密码
     */
    @PostMapping("/onboard")
    @ArchivalAudit(operationType = "USER_ONBOARD", resourceType = "USER", description = "用户入职处理")
    @PreAuthorize("hasAuthority('user:lifecycle:onboard')")
    public Result<String> onboardEmployee(@Validated @RequestBody OnboardEmployeeRequest request) {
        String userId = userLifecycleService.onboardEmployee(request);
        return Result.success("用户入职处理成功", userId);
    }

    /**
     * 用户离职处理
     *
     * POST /user-lifecycle/offboard
     * 自动停用账号、回收权限、生成离职报告
     */
    @PostMapping("/offboard")
    @ArchivalAudit(operationType = "USER_OFFBOARD", resourceType = "USER", description = "用户离职处理")
    @PreAuthorize("hasAuthority('user:lifecycle:offboard')")
    public Result<Void> offboardEmployee(@Validated @RequestBody OffboardEmployeeRequest request) {
        userLifecycleService.offboardEmployee(request);
        return Result.success("用户离职处理成功", null);
    }

    /**
     * 用户调岗处理
     *
     * POST /user-lifecycle/transfer
     * 自动调整组织、更新角色、记录调岗信息
     */
    @PostMapping("/transfer")
    @ArchivalAudit(operationType = "USER_TRANSFER", resourceType = "USER", description = "用户调岗处理")
    @PreAuthorize("hasAuthority('user:lifecycle:transfer')")
    public Result<Void> transferEmployee(@Validated @RequestBody TransferEmployeeRequest request) {
        userLifecycleService.transferEmployee(request);
        return Result.success("用户调岗处理成功", null);
    }
}
