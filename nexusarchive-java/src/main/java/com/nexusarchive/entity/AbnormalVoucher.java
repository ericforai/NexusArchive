// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: AbnormalVoucher 类
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

import java.time.LocalDateTime;

/**
 * 异常凭证数据池
 * 用于存储接收失败或校验失败的 SIP 数据，供人工修正
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_abnormal_voucher")
public class AbnormalVoucher {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String requestId;
    
    private String sourceSystem;
    
    private String voucherNumber;
    
    /**
     * 原始 SIP 数据 (JSON 格式)
     */
    private String sipData;
    
    /**
     * 失败原因
     */
    private String failReason;
    
    /**
     * 状态: PENDING(待处理), RETRYING(重试中), IGNORED(已忽略), RESOLVED(已解决)
     */
    private String status;
    
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
    
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
}
