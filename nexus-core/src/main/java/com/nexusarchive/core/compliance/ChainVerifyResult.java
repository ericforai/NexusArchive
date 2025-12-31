// Input: 链式校验结果
// Output: 校验结果 DTO
// Pos: NexusCore compliance/audit
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.util.List;

/**
 * 审计链校验结果
 */
public record ChainVerifyResult(
    boolean valid,
    long totalRecords,
    long verifiedRecords,
    List<ChainBreak> breaks
) {
    public static ChainVerifyResult success(long totalRecords) {
        return new ChainVerifyResult(true, totalRecords, totalRecords, List.of());
    }
    
    public static ChainVerifyResult failure(long totalRecords, long verifiedRecords, List<ChainBreak> breaks) {
        return new ChainVerifyResult(false, totalRecords, verifiedRecords, breaks);
    }
}
