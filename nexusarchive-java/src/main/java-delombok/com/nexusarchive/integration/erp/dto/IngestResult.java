// Input: Lombok、Java 标准库
// Output: IngestResult 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngestResult {
    private String sourceVoucherId;
    private String archivalCode;
    private String status;
    private String message;
}
