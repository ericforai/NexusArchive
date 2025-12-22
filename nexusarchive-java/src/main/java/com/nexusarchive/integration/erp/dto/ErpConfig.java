// Input: Lombok、Java 标准库
// Output: ErpConfig 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ERP 配置 DTO
 * 
 * @author Agent D (基础设施工程师)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpConfig {

    /**
     * 配置ID
     */
    private String id;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 适配器类型 (yonsuite, kingdee, generic)
     */
    private String adapterType;

    /**
     * API 基础 URL
     */
    private String baseUrl;

    /**
     * 应用 Key
     */
    private String appKey;

    /**
     * 应用 Secret (加密存储)
     */
    private String appSecret;

    /**
     * 租户ID (如适用)
     */
    private String tenantId;

    /**
     * 账套代码 (如适用)
     */
    private String accbookCode;

    /**
     * 额外配置 (JSON 格式)
     */
    private String extraConfig;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
