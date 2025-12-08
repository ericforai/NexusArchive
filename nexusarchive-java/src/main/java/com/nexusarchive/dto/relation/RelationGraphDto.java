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
