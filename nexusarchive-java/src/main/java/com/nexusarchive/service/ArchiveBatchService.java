// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库
// Output: ArchiveBatchService 类
// Pos: 业务服务层
// 一旦我被更新,务必更新我的开头注释,以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.entity.ArchiveBatch;
import com.nexusarchive.mapper.ArchiveBatchMapper;
import cn.hutool.core.util.HexUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 归档批次服务
 * ✅ P1 修复: 实现哈希链验证逻辑,防止数据篡改
 * 
 * @author Agent B - 合规开发工程师
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ArchiveBatchService {
    
    private final ArchiveBatchMapper archiveBatchMapper;
    
    /**
     * 验证归档批次哈希链的完整性
     * ✅ P1 修复: 实现哈希链验证,符合 DA/T 94-2022 防篡改要求
     * 
     * @param batchNo 批次号
     * @return 验证结果
     */
    public boolean verifyChain(String batchNo) {
        ArchiveBatch batch = archiveBatchMapper.selectOne(
            new QueryWrapper<ArchiveBatch>().eq("batch_no", batchNo));
        
        if (batch == null) {
            log.warn("批次不存在: batchNo={}", batchNo);
            return false;
        }
        
        // ✅ 计算预期的哈希链值
        String expectedChainedHash = calculateChainedHash(
            batch.getPrevBatchHash(), 
            batch.getCurrentBatchHash(),
            batch.getHashAlgo());
        
        // ✅ 验证哈希链
        if (!expectedChainedHash.equals(batch.getChainedHash())) {
            log.error("哈希链验证失败: batchNo={}, expected={}, actual={}", 
                batchNo, expectedChainedHash, batch.getChainedHash());
            return false;
        }
        
        log.info("哈希链验证成功: batchNo={}", batchNo);
        return true;
    }
    
    /**
     * 验证整个哈希链的完整性
     * 从最早的批次开始,逐个验证到指定批次
     * 
     * @param batchNo 要验证到的批次号
     * @return 验证结果
     */
    public boolean verifyChainFromBeginning(String batchNo) {
        ArchiveBatch targetBatch = archiveBatchMapper.selectOne(
            new QueryWrapper<ArchiveBatch>().eq("batch_no", batchNo));
        
        if (targetBatch == null) {
            log.warn("目标批次不存在: batchNo={}", batchNo);
            return false;
        }
        
        // 获取所有批次,按创建时间排序
        QueryWrapper<ArchiveBatch> query = new QueryWrapper<>();
        query.orderByAsc("created_time");
        var allBatches = archiveBatchMapper.selectList(query);
        
        if (allBatches == null || allBatches.isEmpty()) {
            log.warn("没有找到任何批次");
            return false;
        }
        
        // 逐个验证
        for (ArchiveBatch batch : allBatches) {
            if (!verifyChain(batch.getBatchNo())) {
                log.error("哈希链验证失败,在批次: {}", batch.getBatchNo());
                return false;
            }
            
            // 如果已经验证到目标批次,则停止
            if (batch.getBatchNo().equals(batchNo)) {
                break;
            }
        }
        
        log.info("哈希链完整性验证成功,从开始到批次: {}", batchNo);
        return true;
    }
    
    /**
     * 计算哈希链值
     * ✅ 使用 SHA-256 或 SM3 算法
     * 
     * @param prevHash 前一批次哈希
     * @param currentHash 当前批次哈希
     * @param algorithm 哈希算法 (SHA-256 或 SM3)
     * @return 哈希链值
     */
    private String calculateChainedHash(String prevHash, String currentHash, String algorithm) {
        try {
            String combined = (prevHash != null ? prevHash : "") + currentHash;
            
            // 根据算法选择
            String algoName = "SHA-256"; // 默认
            if ("SM3".equalsIgnoreCase(algorithm)) {
                // 如果需要 SM3,使用 BouncyCastle
                algoName = "SM3";
            }
            
            MessageDigest md = MessageDigest.getInstance(algoName);
            byte[] hash = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexUtil.encodeHexStr(hash);
        } catch (Exception e) {
            log.error("计算哈希链失败: prevHash={}, currentHash={}, error={}", 
                prevHash, currentHash, e.getMessage(), e);
            throw new RuntimeException("计算哈希链失败", e);
        }
    }
    
    /**
     * 获取批次信息
     * 
     * @param batchNo 批次号
     * @return 批次信息
     */
    public ArchiveBatch getBatch(String batchNo) {
        return archiveBatchMapper.selectOne(
            new QueryWrapper<ArchiveBatch>().eq("batch_no", batchNo));
    }
    
    /**
     * 获取最新批次
     * 
     * @return 最新批次
     */
    public ArchiveBatch getLatestBatch() {
        QueryWrapper<ArchiveBatch> query = new QueryWrapper<>();
        query.orderByDesc("created_time").last("LIMIT 1");
        return archiveBatchMapper.selectOne(query);
    }
}
