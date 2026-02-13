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
        // 按优先级检查各检测项：真实性 > 完整性 > 可用性 > 安全性
        var result = extractFirstFailed(
                report.getAuthenticity(), "真实性检测失败",
                report.getIntegrity(), "完整性检测失败",
                report.getUsability(), "可用性检测失败",
                report.getSafety(), "安全性检测失败"
        );
        return result != null ? result : "检测未通过";
    }

    /**
     * 提取第一个失败的检测项原因
     *
     * @param items 检测项及其默认消息（成对传入：checkItem, defaultMessage）
     * @return 失败原因或 null（如果全部通过）
     */
    private static String extractFirstFailed(Object... items) {
        for (int i = 0; i < items.length; i += 2) {
            com.nexusarchive.dto.sip.report.CheckItem checkItem =
                    (com.nexusarchive.dto.sip.report.CheckItem) items[i];
            String defaultMessage = (String) items[i + 1];

            if (checkItem != null && checkItem.getStatus() == OverallStatus.FAIL) {
                return checkItem.getMessage() != null
                        ? checkItem.getMessage()
                        : defaultMessage;
            }
        }
        return null;
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
