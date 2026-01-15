// Input: Lombok、Java 标准库、匹配引擎枚举
// Output: MatchResult DTO
// Pos: 匹配引擎/DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import com.nexusarchive.engine.matching.enums.BusinessScene;
import com.nexusarchive.engine.matching.enums.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 匹配结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {
    
    private String taskId;
    private String batchTaskId;
    private String matchBatchId;
    private String voucherId;
    private String voucherNo;
    
    // 业务识别
    private BusinessScene scene;
    private String templateId;
    private String templateVersion;
    private BigDecimal confidence;
    private List<String> recognitionReasons;
    
    // 匹配状态
    private MatchStatus status;
    private List<String> missingDocs;
    private String message;
    
    // 匹配详情
    private List<LinkResult> links;
    
    // 配置指纹
    private String voucherHash;
    private String configHash;
    
    private LocalDateTime createdTime;
}
