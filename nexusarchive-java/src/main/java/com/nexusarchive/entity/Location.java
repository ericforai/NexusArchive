// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: Location 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库房/位置实体
 * 对应表: bas_location
 */
@Data
@TableName("bas_location")
public class Location {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 位置名称 (e.g., 1号库房, A区, 01架)
     */
    private String name;

    /**
     * 位置编码
     */
    private String code;

    /**
     * 类型: WAREHOUSE(库房), AREA(区域), SHELF(密集架/货架), BOX(档案盒)
     */
    private String type;

    /**
     * 父级ID
     */
    private String parentId;

    /**
     * 完整路径 (e.g., /1/A/01)
     */
    private String path;

    /**
     * 容量 (可存放档案数量)
     */
    private Integer capacity;

    /**
     * 已用数量
     */
    private Integer usedCount;

    /**
     * 状态: NORMAL, FULL, MAINTENANCE
     */
    private String status;

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    /**
     * RFID标签号 (如果是密集架或档案盒)
     */
    private String rfidTag;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    private Integer deleted;
}
