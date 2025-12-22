// Input: Lombok、Java 标准库
// Output: PoolItemDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
     * 来源唯一标识（幂等性控制，如 YonSuite_xxx）
     */
    private String businessDocNo;

    /**
     * ERP原始凭证号（用户可读，如 记-3）
     */
    private String erpVoucherNo;

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

    /**
     * 来源系统 (如 YonSuite, Kingdee, Manual)
     */
    private String sourceSystem;

    /**
     * 文件名称
     */
    private String fileName;
}
