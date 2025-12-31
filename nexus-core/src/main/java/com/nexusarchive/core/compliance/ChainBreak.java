// Input: 链式断裂点
// Output: 断裂点 DTO
// Pos: NexusCore compliance/audit
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

/**
 * 审计链断裂点
 */
public record ChainBreak(
    long seq,
    String expectedHash,
    String actualHash
) {}
