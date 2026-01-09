// Input: Spring Framework、Lombok、Java 标准库、本地模块
// Output: AsyncFourNatureCheckService 接口
// Pos: 业务服务层 - 异步四性检测服务

package com.nexusarchive.service.compliance;

import com.nexusarchive.dto.compliance.AsyncCheckTaskStatus;
import com.nexusarchive.dto.sip.report.FourNatureReport;

import java.util.concurrent.CompletableFuture;

/**
 * 异步四性检测服务接口
 * <p>
 * 提供异步四性检测能力，支持并行执行真实性、完整性、可用性、安全性检测
 * </p>
 *
 * @author System
 */
public interface AsyncFourNatureCheckService {

    /**
     * 异步执行四性检测
     * <p>
     * 该方法立即返回一个任务ID，实际的检测操作在后台异步执行。
     * 四性检测并行执行，以提升整体性能。
     * </p>
     *
     * @param archiveId   档案ID
     * @param archiveCode 档案编码
     * @return CompletableFuture 包含任务ID
     */
    CompletableFuture<String> submitCheckTask(String archiveId, String archiveCode);

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    AsyncCheckTaskStatus getTaskStatus(String taskId);

    /**
     * 获取检测结果
     * <p>
     * 如果任务尚未完成，该方法会阻塞等待结果。
     * 建议先通过 getTaskStatus 检查任务状态。
     * </p>
     *
     * @param taskId 任务ID
     * @return 检测结果
     */
    FourNatureReport getCheckResult(String taskId);

    /**
     * 异步获取检测结果（非阻塞）
     *
     * @param taskId 任务ID
     * @return CompletableFuture 包含检测结果，如果任务不存在或失败则返回 null
     */
    CompletableFuture<FourNatureReport> getCheckResultAsync(String taskId);

    /**
     * 取消检测任务
     *
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    boolean cancelTask(String taskId);

    /**
     * 并行执行四性检测（内部方法）
     * <p>
     * 同时启动真实性、完整性、可用性、安全性检测，等待所有检测完成后返回汇总结果
     * </p>
     *
     * @param taskId  任务ID
     * @param archiveId 档案ID
     * @return 检测结果
     */
    CompletableFuture<FourNatureReport> performParallelCheck(String taskId, String archiveId);
}
