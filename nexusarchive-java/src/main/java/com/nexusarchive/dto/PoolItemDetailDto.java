// Input: Lombok、Java 标准库、等
// Output: PoolItemDetailDto 类
// Pos: 数据传输对象 DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电子凭证池文件详情 DTO
 */
@Data
public class PoolItemDetailDto {
    private String id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String status;
    private LocalDateTime createdTime;
    private String fiscalYear;
    private String voucherType;
    private String creator;
    private String fondsCode;
    private String sourceSystem;
}
