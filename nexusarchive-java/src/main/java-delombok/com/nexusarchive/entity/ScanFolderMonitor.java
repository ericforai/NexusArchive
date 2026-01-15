// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ScanFolderMonitor 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件夹监控配置实体
 *
 * 对应表: scan_folder_monitor
 * 用于配置自动监控扫描文件夹，支持文件过滤、自动处理等功能
 */
@Data
@TableName("scan_folder_monitor")
public class ScanFolderMonitor implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 监控文件夹路径
     */
    private String folderPath;

    /**
     * 是否激活监控
     */
    private Boolean isActive = true;

    /**
     * 文件过滤器（支持通配符，如 *.pdf;*.jpg）
     */
    private String fileFilter = "*.pdf;*.jpg;*.jpeg;*.png";

    /**
     * 处理后是否自动删除源文件
     */
    private Boolean autoDelete = false;

    /**
     * 处理后移动到的目标路径（可选）
     */
    private String moveToPath;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
