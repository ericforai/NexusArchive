// Input: Jackson JsonNode
// Output: PdfDataParser 工具类
// Pos: PDF 数据解析工具层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;

/**
 * PDF 数据解析工具
 * <p>
 * 从 ERP 凭证 JSON 数据中提取辅助核算、现金流量等信息
 * </p>
 */
@UtilityClass
public class PdfDataParser {

    /**
     * 解析辅助核算项
     * clientAuxiliary: [{dataType, docType, code, name, value}, ...]
     *
     * @param body 分录数据节点
     * @return 辅助核算信息字符串
     */
    public String parseAuxiliaryItems(JsonNode body) {
        StringBuilder sb = new StringBuilder();

        // 尝试 clientAuxiliary 数组
        JsonNode clientAux = body.path("clientAuxiliary");
        if (clientAux.isArray() && clientAux.size() > 0) {
            for (JsonNode item : clientAux) {
                String name = item.path("name").asText("");
                String value = item.path("value").asText("");
                if (!name.isEmpty() || !value.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append("; ");
                    }
                    sb.append(name.isEmpty() ? value : name + ":" + value);
                }
            }
        }

        // 尝试 auxiliary Map
        JsonNode auxiliary = body.path("auxiliary");
        if (auxiliary.isObject() && auxiliary.size() > 0) {
            var fields = auxiliary.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String key = entry.getKey();
                String value = entry.getValue().asText("");
                if (!value.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append("; ");
                    }
                    sb.append(key).append(":").append(value);
                }
            }
        }

        return sb.toString();
    }

    /**
     * 解析现金流量项目
     * cashFlowItem: [{itemId, itemCode, itemName, negative, amountOriginal,
     * amountOrg, innerOrg}, ...]
     *
     * @param body 分录数据节点
     * @return 现金流量信息字符串
     */
    public String parseCashFlowItems(JsonNode body) {
        StringBuilder sb = new StringBuilder();

        JsonNode cashFlowItems = body.path("cashFlowItem");
        if (cashFlowItems.isArray() && cashFlowItems.size() > 0) {
            for (JsonNode item : cashFlowItems) {
                String itemName = item.path("itemName").asText("");
                String itemCode = item.path("itemCode").asText("");
                double amount = item.path("amountOriginal").asDouble(0.0);
                if (amount == 0.0) {
                    amount = item.path("amountOrg").asDouble(0.0);
                }
                boolean negative = item.path("negative").asBoolean(false);

                if (!itemName.isEmpty() || !itemCode.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append("; ");
                    }
                    String display = itemCode.isEmpty() ? itemName : itemCode + " " + itemName;
                    if (amount != 0) {
                        display += (negative ? " -" : " ") + String.format("%.2f", Math.abs(amount));
                    }
                    sb.append(display);
                }
            }
        }

        return sb.toString();
    }

    /**
     * 从 JSON 中获取文本值（尝试多个字段名）
     *
     * @param node JSON 节点
     * @param fieldNames 可能的字段名列表
     * @return 第一个非空值
     */
    public String getTextValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return node.get(fieldName).asText("");
            }
        }
        return "";
    }

    /**
     * 从 JSON 中获取金额值（尝试多个字段名）
     *
     * @param node JSON 节点
     * @param fieldNames 可能的字段名列表
     * @return 第一个非空金额值
     */
    public double getAmountValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return node.get(fieldName).asDouble(0.0);
            }
        }
        return 0.0;
    }
}
