// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ErpSsoClient 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("erp_sso_client")
public class ErpSsoClient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String clientId;

    private String clientSecret;

    private String clientName;

    private String status;

    private LocalDateTime createdTime;

    private LocalDateTime lastModifiedTime;
}
