// Input: Spring Security、CustomUserDetails、批量审批请求 DTO
// Output: ApprovalRequestHelper 辅助类
// Pos: 控制器辅助工具层

package com.nexusarchive.controller.util;

import com.nexusarchive.controller.ArchiveApprovalController.ApprovalRequest;
import com.nexusarchive.dto.approval.BatchApprovalRequest;
import com.nexusarchive.dto.destruction.BatchDestructionApprovalRequest;
import com.nexusarchive.security.CustomUserDetails;

/**
 * 审批请求辅助类
 * <p>
 * 统一处理批量审批请求中的用户信息填充逻辑，
 * 避免在各个控制器中重复代码。
 * </p>
 */
public final class ApprovalRequestHelper {

    private ApprovalRequestHelper() {
        // 工具类，禁止实例化
    }

    /**
     * 为单审批请求设置审批人信息
     *
     * @param request 审批请求
     * @param user 当前认证用户
     */
    public static void setApproverInfo(ApprovalRequest request, CustomUserDetails user) {
        if (user != null) {
            if (request.getApproverId() == null) {
                request.setApproverId(user.getId());
            }
            if (request.getApproverName() == null) {
                request.setApproverName(user.getFullName());
            }
        }
        // 设置默认值（系统调用）
        if (request.getApproverId() == null) {
            request.setApproverId("system");
        }
        if (request.getApproverName() == null) {
            request.setApproverName("system");
        }
    }

    /**
     * 为批量审批请求设置审批人信息
     *
     * @param request 批量审批请求
     * @param user 当前认证用户
     */
    public static void setApproverInfo(BatchApprovalRequest request, CustomUserDetails user) {
        if (user != null) {
            if (request.getApproverId() == null) {
                request.setApproverId(user.getId());
            }
            if (request.getApproverName() == null) {
                request.setApproverName(user.getFullName());
            }
        }
        // 设置默认值（系统调用）
        if (request.getApproverId() == null) {
            request.setApproverId("system");
        }
        if (request.getApproverName() == null) {
            request.setApproverName("system");
        }
    }

    /**
     * 获取审批人ID，优先使用认证用户，否则返回 "system"
     *
     * @param user 当前认证用户
     * @return 审批人ID
     */
    public static String getApproverId(CustomUserDetails user) {
        return user != null ? user.getId() : "system";
    }
}
