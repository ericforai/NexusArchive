// Input: Lombok、Java 标准库、匹配引擎枚举
// Output: LinkResult DTO
// Pos: 匹配引擎/DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import com.nexusarchive.engine.matching.enums.EvidenceRole;
import com.nexusarchive.engine.matching.enums.LinkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单个证据角色的关联结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkResult {
    
    private EvidenceRole evidenceRole;
    private String evidenceRoleName;
    private LinkType linkType;
    
    // 匹配结果
    private String matchedDocId;
    private String matchedDocNo;
    private Integer score;
    private List<String> reasons;
    private String status;  // MATCHED / MISSING / NEED_CONFIRM
    
    // 候选列表（供人工选择）
    private List<ScoredCandidate> candidates;
    
    // 冲突/缺失信息
    private String conflictReason;
    private String suggestion;
}
