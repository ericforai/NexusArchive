package com.nexusarchive.dto.relation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComplianceStatusDto {
    private boolean passed;
    private int score;
    private Details details;
    private String checkDate;

    @Data
    @Builder
    public static class Details {
        private boolean authenticity;
        private boolean integrity;
        private boolean usability;
        private boolean safety;
    }
}
