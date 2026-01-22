// Input: Spring Framework、MyBatis-Plus、Java 标准库
// Output: ArchiveSubmitBatchService 接口
// Pos: 服务层接口
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.ArchiveSubmitBatch;
import com.nexusarchive.entity.ArchiveBatchItem;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 归档提交批次服务接口
 *
 * 管理从预归档库到正式档案库的批量归档流程。
 */
public interface ArchiveSubmitBatchService {

    // ========== 批次管理 ==========

    /**
     * 创建归档批次
     *
     * @param fondsId 全宗 ID
     * @param periodStart 期间起始日期
     * @param periodEnd 期间结束日期
     * @param createdBy 创建人 ID
     * @return 创建的批次
     */
    ArchiveSubmitBatch createBatch(String fondsId, LocalDate periodStart, LocalDate periodEnd, Long createdBy);

    /**
     * 获取批次详情
     */
    ArchiveSubmitBatch getBatch(Long batchId);

    /**
     * 分页查询批次
     */
    IPage<ArchiveSubmitBatch> listBatches(Page<ArchiveSubmitBatch> page, String fondsId, String status);

    /**
     * 按全宗查询所有批次
     */
    List<ArchiveSubmitBatch> listBatchesByFonds(String fondsId, String status);

    /**
     * 删除批次（仅 PENDING 状态可删）
     */
    void deleteBatch(Long batchId);

    // ========== 批次条目管理 ==========

    /**
     * 添加凭证到批次
     *
     * @param batchId 批次 ID
     * @param voucherIds 凭证 ID 列表 (String)
     * @return 添加的条目数
     */
    int addVouchersToBatch(Long batchId, List<String> voucherIds);

    /**
     * 添加单据到批次
     *
     * @param batchId 批次 ID
     * @param docIds 单据 ID 列表 (String)
     * @return 添加的条目数
     */
    int addDocsToBatch(Long batchId, List<String> docIds);

    /**
     * 从批次移除条目
     */
    void removeItemFromBatch(Long batchId, Long itemId);

    /**
     * 获取批次条目列表
     */
    List<ArchiveBatchItem> getBatchItems(Long batchId);

    /**
     * 按类型获取批次条目
     */
    List<ArchiveBatchItem> getBatchItemsByType(Long batchId, String itemType);

    // ========== 归档流程 ==========

    /**
     * 提交批次进行校验
     *
     * @param batchId 批次 ID
     * @param submittedBy 提交人 ID
     * @return 更新后的批次
     */
    ArchiveSubmitBatch submitBatch(Long batchId, Long submittedBy);

    /**
     * 执行批次校验
     *
     * @param batchId 批次 ID
     * @return 校验报告
     */
    Map<String, Object> validateBatch(Long batchId);

    /**
     * 审批通过
     *
     * @param batchId 批次 ID
     * @param approvedBy 审批人 ID
     * @param comment 审批意见
     * @return 更新后的批次
     */
    ArchiveSubmitBatch approveBatch(Long batchId, Long approvedBy, String comment);

    /**
     * 审批驳回
     *
     * @param batchId 批次 ID
     * @param rejectedBy 驳回人 ID
     * @param comment 驳回原因
     * @return 更新后的批次
     */
    ArchiveSubmitBatch rejectBatch(Long batchId, Long rejectedBy, String comment);

    /**
     * 执行归档
     *
     * @param batchId 批次 ID
     * @param archivedBy 归档执行人 ID
     * @return 更新后的批次
     */
    ArchiveSubmitBatch executeBatchArchive(Long batchId, Long archivedBy);

    // ========== 四性检测 ==========

    /**
     * 执行四性检测
     *
     * @param batchId 批次 ID
     * @return 检测报告
     */
    Map<String, Object> runIntegrityCheck(Long batchId);

    // ========== 统计 ==========

    /**
     * 获取批次统计
     */
    Map<String, Object> getBatchStats(String fondsId);
}