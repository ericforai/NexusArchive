// Input: Lombok、Java 标准库
// Output: ImportResult 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 历史数据导入结果
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    /**
     * 导入任务ID
     */
    private String importId;
    
    /**
     * 总行数
     */
    private int totalRows;
    
    /**
     * 成功导入行数
     */
    private int successRows;
    
    /**
     * 失败行数
     */
    private int failedRows;
    
    /**
     * 错误列表
     */
    private List<ImportError> errors;
    
    /**
     * 自动创建的全宗号列表
     */
    private List<String> createdFondsNos;
    
    /**
     * 自动创建的实体ID列表
     */
    private List<String> createdEntityIds;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 导入状态: SUCCESS, PARTIAL_SUCCESS, FAILED
     */
    private ImportStatus status;
    
    /**
     * 错误报告下载URL
     */
    private String errorReportUrl;
    
    public enum ImportStatus {
        SUCCESS,        // 全部成功
        PARTIAL_SUCCESS, // 部分成功
        FAILED          // 全部失败
    }
}



