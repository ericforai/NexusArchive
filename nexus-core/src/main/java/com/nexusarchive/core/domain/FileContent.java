// Input: 电子文件元数据
// Output: 领域对象
// Pos: NexusCore domain
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 电子文件内容表 (映射 arc_file_content)
 * 存储文件物理路径与哈希信息
 */
@Data
@TableName("arc_file_content")
public class FileContent implements Serializable {
    @TableId
    private String id;

    @TableField("archival_code")
    private String archivalCode;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType; // PDF/OFD/XML

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_hash")
    private String fileHash;

    @TableField("hash_algorithm")
    private String hashAlgorithm;

    @TableField("storage_path")
    private String storagePath;

    @TableField("item_id")
    private String itemId; // 关联单据ID

    @TableField("fonds_code")
    private String fondsCode;

    @TableField("fiscal_year")
    private String fiscalYear;

    @TableField("created_time")
    private LocalDateTime createdTime;
}
