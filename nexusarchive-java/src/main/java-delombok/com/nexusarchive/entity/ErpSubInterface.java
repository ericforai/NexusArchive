// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ErpSubInterface 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ERP 子接口配置实体
 * 对应 sys_erp_sub_interface 表
 * 支持场景内子接口级别的开关控制
 */
@Data
@TableName("sys_erp_sub_interface")
public class ErpSubInterface {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的场景ID
    private Long scenarioId;

    // 接口标识 (如: LIST_QUERY, DETAIL_QUERY)
    private String interfaceKey;

    // 接口名称 (如: 凭证列表查询)
    private String interfaceName;

    // 接口描述
    private String description;

    // 是否启用
    private Boolean isActive;

    // 排序顺序
    private Integer sortOrder;

    // 接口配置参数 (JSON)
    private String configJson;

    private LocalDateTime createdTime;

    private LocalDateTime lastModifiedTime;
}
