// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ArchiveBatch 类
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
 * 归档批次实体类 (哈希链核心)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_archive_batch")
public class ArchiveBatch {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;

    private String prevBatchHash;

    private String currentBatchHash;

    private String chainedHash;

    private String hashAlgo;

    private Integer itemCount;

    private String operatorId;

    // [ADDED P0-4] 批次序列号，用于防止并发竞态
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Long batchSequence;

    private LocalDateTime createdTime;
}
