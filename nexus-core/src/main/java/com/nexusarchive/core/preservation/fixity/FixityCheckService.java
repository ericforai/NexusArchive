// Input: Context for fixity check (e.g. limit)
// Output: Summary of checks
// Pos: NexusCore preservation/fixity
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.fixity;

/**
 * Service for periodic integrity verification of archived files.
 * Detects bit rot or unauthorized tampering on the storage layer.
 */
public interface FixityCheckService {

    /**
     * Perform fixity check on a batch of files.
     * @param limit Maximum number of files to check in this run
     * @return Number of files checked
     */
    int performBatchCheck(int limit);
}
