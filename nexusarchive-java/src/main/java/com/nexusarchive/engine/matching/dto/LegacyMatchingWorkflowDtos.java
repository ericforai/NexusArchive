package com.nexusarchive.engine.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public final class LegacyMatchingWorkflowDtos {

    private LegacyMatchingWorkflowDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingScanResult {
        private String companyId;
        private int totalVouchers;
        private int matchedVouchers;
        private int unmatchedVouchers;
        private Map<String, Integer> typeDistribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceReport {
        private String period;
        private int totalVouchers;
        private int matchedVouchers;
        private int unmatchedVouchers;
        private double complianceRate;
        private List<ComplianceIssue> issues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceIssue {
        private String type;
        private String description;
        private int count;
        private String severity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingDocsResult {
        private int missingCount;
        private List<MissingDocItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingDocItem {
        private String voucherId;
        private String voucherType;
        private String voucherDate;
        private String reason;
    }
}
