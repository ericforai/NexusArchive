// Input: Hutool JSON、Lombok、Java 标准库
// Output: ErpConfigApiDtoBuilder 类
// Pos: 数据传输对象转换器 - ERP配置API DTO构建器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.entity.ErpConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * ErpConfigApiDto 构建器（用于 API 响应）
 *
 * <p>负责将 ErpConfig 实体转换为 ErpConfigDto（API 响应），自动清理敏感信息。</p>
 *
 * <p><strong>与 service.erp.ErpConfigDtoBuilder 的区别：</strong></p>
 * <ul>
 *   <li>此类用于 API 响应（敏感字段已清除）</li>
 *   <li>service.erp.ErpConfigDtoBuilder 用于适配器调用（包含解密后的敏感信息）</li>
 * </ul>
 *
 * <p><strong>安全规则：</strong></p>
 * <ul>
 *   <li>自动过滤敏感字段：appSecret, clientSecret 及其 _encrypted 后缀版本</li>
 *   <li>保留非敏感配置：baseUrl, appKey, accbookCode, extraConfig 等</li>
 *   <li>类型安全：使用明确的 Map 而不是原始 JSON 字符串</li>
 * </ul>
 */
public final class ErpConfigApiDtoBuilder {

    /**
     * 敏感字段列表（需要从 configJson 中移除）
     */
    private static final String[] SENSITIVE_FIELDS = {
            "appSecret",
            "clientSecret",
            "appSecret_encrypted",
            "clientSecret_encrypted",
            "password",
            "token",
            "privateKey"
    };

    private ErpConfigApiDtoBuilder() {
        // 工具类，禁止实例化
    }

    /**
     * 将 ErpConfig 实体转换为对外 API DTO（自动清理敏感信息）
     *
     * @param entity ERP 配置实体
     * @return 对外 API DTO，敏感字段已清理
     */
    public static ErpConfigDto toDto(ErpConfig entity) {
        if (entity == null) {
            return null;
        }

        Map<String, Object> sanitizedConfig = sanitizeConfigJson(entity.getConfigJson());

        return ErpConfigDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .erpType(entity.getErpType())
                .sanitizedConfig(sanitizedConfig)
                .isActive(entity.getIsActive())
                .createdTime(entity.getCreatedTime())
                .lastModifiedTime(entity.getLastModifiedTime())
                .accbookMapping(entity.getAccbookMapping())
                .sapInterfaceType(entity.getSapInterfaceType())
                .build();
    }

    /**
     * 清理 configJson 中的敏感字段
     *
     * @param configJson 原始 JSON 字符串
     * @return 清理后的 Map
     */
    private static Map<String, Object> sanitizeConfigJson(String configJson) {
        Map<String, Object> result = new HashMap<>();

        if (configJson == null || configJson.isEmpty()) {
            return result;
        }

        try {
            JSONObject json = JSONUtil.parseObj(configJson);

            // 复制所有非敏感字段
            json.forEach((key, value) -> {
                if (!isSensitiveField(key)) {
                    result.put(key, value);
                }
            });

        } catch (Exception e) {
            // JSON 解析失败时返回空 Map
            // 生产环境应记录日志
        }

        return result;
    }

    /**
     * 检查字段是否为敏感字段
     *
     * @param fieldName 字段名
     * @return true 如果是敏感字段
     */
    private static boolean isSensitiveField(String fieldName) {
        for (String sensitive : SENSITIVE_FIELDS) {
            if (sensitive.equalsIgnoreCase(fieldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查字段名是否包含敏感关键词（用于动态字段检测）
     *
     * @param fieldName 字段名
     * @return true 如果包含敏感关键词
     */
    public static boolean isPotentiallySensitive(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lower = fieldName.toLowerCase();
        return lower.contains("secret")
                || lower.contains("password")
                || lower.contains("token")
                || lower.contains("privatekey")
                || lower.contains("private_key");
    }
}
