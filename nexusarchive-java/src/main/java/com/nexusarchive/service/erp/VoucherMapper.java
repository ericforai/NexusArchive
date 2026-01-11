// Input: ERP DTO、ArcFileContent、Lombok、Java 标准库
// Output: VoucherMapper 工具类
// Pos: 数据转换工具层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.erp;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * ERP 凭证 DTO 到档案文件内容的映射器
 * <p>
 * 负责将 ERP 系统返回的凭证数据转换为系统可识别的档案文件内容格式。
 * </p>
 */
@UtilityClass
public class VoucherMapper {

    /**
     * 将 ERP 凭证 DTO 转换为档案文件内容
     *
     * @param dto ERP 凭证数据传输对象
     * @return 档案文件内容实体
     */
    public ArcFileContent toArcFileContent(VoucherDTO dto) {
        ArcFileContent content = new ArcFileContent();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        content.setId(uuid);
        content.setFileName(dto.getVoucherNo() + ".pdf"); // PDF格式 (由VoucherPdfGeneratorService生成)
        content.setFileType("PDF");
        content.setFileSize(1024L); // Mock size, 实际大小由PDF生成后更新
        content.setCreatedTime(LocalDateTime.now());
        content.setCreator(dto.getCreator());
        content.setPreArchiveStatus(com.nexusarchive.entity.enums.PreArchiveStatus.READY_TO_ARCHIVE.getCode());

        // 设置ERP凭证号 (用户可读的单据编号)
        content.setErpVoucherNo(dto.getVoucherNo());
        // 设置凭证类型 (用于前端动态显示标签: COLLECTION_BILL, PAYMENT_BILL, VOUCHER 等)
        content.setVoucherType(dto.getStatus());
        // 设置来源系统
        content.setSourceSystem("用友YonSuite");

        // 填充显示字段 (凭证字号、摘要、业务日期)
        content.setVoucherWord(dto.getVoucherNo()); // 凭证字号 = 单据编号
        content.setDocDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : LocalDate.now()); // 业务日期

        // 生成摘要: 单据类型 + 供应商/客户
        String summary = dto.getSummary();
        if (summary == null || summary.isEmpty()) {
            String typeLabel = dto.getStatus() != null ? dto.getStatus() : "单据";
            summary = typeLabel + "-" + (dto.getVoucherNo() != null ? dto.getVoucherNo() : "");
        }
        content.setSummary(summary);

        // 生成临时档号 (YS-年月日-UUID前8位)
        String tempArchivalCode = "YS-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + uuid.substring(0, 8).toUpperCase();
        content.setArchivalCode(tempArchivalCode);

        // 存储路径 (由PDF生成服务更新为实际路径)
        String storagePath = "/data/archives/pre-archive/" + tempArchivalCode + "/" + dto.getVoucherNo() + ".pdf";
        content.setStoragePath(storagePath);

        return content;
    }
}
