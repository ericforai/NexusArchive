// Input: Lombok、Java 标准库、Swagger、MyBatis-Plus
// Output: AuditLogResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import com.nexusarchive.entity.SysAuditLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志响应 DTO
 * <p>
 * 从 SysAuditLog Entity 转换，隐藏以下敏感/大字段：
 * - dataBefore: 操作前数据（可能包含敏感信息，仅返回摘要）
 * - dataAfter: 操作后数据（可能包含敏感信息，仅返回摘要）
 * - prevLogHash: 前一条日志哈希（内部校验用）
 * - logHash: 当前日志哈希（内部校验用）
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "审计日志响应")
public class AuditLogResponse {

    /**
     * 日志ID
     */
    @Schema(description = "日志ID", example = "1234567890")
    private String id;

    /**
     * 操作者ID
     */
    @Schema(description = "操作者ID", example = "user001")
    private String userId;

    /**
     * 操作者姓名
     */
    @Schema(description = "操作者姓名", example = "张三")
    private String username;

    /**
     * 角色类型
     */
    @Schema(description = "角色类型", example = "system_admin")
    private String roleType;

    /**
     * 操作类型 (CAPTURE, ARCHIVE, MODIFY_META, DESTROY, PRINT, DOWNLOAD)
     */
    @Schema(description = "操作类型", example = "ARCHIVE")
    private String action;

    /**
     * 资源类型
     */
    @Schema(description = "资源类型", example = "ARCHIVE")
    private String resourceType;

    /**
     * 资源ID
     */
    @Schema(description = "资源ID", example = "archive001")
    private String resourceId;

    /**
     * 操作结果
     */
    @Schema(description = "操作结果", example = "SUCCESS")
    private String operationResult;

    /**
     * 风险等级
     */
    @Schema(description = "风险等级", example = "LOW")
    private String riskLevel;

    /**
     * 操作详情
     */
    @Schema(description = "操作详情", example = "创建新档案")
    private String details;

    /**
     * 会话ID
     */
    @Schema(description = "会话ID", example = "session123")
    private String sessionId;

    /**
     * 客户端IP地址
     */
    @Schema(description = "客户端IP地址", example = "192.168.1.1")
    private String clientIp;

    /**
     * MAC地址（脱敏处理）
     */
    @Schema(description = "MAC地址", example = "00:1A:2B:3C:4D:5E")
    private String macAddress;

    /**
     * 被操作对象的哈希值
     */
    @Schema(description = "对象摘要", example = "abc123...")
    private String objectDigest;

    /**
     * 用户代理
     */
    @Schema(description = "用户代理", example = "Mozilla/5.0...")
    private String userAgent;

    /**
     * 设备指纹
     */
    @Schema(description = "设备指纹", example = "fingerprint123")
    private String deviceFingerprint;

    /**
     * 追踪ID
     */
    @Schema(description = "追踪ID", example = "trace123")
    private String traceId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    private LocalDateTime createdTime;

    /**
     * 是否有详细变更数据
     */
    @Schema(description = "是否有详细变更数据", example = "true")
    private Boolean hasChanges;

    /**
     * 变更摘要（简要说明变更内容，不暴露完整数据）
     */
    @Schema(description = "变更摘要", example = "修改了档案标题")
    private String changeSummary;

    /**
     * 数据前长度（用于判断变更量）
     */
    @Schema(description = "数据前长度", example = "256")
    private Integer dataBeforeLength;

    /**
     * 数据后长度（用于判断变更量）
     */
    @Schema(description = "数据后长度", example = "512")
    private Integer dataAfterLength;

    /**
     * 从 Entity 转换为 DTO（基础版本，隐藏敏感字段）
     * 隐藏字段: dataBefore, dataAfter, prevLogHash, logHash
     */
    public static AuditLogResponse fromEntity(SysAuditLog entity) {
        // 判断是否有详细变更数据
        boolean hasChanges = (entity.getDataBefore() != null && !entity.getDataBefore().isEmpty())
                || (entity.getDataAfter() != null && !entity.getDataAfter().isEmpty());

        return AuditLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .roleType(entity.getRoleType())
                .action(entity.getAction())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .operationResult(entity.getOperationResult())
                .riskLevel(entity.getRiskLevel())
                .details(entity.getDetails())
                .sessionId(entity.getSessionId())
                .clientIp(entity.getClientIp())
                .macAddress(entity.getMacAddress())
                .objectDigest(entity.getObjectDigest())
                .userAgent(entity.getUserAgent())
                .deviceFingerprint(entity.getDeviceFingerprint())
                .traceId(entity.getTraceId())
                .createdTime(entity.getCreatedTime())
                .hasChanges(hasChanges)
                .dataBeforeLength(entity.getDataBefore() != null ? entity.getDataBefore().length() : 0)
                .dataAfterLength(entity.getDataAfter() != null ? entity.getDataAfter().length() : 0)
                .build();
    }

    /**
     * 从 Entity 转换为 DTO（包含变更摘要）
     */
    public static AuditLogResponse fromEntityWithSummary(SysAuditLog entity, String changeSummary) {
        boolean hasChanges = (entity.getDataBefore() != null && !entity.getDataBefore().isEmpty())
                || (entity.getDataAfter() != null && !entity.getDataAfter().isEmpty());

        return AuditLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .roleType(entity.getRoleType())
                .action(entity.getAction())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .operationResult(entity.getOperationResult())
                .riskLevel(entity.getRiskLevel())
                .details(entity.getDetails())
                .sessionId(entity.getSessionId())
                .clientIp(entity.getClientIp())
                .macAddress(entity.getMacAddress())
                .objectDigest(entity.getObjectDigest())
                .userAgent(entity.getUserAgent())
                .deviceFingerprint(entity.getDeviceFingerprint())
                .traceId(entity.getTraceId())
                .createdTime(entity.getCreatedTime())
                .hasChanges(hasChanges)
                .changeSummary(changeSummary)
                .dataBeforeLength(entity.getDataBefore() != null ? entity.getDataBefore().length() : 0)
                .dataAfterLength(entity.getDataAfter() != null ? entity.getDataAfter().length() : 0)
                .build();
    }

    /**
     * 脱敏 MAC 地址（保留前6位）
     */
    public static String maskMacAddress(String macAddress) {
        if (macAddress == null || macAddress.isEmpty()) {
            return "UNKNOWN";
        }
        if (macAddress.length() >= 8) {
            return macAddress.substring(0, 8) + "**:**:**";
        }
        return "****";
    }

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public void setAction(String action) { this.action = action; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public void setOperationResult(String operationResult) { this.operationResult = operationResult; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setDetails(String details) { this.details = details; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public void setObjectDigest(String objectDigest) { this.objectDigest = objectDigest; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public void setHasChanges(Boolean hasChanges) { this.hasChanges = hasChanges; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
    public void setDataBeforeLength(Integer dataBeforeLength) { this.dataBeforeLength = dataBeforeLength; }
    public void setDataAfterLength(Integer dataAfterLength) { this.dataAfterLength = dataAfterLength; }

    // Manual Builder
    public static AuditLogResponseBuilder builder() {
        return new AuditLogResponseBuilder();
    }

    public static class AuditLogResponseBuilder {
        private AuditLogResponse dto = new AuditLogResponse();
        public AuditLogResponseBuilder id(String id) { dto.setId(id); return this; }
        public AuditLogResponseBuilder userId(String userId) { dto.setUserId(userId); return this; }
        public AuditLogResponseBuilder username(String username) { dto.setUsername(username); return this; }
        public AuditLogResponseBuilder roleType(String roleType) { dto.setRoleType(roleType); return this; }
        public AuditLogResponseBuilder action(String action) { dto.setAction(action); return this; }
        public AuditLogResponseBuilder resourceType(String resourceType) { dto.setResourceType(resourceType); return this; }
        public AuditLogResponseBuilder resourceId(String resourceId) { dto.setResourceId(resourceId); return this; }
        public AuditLogResponseBuilder operationResult(String operationResult) { dto.setOperationResult(operationResult); return this; }
        public AuditLogResponseBuilder riskLevel(String riskLevel) { dto.setRiskLevel(riskLevel); return this; }
        public AuditLogResponseBuilder details(String details) { dto.setDetails(details); return this; }
        public AuditLogResponseBuilder sessionId(String sessionId) { dto.setSessionId(sessionId); return this; }
        public AuditLogResponseBuilder clientIp(String clientIp) { dto.setClientIp(clientIp); return this; }
        public AuditLogResponseBuilder macAddress(String macAddress) { dto.setMacAddress(macAddress); return this; }
        public AuditLogResponseBuilder objectDigest(String objectDigest) { dto.setObjectDigest(objectDigest); return this; }
        public AuditLogResponseBuilder userAgent(String userAgent) { dto.setUserAgent(userAgent); return this; }
        public AuditLogResponseBuilder deviceFingerprint(String deviceFingerprint) { dto.setDeviceFingerprint(deviceFingerprint); return this; }
        public AuditLogResponseBuilder traceId(String traceId) { dto.setTraceId(traceId); return this; }
        public AuditLogResponseBuilder createdTime(LocalDateTime createdTime) { dto.setCreatedTime(createdTime); return this; }
        public AuditLogResponseBuilder hasChanges(Boolean hasChanges) { dto.setHasChanges(hasChanges); return this; }
        public AuditLogResponseBuilder changeSummary(String changeSummary) { dto.setChangeSummary(changeSummary); return this; }
        public AuditLogResponseBuilder dataBeforeLength(Integer dataBeforeLength) { dto.setDataBeforeLength(dataBeforeLength); return this; }
        public AuditLogResponseBuilder dataAfterLength(Integer dataAfterLength) { dto.setDataAfterLength(dataAfterLength); return this; }
        public AuditLogResponse build() { return dto; }
    }
}
