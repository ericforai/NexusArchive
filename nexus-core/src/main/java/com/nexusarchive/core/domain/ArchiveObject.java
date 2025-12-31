// Input: 档案实体
// Output: 领域对象
// Pos: NexusCore domain
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 档案领域对象 (映射 acc_archive 表)
 */
@Data
@TableName("acc_archive")
public class ArchiveObject {
    @TableId
    private String id;
    
    private String fondsNo;
    
    @TableField("fiscal_year")
    private String archiveYear; // 映射 fiscal_year
    
    private String title;
    
    private BigDecimal amount;
    
    private LocalDate docDate;
    
    // V70 新增字段
    private String counterparty;
    private String voucherNo;
    private String invoiceNo;
    
    private String categoryCode;
    
    private String status;
    
    // 审计字段
    private LocalDateTime createdTime;
    private LocalDateTime lastModifiedTime;
}
