// Input: Lombok、Java 标准库
// Output: ImportError 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入错误信息
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportError {
    /**
     * 行号（从1开始，包含表头）
     */
    private int rowNumber;
    
    /**
     * 字段名
     */
    private String fieldName;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 错误消息
     */
    private String errorMessage;
}


