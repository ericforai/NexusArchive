package com.nexusarchive.dto.sip.report;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 四性检测报告
 * Reference: DA/T 92-2022 Clause 6
 */
public class FourNatureReport {
    
    private String checkId;
    
    private LocalDateTime checkTime;
    
    private String archivalCode;
    
    private OverallStatus status;
    
    private CheckItem authenticity;
    
    private CheckItem integrity;
    
    private CheckItem usability;
    
    private CheckItem safety;

    public FourNatureReport() {}

    public String getCheckId() { return checkId; }
    public void setCheckId(String checkId) { this.checkId = checkId; }

    public LocalDateTime getCheckTime() { return checkTime; }
    public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }

    public String getArchivalCode() { return archivalCode; }
    public void setArchivalCode(String archivalCode) { this.archivalCode = archivalCode; }

    public OverallStatus getStatus() { return status; }
    public void setStatus(OverallStatus status) { this.status = status; }

    public CheckItem getAuthenticity() { return authenticity; }
    public void setAuthenticity(CheckItem authenticity) { this.authenticity = authenticity; }

    public CheckItem getIntegrity() { return integrity; }
    public void setIntegrity(CheckItem integrity) { this.integrity = integrity; }

    public CheckItem getUsability() { return usability; }
    public void setUsability(CheckItem usability) { this.usability = usability; }

    public CheckItem getSafety() { return safety; }
    public void setSafety(CheckItem safety) { this.safety = safety; }

    public static FourNatureReportBuilder builder() {
        return new FourNatureReportBuilder();
    }

    public static class FourNatureReportBuilder {
        private FourNatureReport report = new FourNatureReport();

        public FourNatureReportBuilder checkId(String checkId) { report.setCheckId(checkId); return this; }
        public FourNatureReportBuilder checkTime(LocalDateTime checkTime) { report.setCheckTime(checkTime); return this; }
        public FourNatureReportBuilder archivalCode(String archivalCode) { report.setArchivalCode(archivalCode); return this; }
        public FourNatureReportBuilder status(OverallStatus status) { report.setStatus(status); return this; }
        public FourNatureReportBuilder authenticity(CheckItem authenticity) { report.setAuthenticity(authenticity); return this; }
        public FourNatureReportBuilder integrity(CheckItem integrity) { report.setIntegrity(integrity); return this; }
        public FourNatureReportBuilder usability(CheckItem usability) { report.setUsability(usability); return this; }
        public FourNatureReportBuilder safety(CheckItem safety) { report.setSafety(safety); return this; }
        
        public FourNatureReport build() { return report; }
    }
}
