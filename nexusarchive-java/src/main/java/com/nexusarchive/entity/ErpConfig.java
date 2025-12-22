// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ErpConfig 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_erp_config")
public class ErpConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String erpType; // KINGDEE, YONSUITE, GENERIC
    
    private String configJson; // JSON string storing host, username, password etc.
    
    private Integer isActive;
    
    private LocalDateTime createdTime;
    
    private LocalDateTime lastModifiedTime;

    /**
     * 关联组织ID
     */
    private String orgId;
}
