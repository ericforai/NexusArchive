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
}
