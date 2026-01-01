// Input: Jackson、Lombok、Spring Framework
// Output: LegacyDataConverter 类
// Pos: 历史数据导入服务 - 数据转换层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.request.ImportRow;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.ImportValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史数据转换器
 * <p>
 * 负责将 ImportRow 转换为 Archive 实体并批量导入
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyDataConverter {

    private final ArchiveService archiveService;
    private final ImportValidationService validationService;
    private final ObjectMapper objectMapper;

    /**
     * 批量导入档案
     */
    public int batchImportArchives(List<ImportRow> rows, String operatorId) {
        int successCount = 0;

        for (ImportRow row : rows) {
            try {
                Archive archive = convertToArchive(row);
                archiveService.createArchive(archive, operatorId);
                successCount++;
            } catch (Exception e) {
                log.error("导入档案失败: rowNumber={}, error={}", row.getRowNumber(), e.getMessage());
                // 继续处理下一行，错误已在验证阶段收集
            }
        }

        return successCount;
    }

    /**
     * 将 ImportRow 转换为 Archive 实体
     */
    public Archive convertToArchive(ImportRow row) {
        Archive archive = new Archive();

        // 必需字段
        archive.setFondsNo(row.getFondsNo());
        archive.setFiscalYear(String.valueOf(row.getArchiveYear()));
        archive.setCategoryCode(row.getDocType()); // Use docType as categoryCode
        archive.setTitle(row.getTitle());

        // 解析保管期限
        String retentionPeriod = validationService.resolveRetentionPeriod(row.getRetentionPolicyName());
        if (retentionPeriod == null) {
            retentionPeriod = "PERMANENT"; // 默认值
        }
        archive.setRetentionPeriod(retentionPeriod);

        // 可选字段
        if (row.getDocDate() != null) {
            archive.setDocDate(row.getDocDate());
        }

        if (row.getAmount() != null) {
            archive.setAmount(row.getAmount());
        }

        // 设置扩展元数据
        if (StringUtils.hasText(row.getCustomMetadata())) {
            archive.setCustomMetadata(row.getCustomMetadata());
        } else {
            // 构建扩展元数据
            Map<String, Object> metadata = new HashMap<>();
            if (StringUtils.hasText(row.getCounterparty())) {
                metadata.put("counterparty", row.getCounterparty());
            }
            if (StringUtils.hasText(row.getVoucherNo())) {
                metadata.put("voucherNo", row.getVoucherNo());
            }
            if (StringUtils.hasText(row.getInvoiceNo())) {
                metadata.put("invoiceNo", row.getInvoiceNo());
            }
            if (!metadata.isEmpty()) {
                try {
                    archive.setCustomMetadata(objectMapper.writeValueAsString(metadata));
                } catch (Exception e) {
                    log.warn("构建扩展元数据失败: {}", e.getMessage());
                }
            }
        }

        // 设置状态
        archive.setStatus("archived");

        return archive;
    }
}
