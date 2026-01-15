// Input: Lombok、Java 标准库
// Output: ScoredCandidate DTO
// Pos: 匹配引擎/DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 评分后的候选文档
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredCandidate {
    
    private String docId;
    private String docNo;
    private String docType;
    private String docTypeName;
    private LocalDate docDate;
    private BigDecimal amount;
    private String counterparty;
    
    // 评分
    private Integer score;
    private List<String> reasons;
    
    // 是否已被其他凭证占用
    private Boolean isLinked;
    private String linkedVoucherId;

    // Manual Getters
    public String getDocNo() { return docNo; }
}
