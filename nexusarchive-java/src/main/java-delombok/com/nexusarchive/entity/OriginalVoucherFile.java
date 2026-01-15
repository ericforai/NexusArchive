// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: OriginalVoucherFile 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 原始凭证文件实体
 * 对应表: arc_original_voucher_file
 * 
 * 支持一个原始凭证关联多个文件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_original_voucher_file")
public class OriginalVoucherFile {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 关联原始凭证ID
     */
    private String voucherId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型: PDF/OFD/XML/JPG/PNG
     */
    private String fileType;

    /**
     * 文件大小 (bytes)
     */
    private Long fileSize;

    /**
     * 存储路径
     */
    private String storagePath;

    /**
     * 文件哈希值
     */
    private String fileHash;

    /**
     * 哈希算法: SM3/SHA256
     */
    @Builder.Default
    private String hashAlgorithm = "SM3";

    /**
     * 接收时的原始哈希
     */
    private String originalHash;

    // ===== 电子签章 (财会〔2020〕6号) =====

    /**
     * 电子签名值
     */
    private byte[] signValue;

    /**
     * 数字证书 Base64
     */
    private String signCert;

    /**
     * 签名时间
     */
    private LocalDateTime signTime;

    /**
     * TSA 时间戳
     */
    private byte[] timestampToken;

    // ===== 文件分类 =====

    /**
     * 文件角色: PRIMARY/ATTACHMENT/SUPPLEMENT
     */
    @Builder.Default
    private String fileRole = "PRIMARY";

    /**
     * 文件序号
     */
    @Builder.Default
    private Integer sequenceNo = 1;

    // ===== 审计字段 =====

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableLogic
    @Builder.Default
    private Integer deleted = 0;
}
