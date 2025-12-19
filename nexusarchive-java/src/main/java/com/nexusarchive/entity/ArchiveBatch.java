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

    private LocalDateTime createdTime;
}
