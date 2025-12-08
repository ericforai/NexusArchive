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
}
