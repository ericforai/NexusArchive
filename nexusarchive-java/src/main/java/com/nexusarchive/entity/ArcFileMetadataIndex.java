// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ArcFileMetadataIndex 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 智能解析元数据索引
 * 对应表: arc_file_metadata_index
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_file_metadata_index")
public class ArcFileMetadataIndex {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 关联的文件ID
     */
    private String fileId;

    /**
     * 发票代码
     */
    private String invoiceCode;

    /**
     * 发票号码
     */
    private String invoiceNumber;

    /**
     * 价税合计 (Total Amount)
     */
    private BigDecimal totalAmount;

    /**
     * 销售方名称
     */
    private String sellerName;

    /**
     * 开票日期
     */
    private LocalDate issueDate;

    /**
     * 解析时间
     */
    private LocalDateTime parsedTime;

    /**
     * 解析器类型 (e.g., XML_V1, PDF_REGEX)
     */
    private String parserType;
}
