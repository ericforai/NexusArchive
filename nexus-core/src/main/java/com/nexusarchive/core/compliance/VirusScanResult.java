// Input: 扫描结果数据
// Output: 扫描结果 DTO
// Pos: NexusCore compliance/virus
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

/**
 * 病毒扫描结果
 */
public record VirusScanResult(
    boolean clean,
    String virusName,      // 若检测到病毒
    String scanEngine,     // "ClamAV"
    long scanDurationMs
) {
    public static VirusScanResult clean(long durationMs) {
        return new VirusScanResult(true, null, "ClamAV", durationMs);
    }
    
    public static VirusScanResult infected(String virusName, long durationMs) {
        return new VirusScanResult(false, virusName, "ClamAV", durationMs);
    }
    
    public static VirusScanResult error(String errorMessage) {
        return new VirusScanResult(false, "SCAN_ERROR: " + errorMessage, "ClamAV", 0);
    }
}
