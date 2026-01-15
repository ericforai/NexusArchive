// Input: Jackson、Spring Framework
// Output: LegacyImportUtils 类
// Pos: 历史数据导入服务 - 工具方法层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.legacy;

import com.nexusarchive.dto.request.ImportRow;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 历史数据导入工具类
 * <p>
 * 提供数据解析、转换等通用工具方法
 * </p>
 */
@UtilityClass
public class LegacyImportUtils {

    /**
     * 构建 ImportRow 对象
     */
    public ImportRow buildImportRow(Map<String, String> rawData, int rowNumber) {
        ImportRow.ImportRowBuilder builder = ImportRow.builder()
            .rowNumber(rowNumber)
            .rawData(rawData);

        // 映射字段
        builder.fondsNo(getStringValue(rawData, "fonds_no"));
        builder.fondsName(getStringValue(rawData, "fonds_name"));
        builder.archiveYear(getIntegerValue(rawData, "archive_year"));
        builder.docType(getStringValue(rawData, "doc_type"));
        builder.title(getStringValue(rawData, "title"));
        builder.retentionPolicyName(getStringValue(rawData, "retention_policy_name"));
        builder.entityName(getStringValue(rawData, "entity_name"));
        builder.entityTaxCode(getStringValue(rawData, "entity_tax_code"));
        builder.docDate(getDateValue(rawData, "doc_date"));
        builder.amount(getBigDecimalValue(rawData, "amount"));
        builder.counterparty(getStringValue(rawData, "counterparty"));
        builder.voucherNo(getStringValue(rawData, "voucher_no"));
        builder.invoiceNo(getStringValue(rawData, "invoice_no"));
        builder.customMetadata(getStringValue(rawData, "custom_metadata"));
        builder.filePath(getStringValue(rawData, "file_path"));
        builder.fileHash(getStringValue(rawData, "file_hash"));

        return builder.build();
    }

    /**
     * 获取字符串值
     */
    public String getStringValue(Map<String, String> data, String key) {
        String value = data.get(key);
        return StringUtils.hasText(value) ? value : null;
    }

    /**
     * 获取整数值
     */
    public Integer getIntegerValue(Map<String, String> data, String key) {
        String value = data.get(key);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取 BigDecimal 值
     */
    public BigDecimal getBigDecimalValue(Map<String, String> data, String key) {
        String value = data.get(key);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取日期值
     */
    public LocalDate getDateValue(Map<String, String> data, String key) {
        String value = data.get(key);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        // 尝试多种日期格式
        String[] dateFormats = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd"};
        for (String format : dateFormats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }
        return null;
    }

    /**
     * 获取当前用户名
     */
    public String getCurrentUserName() {
        try {
            org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // 静默失败
        }
        return "SYSTEM";
    }
}
