// Input: Java 标准库、本地模块
// Output: ArchiveSecurityService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.ArchiveBatch;
import com.nexusarchive.entity.ArcFileContent;
import java.util.List;

/**
 * 档案安全服务：处理哈希链、时间戳与长期保存安全
 */
public interface ArchiveSecurityService {
    
    /**
     * 为一组新归档的文件创建安全批次并挂接哈希链
     * 
     * @param batchNo 批次号 (通常由 IngestService 生成)
     * @param items 本批次归档的文件列表
     * @param operatorId 操作员
     * @return 创建的批次记录
     */
    ArchiveBatch createSecurityBatch(String batchNo, List<ArcFileContent> items, String operatorId);
    
    /**
     * 验证指定批次的哈希链完整性
     */
    boolean verifyBatchIntegrity(Long batchId);
    
    /**
     * 全库哈希链完整性巡检
     */
    void inspectFullChain();
}
