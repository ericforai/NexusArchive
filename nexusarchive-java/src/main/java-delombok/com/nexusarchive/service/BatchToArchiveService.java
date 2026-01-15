// Input: Spring Framework、Java 标准库
// Output: BatchToArchiveService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;

/**
 * 批次文件转换服务
 *
 * 负责将批量上传的文件转换为 acc_archive 档案记录
 * 符合 DA/T 94-2022 元数据同步捕获要求
 *
 * @see <a href="https://openstd.samr.gov.cn/bzgk/gb/newGbInfo?hcno=DD799F37A6B8E73F13405D186EEC73CF">DA/T 94-2022 电子会计档案管理规范</a>
 */
public interface BatchToArchiveService {

    /**
     * 从批次信息创建档案记录
     *
     * <p>在上传文件后立即创建档案记录，状态为 PENDING_METADATA。
     * 这样可以确保元数据与文件同步捕获，符合 DA/T 94-2022 第14条要求。</p>
     *
     * <p>创建的档案记录具有以下特征：</p>
     * <ul>
     *   <li>categoryCode = AC04 (其他会计资料/原始凭证附件)</li>
     *   <li>status = PENDING_METADATA (待补录)</li>
     *   <li>title = 文件名 (初始值，用户可修改)</li>
     * </ul>
     *
     * @param fileContent 上传的文件内容
     * @param batch 所属批次信息
     * @return 创建的档案记录
     */
    Archive createArchiveFromBatch(ArcFileContent fileContent, CollectionBatch batch);

    /**
     * 批量完成时，标记所有文件的预归档状态
     *
     * <p>当用户点击"完成上传"时调用，将批次中所有文件的档案记录状态更新为 PENDING_METADATA。</p>
     *
     * @param batchId 批次ID
     */
    void markBatchAsPendingMetadata(Long batchId);

    /**
     * 根据文件ID获取关联的档案ID
     *
     * @param fileId 文件ID (arc_file_content.id)
     * @return 档案ID (acc_archive.id)，不存在返回null
     */
    String getArchiveIdByFileId(String fileId);
}
