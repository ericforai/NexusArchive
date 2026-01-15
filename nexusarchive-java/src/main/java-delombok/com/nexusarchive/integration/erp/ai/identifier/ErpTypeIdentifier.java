// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/identifier/ErpTypeIdentifier.java
// Input: OpenApiDefinition, filename
// Output: ERP type identification
// Pos: AI 模块 - ERP 类型识别器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.identifier;

import com.nexusarchive.integration.erp.ai.parser.OpenApiDefinition;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * ERP 类型识别器
 * <p>
 * 从文件名和 OpenAPI 文档内容识别 ERP 类型
 * </p>
 */
@Component
public class ErpTypeIdentifier {

    /**
     * 识别 ERP 类型
     * <p>
     * 优先级顺序：
     * 1. 文件名关键字匹配
     * 2. API 路径关键字匹配
     * 3. 默认为通用类型
     * </p>
     *
     * @param filename OpenAPI 文档文件名
     * @param document OpenAPI 接口定义
     * @return ERP 类型
     */
    public ErpType identify(String filename, OpenApiDefinition document) {
        if (filename == null && document == null) {
            return ErpType.GENERIC;
        }

        // 优先检查文件名
        if (filename != null) {
            String lowerFileName = filename.toLowerCase();

            // YonSuite 关键字
            if (lowerFileName.contains("yonsuite") ||
                lowerFileName.contains("yonbip") ||
                lowerFileName.contains("yonyou")) {
                return ErpType.YONSUITE;
            }

            // Kingdee 关键字
            if (lowerFileName.contains("kingdee") ||
                lowerFileName.contains("k3cloud")) {
                return ErpType.KINGDEE;
            }

            // Weaver 关键字
            if (lowerFileName.contains("weaver") ||
                lowerFileName.contains("ecology")) {
                return ErpType.WEAVER;
            }
        }

        // 检查 API 路径
        if (document != null && document.getPath() != null) {
            String lowerPath = document.getPath().toLowerCase();

            if (lowerPath.contains("/yonbip/")) {
                return ErpType.YONSUITE;
            }

            if (lowerPath.contains("/k3cloud/")) {
                return ErpType.KINGDEE;
            }
        }

        // 默认为通用类型
        return ErpType.GENERIC;
    }

    /**
     * ERP 类型枚举
     */
    @Getter
    public enum ErpType {
        /**
         * 用友 YonSuite
         */
        YONSUITE("YonSuite", "yonsuite"),

        /**
         * 金蝶
         */
        KINGDEE("Kingdee", "kingdee"),

        /**
         * 泛微 OA
         */
        WEAVER("泛微OA", "weaver"),

        /**
         * 通用类型
         */
        GENERIC("通用", "generic");

        /**
         * 显示名称
         */
        private final String displayName;

        /**
         * 代码标识
         */
        private final String code;

        ErpType(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }
    }
}
