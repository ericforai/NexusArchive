// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ErpSsoLaunchTicket 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("erp_sso_launch_ticket")
public class ErpSsoLaunchTicket {

    @TableId(type = IdType.INPUT)
    private String id;

    private String clientId;

    private String erpUserJobNo;

    private String nexusUserId;

    private String accbookCode;

    private String fondsCode;

    private String voucherNo;

    private LocalDateTime expiresAt;

    private Integer used;

    private LocalDateTime usedAt;

    private LocalDateTime createdTime;

    private LocalDateTime lastModifiedTime;
}
