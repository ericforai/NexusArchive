package com.nexusarchive.dto.relation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RelationNodeDto {
    private String id;
    private String code;
    private String name;
    private String type;
    private String amount;
    private String date;
    private String status;
}
