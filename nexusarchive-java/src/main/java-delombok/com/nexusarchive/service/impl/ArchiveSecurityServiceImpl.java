// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: ArchiveSecurityServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.entity.ArchiveBatch;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveBatchMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.service.ArchiveSecurityService;
import com.nexusarchive.util.SM3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveSecurityServiceImpl implements ArchiveSecurityService {

    private final ArchiveBatchMapper batchMapper;
    private final ArcFileContentMapper fileContentMapper; // 注入Mapper (High #5 Fix)
    private final SM3Utils sm3Utils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveBatch createSecurityBatch(String batchNo, List<ArcFileContent> items, String operatorId) {
        // 1. 批次号唯一性校验 (防重入)
        if (batchMapper.selectCount(new LambdaQueryWrapper<ArchiveBatch>()
                .eq(ArchiveBatch::getBatchNo, batchNo)) > 0) {
            throw new IllegalArgumentException("Batch number already exists: " + batchNo);
        }
        
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Archive items cannot be empty");
        }

        if (operatorId == null) {
            operatorId = "SYSTEM";
        }

        log.info("Creating Security Batch for {} items, BatchNo: {}", items.size(), batchNo);

        // [FIX P0-CRIT 1] 必须先确保所有 items 已持久化并获得 ID，否则后续计算 currentBatchHash 时 getId() 为空
        for (ArcFileContent item : items) {
            if (item.getId() == null) {
                fileContentMapper.insert(item);
            }
        }

        // [FIX P0-CRIT 2] 使用悲观锁锁定哈希链尾部，防止并发下的 prevChainedHash 读取脏数据导致链条断裂
        String prevChainedHash = batchMapper.getLastChainedHashForUpdate();
        if (prevChainedHash == null) {
            // 创世批次首哈希
            prevChainedHash = "0000000000000000000000000000000000000000000000000000000000000000";
        }

        // 获取下一个序列号 (仅用于审计追溯顺序)
        Long batchSeq = batchMapper.getNextBatchSequence();

        // 2. 计算当前批次数据的聚合哈希 (使用已生成的 ID)
        String dataContent = items.stream()
                .sorted(java.util.Comparator.comparing(ArcFileContent::getSequenceInBatch))
                .map(item -> item.getFileHash() + "||" + item.getId())
                .collect(Collectors.joining(";;"));
        String currentBatchHash = sm3Utils.hash(dataContent);

        // 3. 计算本批次的挂接哈希: SM3(prev + current)
        String chainedHash = sm3Utils.hash(prevChainedHash + currentBatchHash);

        // 4. 保存批次记录 (ChainedHash 成为下一条的 prev)
        ArchiveBatch batch = ArchiveBatch.builder()
                .batchNo(batchNo)
                .batchSequence(batchSeq)
                .prevBatchHash(prevChainedHash)
                .currentBatchHash(currentBatchHash)
                .chainedHash(chainedHash)
                .hashAlgo("SM3")
                .itemCount(items.size())
                .operatorId(operatorId)
                .build();
        batchMapper.insert(batch);

        // 5. 自动关联文件与批次 ID
        for (ArcFileContent item : items) {
            item.setBatchId(batch.getId());
            fileContentMapper.updateById(item);
        }

        log.info("Security Batch created successfully ID: {}, Seq: {}, ChainedHash: {}", 
                batch.getId(), batchSeq, chainedHash);
        return batch;
    }

    @Override
    public boolean verifyBatchIntegrity(Long batchId) {
        ArchiveBatch batch = batchMapper.selectById(batchId);
        if (batch == null)
            return false;

        // 1. 获取关联文件的真实数据 (按顺序)
        List<ArcFileContent> files = fileContentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                        .eq(ArcFileContent::getBatchId, batchId)
                        .orderByAsc(ArcFileContent::getSequenceInBatch));
        // 防御性检查：若文件列表为空直接返回 false
        if (files == null) {
            log.warn("Batch {} 对应文件列表为空", batchId);
            return false;
        }

        // 2. 重新计算当前批次数据哈希
        // [FIXED] 使用与创建时完全一致的序列化格式: hash + "||" + id, 分隔符 ";;"
        String dataContent = files.stream()
                .map(item -> item.getFileHash() + "||" + item.getId())
                .collect(Collectors.joining(";;"));
        String recalculatedHash = sm3Utils.hash(dataContent);

        // 3. 验证当前批次哈希是否被篡改
        if (!recalculatedHash.equals(batch.getCurrentBatchHash())) {
            log.warn("Batch {} integrity check failed: Data hash mismatch! DB={}, Calc={}",
                    batchId, batch.getCurrentBatchHash(), recalculatedHash);
            return false;
        }

        // 4. 验证哈希链是否断裂
        String expectedChainHash = sm3Utils.hash(batch.getPrevBatchHash() + recalculatedHash);
        return expectedChainHash.equals(batch.getChainedHash());
    }

    @Override
    public void inspectFullChain() {
        log.info("Full security chain inspection started...");
        int pageSize = 100;
        long lastProcessedSeq = -1;
        String lastChainedHash = null;
        boolean allPassing = true;

        while (true) {
            List<ArchiveBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<ArchiveBatch>()
                    .gt(ArchiveBatch::getBatchSequence, lastProcessedSeq)
                    .orderByAsc(ArchiveBatch::getBatchSequence)
                    .last("LIMIT " + pageSize));

            if (batches == null || batches.isEmpty()) {
                break;
            }

            for (ArchiveBatch batch : batches) {
                // 1. 验证本条记录的哈希链挂接是否正确 (上一个记录的 chainedHash 与当前的 prevBatchHash)
                if (lastChainedHash != null && !lastChainedHash.equals(batch.getPrevBatchHash())) {
                    log.error("HASH CHAIN BROKEN! Sequence: {}, Expected Prev: {}, Actual Prev: {}",
                            batch.getBatchSequence(), lastChainedHash, batch.getPrevBatchHash());
                    allPassing = false;
                }

                // 2. 验证数据物理完整性 (调用已有的 verifyBatchIntegrity)
                if (!verifyBatchIntegrity(batch.getId())) {
                    log.error("DATA TAMPERED! Sequence: {}, BatchID: {}", batch.getBatchSequence(), batch.getId());
                    allPassing = false;
                }

                lastProcessedSeq = batch.getBatchSequence();
                lastChainedHash = batch.getChainedHash();
            }
        }

        if (allPassing) {
            log.info("Full security chain inspection COMPLETED. Verdict: PROTECTED.");
        } else {
            log.warn("Full security chain inspection COMPLETED. Verdict: VULNERABLE / TAMPERED.");
        }
    }
}
