// Input: Archive ID
// Output: void (Exception if failed)
// Pos: NexusCore service
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.service;

/**
 * 档案核心服务 (Sprint 2/3)
 * 处理归档、状态变更、四性检测集成
 */
public interface ArchiveService {
    /**
     * 执行归档操作 (含四性检测)
     * @param archiveId 档案ID
     */
    void archive(String archiveId);
}
