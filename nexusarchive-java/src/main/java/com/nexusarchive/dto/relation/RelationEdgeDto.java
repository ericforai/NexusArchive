package com.nexusarchive.dto.relation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RelationEdgeDto {
    private String from;
    private String to;
    private String relationType;
    private String description;
}
