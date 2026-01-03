// Input: FondsHistory Entity
// Output: FondsHistoryDetail DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 全宗沿革详情 DTO
 */
@Data
public class FondsHistoryDetail {
    
    private String id;
    private String fondsNo;
    private String eventType;
    private String fromFondsNo;
    private String toFondsNo;
    private LocalDate effectiveDate;
    private String reason;
    private String approvalTicketId;
    private Map<String, Object> snapshot;  // 解析后的快照信息
    private String createdBy;
    private LocalDateTime createdAt;
}


