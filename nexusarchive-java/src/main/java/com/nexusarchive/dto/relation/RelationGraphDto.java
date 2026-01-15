// Input: Lombok、Java 标准库
// Output: RelationGraphDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.relation;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RelationGraphDto {
    private String centerId;
    private List<RelationNodeDto> nodes;
    private List<RelationEdgeDto> edges;
    
    /**
     * 原始查询档案ID（如果发生了自动转换）
     * 当用户输入非凭证档案时，系统自动查找关联的记账凭证作为中心，
     * 此字段记录用户原始输入的档案ID，用于前端高亮显示
     */
    private String originalQueryId;
    
    /**
     * 是否自动转换（true表示以原始档案为中心时自动找到凭证）
     */
    private Boolean autoRedirected;
    
    /**
     * 转换提示信息
     * 例如："已自动切换到关联的记账凭证查看完整业务链路"
     */
    private String redirectMessage;
}
