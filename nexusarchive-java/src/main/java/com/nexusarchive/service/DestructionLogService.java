// Input: Archive Entity, Destruction Entity
// Output: DestructionLogService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;

import java.time.LocalDate;

/**
 * 销毁清册服务
 * 
 * 功能：
 * 1. 写入销毁清册记录（包含完整元数据快照）
 * 2. 导出销毁清册（Excel/PDF）
 * 3. 计算清册哈希链
 * 
 * PRD 要求：
 * - 销毁清册永久只读，禁止修改/删除
 * - 支持哈希链验真
 */
public interface DestructionLogService {
    
    /**
     * 写入销毁清册记录
     * 
     * @param archive 档案对象
     * @param destructionId 销毁申请ID
     * @param executorId 执行人ID
     * @param traceId 追踪ID
     */
    void logDestruction(Archive archive, String destructionId, String executorId, String traceId);
    
    /**
     * 导出销毁清册
     * 
     * @param fondsNo 全宗号
     * @param archiveYear 归档年度
     * @param fromDate 开始日期
     * @param toDate 结束日期
     * @return 文件字节数组（Excel/PDF）
     */
    byte[] exportDestructionLog(String fondsNo, Integer archiveYear, 
                                LocalDate fromDate, LocalDate toDate);
    
    /**
     * 计算清册哈希链
     * 
     * @param prevHash 前一条记录的哈希值
     * @param logData 当前清册记录数据
     * @return 当前记录的哈希值
     */
    String calculateHashChain(String prevHash, Object logData);
}

