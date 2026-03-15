// Input: Java 标准库
// Output: ErrorCode 枚举
// Pos: 异常定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 * 统一管理系统中的错误消息和对应的HTTP状态码
 */
@Getter
public enum ErrorCode {

    // 通用错误 (4xx)
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),

    // 业务错误 (5xx)
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    // 档案文件相关错误
    FILE_NOT_FOUND(404, "File not found: %s"),
    FILE_NOT_FOUND_FOR_ARCHIVE(404, "File not found for archive: %s"),
    PHYSICAL_FILE_NOT_FOUND(404, "Physical file not found: %s"),
    FILE_NOT_BOUND_TO_ARCHIVE(400, "File not bound to an archive"),
    ARCHIVE_NOT_FOUND(404, "Archive not found: %s"),
    ARCHIVE_ACCESS_DENIED(403, "无权访问该档案文件"),
    ARCHIVE_CODE_EXISTS(409, "档号已存在: %s"),

    // 用户相关错误
    USERNAME_EXISTS(409, "用户名已存在"),
    USER_NOT_FOUND(404, "用户不存在"),
    INVALID_USER_STATUS(400, "非法状态值"),
    CANNOT_GET_CURRENT_USER(401, "无法获取当前登录用户"),

    // 案卷相关错误
    VOLUME_NOT_FOUND(404, "案卷不存在"),
    VOLUME_NOT_IN_DRAFT_STATUS(400, "只有草稿状态的案卷可以提交审核"),
    VOLUME_NOT_IN_PENDING_STATUS(400, "只有待审核状态的案卷可以审批"),
    VOLUME_NOT_IN_PENDING_STATUS_FOR_REJECTION(400, "只有待审核状态的案卷可以驳回"),
    VOLUME_NOT_ARCHIVED(400, "只有已归档的案卷可以移交"),
    VOLUME_ALREADY_TRANSFERRED(400, "案卷已在档案部门保管中"),
    NO_VOUCHERS_TO_ASSEMBLE(404, "该期间没有待组卷的凭证"),
    VOLUME_CANNOT_EXPORT(400, "只有已归档的案卷可以导出 AIP 包"),

    // 借阅相关错误
    BORROW_REQUEST_CANNOT_BE_EMPTY(400, "借阅请求不能为空"),
    BORROW_ARCHIVE_CANNOT_BE_EMPTY(400, "借阅档案不能为空"),
    BORROW_ARCHIVE_NOT_FOUND(404, "档案不存在，无法发起借阅"),
    BORROW_APPROVAL_PARAMS_CANNOT_BE_EMPTY(400, "审批参数不能为空"),
    BORROW_RECORD_NOT_FOUND(404, "借阅记录不存在或已被删除"),
    BORROW_INVALID_STATUS(400, "当前状态不允许执行此操作"),
    BORROW_USER_NOT_FOUND(401, "未获取到当前用户，请重新登录后重试"),

    // 密码相关错误
    PASSWORD_TOO_SHORT(400, "密码至少 8 位"),
    PASSWORD_MISSING_UPPERCASE(400, "密码需包含大写字母"),
    PASSWORD_MISSING_LOWERCASE(400, "密码需包含小写字母"),
    PASSWORD_MISSING_DIGIT(400, "密码需包含数字"),
    PASSWORD_MISSING_SPECIAL_CHAR(400, "密码需包含特殊字符"),

    // 系统配置相关错误
    SYSTEM_CONFIG_NOT_FOUND(404, "配置为空"),
    SYSTEM_CONFIG_KEY_CANNOT_BE_EMPTY(400, "配置键不能为空"),

    // 档号生成相关错误
    ARCHIVAL_CODE_GENERATION_FAILED(500, "Failed to generate archival code sequence"),
    MISSING_FONDS_CODE(400, "Missing Fonds Code for archival"),
    MISSING_FISCAL_YEAR(400, "Missing Fiscal Year for archival"),
    MISSING_CATEGORY_CODE(400, "Missing Category Code for archival"),
    MISSING_RETENTION_PERIOD(400, "Missing Retention Period for archival"),

    // 对账相关错误
    RECONCILIATION_TOO_FREQUENT(429, "对账请求过于频繁,请稍后再试"),

    // 权限相关错误
    NO_PERMISSION_TO_VIEW_ARCHIVE(403, "无权查看该档案"),

    // 状态转换相关错误
    VERSION_CONFLICT(409, "版本冲突，请刷新后重试"),
    INVALID_STATE_TRANSITION(400, "非法的状态转换");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取格式化后的错误消息
     * @param args 格式化参数
     * @return 格式化后的消息
     */
    public String getMessage(Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }
}
