// Input: Lombok、Java 标准库、等
// Output: BatchSubmitRequest 类
// Pos: 数据传输对象 DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量提交归档申请请求 DTO
 */
@Data
public class BatchSubmitRequest {
    private List<String> fileIds;
    private String applicantId;
    private String applicantName;
    private String reason;
}
