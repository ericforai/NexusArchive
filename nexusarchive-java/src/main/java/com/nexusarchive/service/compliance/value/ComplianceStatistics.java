// Input: Lombok、Java 标准库
// Output: ComplianceStatistics 类
// Pos: 业务服务层

package com.nexusarchive.service.compliance.value;

import lombok.Data;

/**
 * 符合性统计数据
 */
@Data
public class ComplianceStatistics {
    private int totalArchives;
    private int fullyCompliant;
    private int compliantWithWarnings;
    private int nonCompliant;
    private double complianceRate;
}
