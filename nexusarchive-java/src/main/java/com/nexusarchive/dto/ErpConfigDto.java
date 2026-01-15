// Input: Lombok、Java 标准库
// Output: ErpConfigDto 类
// Pos: 数据传输对象 - ERP配置对外响应
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ERP 配置 DTO（对外 API 响应）
 *
 * <p>用于 API 响应，不包含敏感信息（如 appSecret、clientSecret）。
 * 敏感字段在从 Entity 转换时自动过滤。</p>
 *
 * <p><strong>安全规则：</strong></p>
 * <ul>
 *   <li>不包含 appSecret、clientSecret 等敏感字段</li>
 *   <li>configJson 只包含非敏感配置（如 baseUrl、accbookCode）</li>
 *   <li>通过 ErpConfigDtoBuilder 从 ErpConfig 实体转换</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpConfigDto {

    /**
     * 配置 ID
     */
    private Long id;

    /**
     * 配置名称（如 用友 YonSuite 生产环境）
     */
    private String name;

    /**
     * ERP 类型（YONSUITE, KINGDEE, SAP, WEAVER, GENERIC）
     */
    private String erpType;

    /**
     * 清理后的配置 JSON（不含敏感信息）
     * 包含: baseUrl, accbookCode, extraConfig 等
     * 不包含: appSecret, clientSecret 等
     */
    private Map<String, Object> sanitizedConfig;

    /**
     * 是否激活（1=激活, 0=未激活）
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 最后修改时间
     */
    private LocalDateTime lastModifiedTime;

    /**
     * 账套-全宗映射（JSON 格式）
     */
    private String accbookMapping;

    /**
     * SAP 接口类型（仅当 erpType=SAP 时有效）
     */
    private String sapInterfaceType;

    /**
     * 获取 baseUrl（从 sanitizedConfig 中提取）
     */
    public String getBaseUrl() {
        return sanitizedConfig != null ? (String) sanitizedConfig.get("baseUrl") : null;
    }

    /**
     * 获取 appKey（从 sanitizedConfig 中提取）
     */
    public String getAppKey() {
        return sanitizedConfig != null ? (String) sanitizedConfig.get("appKey") : null;
    }

    /**
     * 获取 accbookCode（从 sanitizedConfig 中提取）
     */
    public String getAccbookCode() {
        return sanitizedConfig != null ? (String) sanitizedConfig.get("accbookCode") : null;
    }
}
