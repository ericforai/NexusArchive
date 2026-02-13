// Input: Four Nature Report DTO
// Output: FourNatureCheckHelper
// Pos: Service Layer - Collection Subdomain

package com.nexusarchive.service.collection;

import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.service.CollectionBatchService;

/**
 * 四性检测助手
 *
 * <p>职责：</p>
 * <ul>
 *   <li>解析四性检测报告结果</li>
 *   <li>提供失败原因提取</li>
 *   <li>维护检测统计信息</li>
 * </ul>
 */
public class FourNatureCheckHelper {

    /**
     * 从检测报告中提取失败原因
     *
     * @param report 四性检测报告
     * @return 失败原因描述
     */
    public static String extractFailureReason(FourNatureReport report) {
        if (report.getAuthenticity() != null &&
            report.getAuthenticity().getStatus() == OverallStatus.FAIL) {
            return "真实性检测失败";
        }
        if (report.getIntegrity() != null &&
            report.getIntegrity().getStatus() == OverallStatus.FAIL) {
            return "完整性检测失败";
        }
        if (report.getUsability() != null &&
            report.getUsability().getStatus() == OverallStatus.FAIL) {
            return "可用性检测失败";
        }
        if (report.getSafety() != null &&
            report.getSafety().getStatus() == OverallStatus.FAIL) {
            return "安全性检测失败";
        }
        return "检测未通过";
    }

    /**
     * 检测统计结果
     */
    public static class BatchCheckStatistics {
        private int checkedCount = 0;
        private int passedCount = 0;
        private final java.util.List<CollectionBatchService.FailedFileInfo> failedFiles = new java.util.ArrayList<>();

        public int getCheckedCount() { return checkedCount; }
        public int getPassedCount() { return passedCount; }
        public java.util.List<CollectionBatchService.FailedFileInfo> getFailedFiles() { return failedFiles; }
        public int getFailedCount() { return failedFiles.size(); }

        public void addPassed(String fileId, String fileName) {
            checkedCount++;
            passedCount++;
        }

        public void addFailed(String fileId, String fileName, String reason) {
            checkedCount++;
            failedFiles.add(new CollectionBatchService.FailedFileInfo(fileId, fileName, reason));
        }
    }
}
