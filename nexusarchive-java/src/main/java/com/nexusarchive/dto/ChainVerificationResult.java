// Input: Java 标准库、Lombok
// Output: ChainVerificationResult DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审计日志哈希链验证结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainVerificationResult {
    
    /**
     * 哈希链是否完整
     */
    private boolean chainIntact;
    
    /**
     * 总日志数
     */
    private int totalLogs;
    
    /**
     * 有效日志数
     */
    private int validLogs;
    
    /**
     * 无效日志数
     */
    private int invalidLogs;

    /**
     * 缺失日志数量
     */
    private int missingLogs;

    /**
     * 断链日志数量
     */
    private int brokenChainLogs;

    /**
     * 哈希不匹配（疑似篡改）数量
     */
    private int tamperedLogs;
    
    /**
     * 无效日志详情
     */
    @Builder.Default
    private List<VerificationResult> invalidResults = new ArrayList<>();
    
    /**
     * 验证时间
     */
    private LocalDateTime verifiedAt;
}




