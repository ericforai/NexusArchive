// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/identifier/ScenarioName.java
// Input: Scenario naming result
// Output: Record containing scenario key, display name, and description
// Pos: AI 模块 - 场景名称记录
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.identifier;

/**
 * 场景名称
 * <p>
 * 记录自动生成的场景标识信息
 * </p>
 *
 * @param scenarioKey 场景标识码，例如 "SALESOUT_DOC_QUERY"
 * @param displayName 场景显示名称，例如 "查询销售出库单列表"
 * @param description 场景描述，例如 "AI 自动识别: /api/path (POST)"
 */
public record ScenarioName(
        String scenarioKey,
        String displayName,
        String description
) {
}
