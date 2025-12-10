package com.nexusarchive.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 电子凭证池列表项 DTO
 */
@Data
@Builder
public class PoolItemDto {

    /**
     * 文件ID
     */
    private String id;

    /**
     * 业务单据号 (来自 ERP 凭证号，优先显示)
     */
    private String businessDocNo;

    /**
     * 唯一流水号 (系统生成，用于前端显示)
     */
    private String code;

    /**
     * 来源系统
     */
    private String source;

    /**
     * 单据类型 (文件类型)
     */
    private String type;

    /**
     * 金额 (暂时为 "-", 待OCR识别后更新)
     */
    private String amount;

    /**
     * 入池时间 (上传时间)
     */
    private String date;

    /**
     * 解析状态
     */
    private String status;
}
