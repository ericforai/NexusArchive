// Input: Spring Framework, Java Standard Library
// Output: CollectionMetadataInheritor
// Pos: Service Layer - Collection Subdomain

package com.nexusarchive.service.collection;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Year;

/**
 * 采集元数据继承器
 *
 * <p>职责：在四性检测前，从批次信息智能继承缺失的必填元数据。</p>
 *
 * <p>继承规则：</p>
 * <ul>
 *   <li>责任者 (Creator): 优先使用批次创建人，否则填"系统管理员"</li>
 *   <li>凭证日期 (DocDate): 基于批次的会计年度和期间推算为当月1号</li>
 *   <li>摘要 (Summary): 组合格批名称和文件名生成</li>
 * </ul>
 */
@Component
@Slf4j
public class CollectionMetadataInheritor {

    /**
     * 为缺失的必填字段设置默认值（智能继承）
     *
     * @param file        待补全的文件元数据
     * @param currentBatch 所属批次信息
     * @return 是否有字段被更新
     */
    public boolean inheritMissingMetadata(ArcFileContent file, CollectionBatch currentBatch) {
        boolean needsUpdate = false;

        if (file.getCreator() == null || file.getCreator().trim().isEmpty()) {
            file.setCreator(resolveCreator(currentBatch));
            needsUpdate = true;
        }

        if (file.getDocDate() == null) {
            file.setDocDate(resolveDocDate(currentBatch));
            needsUpdate = true;
        }

        if (file.getSummary() == null || file.getSummary().trim().isEmpty()) {
            file.setSummary(resolveSummary(currentBatch, file.getFileName()));
            needsUpdate = true;
        }

        return needsUpdate;
    }

    /**
     * 解析责任者：优先使用批次创建人
     */
    private String resolveCreator(CollectionBatch batch) {
        return batch.getCreatedBy() != null ? batch.getCreatedBy() : "系统管理员";
    }

    /**
     * 解析凭证日期：基于会计年度和期间推算为当月1号
     */
    private java.time.LocalDate resolveDocDate(CollectionBatch batch) {
        int year = parseFiscalYear(batch.getFiscalYear());
        int month = parseFiscalPeriod(batch.getFiscalPeriod());
        return java.time.LocalDate.of(year, month, 1);
    }

    /**
     * 解析会计年度，失败则使用当前年份
     */
    private int parseFiscalYear(String fiscalYear) {
        if (fiscalYear != null && !fiscalYear.trim().isEmpty()) {
            try {
                return Integer.parseInt(fiscalYear.trim());
            } catch (NumberFormatException e) {
                log.debug("无法解析批次会计年度: {}", fiscalYear);
            }
        }
        return Year.now().getValue();
    }

    /**
     * 解析会计期间，失败则使用1月
     */
    private int parseFiscalPeriod(String fiscalPeriod) {
        if (fiscalPeriod != null && !fiscalPeriod.trim().isEmpty()) {
            try {
                int month = Integer.parseInt(fiscalPeriod.trim());
                if (month >= 1 && month <= 12) {
                    return month;
                }
            } catch (NumberFormatException e) {
                log.debug("无法解析批次会计期间: {}", fiscalPeriod);
            }
        }
        return 1;
    }

    /**
     * 生成摘要：[批次名称] 文件名
     */
    private String resolveSummary(CollectionBatch batch, String fileName) {
        String cleanFileName = fileName;
        if (cleanFileName != null && cleanFileName.contains(".")) {
            cleanFileName = cleanFileName.substring(0, cleanFileName.lastIndexOf('.'));
        }
        return String.format("[%s] %s",
            batch.getBatchName(),
            cleanFileName != null ? cleanFileName : "未知文件");
    }
}
