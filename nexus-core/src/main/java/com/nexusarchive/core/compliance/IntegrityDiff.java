// Input: 字段差异数据
// Output: 差异项 DTO
// Pos: NexusCore compliance/integrity
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

/**
 * 完整性差异项
 */
public record IntegrityDiff(
    String fieldName,      // 如 "amount", "invoice_no"
    String xmlValue,       // XML 中的值
    String formatValue,    // OFD/PDF 中的值
    String message
) {
    public static IntegrityDiff of(String fieldName, String xmlValue, String formatValue) {
        String msg = String.format("字段 '%s' 不一致: XML='%s', 版式文件='%s'", 
                fieldName, xmlValue, formatValue);
        return new IntegrityDiff(fieldName, xmlValue, formatValue, msg);
    }
}
