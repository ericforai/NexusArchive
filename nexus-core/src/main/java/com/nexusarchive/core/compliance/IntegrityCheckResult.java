// Input: 完整性校验结果数据
// Output: 校验结果 DTO
// Pos: NexusCore compliance/integrity
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.util.List;

/**
 * 完整性检测结果
 */
public record IntegrityCheckResult(
    boolean passed,
    List<IntegrityDiff> diffs
) {
    public static IntegrityCheckResult success() {
        return new IntegrityCheckResult(true, List.of());
    }
    
    public static IntegrityCheckResult failure(List<IntegrityDiff> diffs) {
        return new IntegrityCheckResult(false, diffs);
    }
}
