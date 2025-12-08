package com.nexusarchive.dto.relation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkedFileDto {
    private String id;
    private String name;
    private String type; // invoice, contract, bank_slip, other
    private String url;
    private String uploadDate;
    private String size;
}
