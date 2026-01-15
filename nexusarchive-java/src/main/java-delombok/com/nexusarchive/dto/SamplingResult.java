// Input: Java 标准库
// Output: SamplingResult DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽检结果
 */
@Data
@Builder
public class SamplingResult {
    
    /**
     * 符合条件的总日志数
     */
    private int totalLogs;
    
    /**
     * 实际抽检的日志数
     */
    private int sampledLogs;
    
    /**
     * 验真结果
     */
    private ChainVerificationResult verificationResult;
    
    /**
     * 抽检的日志ID列表
     */
    private List<String> sampledLogIds = new ArrayList<>();
}

