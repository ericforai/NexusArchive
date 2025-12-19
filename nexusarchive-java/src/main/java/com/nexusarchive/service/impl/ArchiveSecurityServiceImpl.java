package com.nexusarchive.service.impl;

import com.nexusarchive.entity.ArchiveBatch;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveBatchMapper;
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
    @Transactional
    public ArchiveBatch createSecurityBatch(String batchNo, List<ArcFileContent> items, String operatorId) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        log.info("Creating Security Batch for {} items, BatchNo: {}", items.size(), batchNo);

        // 1. 计算当前批次数据的聚合哈希 (简单起见使用级联哈希)
        // 实际生产中可使用 Merkle Tree 以支持局部验证
        String dataContent = items.stream()
                .map(item -> item.getFileHash() + "|" + item.getId())
                .collect(Collectors.joining(","));
        String currentBatchHash = sm3Utils.hash(dataContent);

        // 2. 获取上一批次的挂接哈希 (Chained Hash)
        String prevChainedHash = batchMapper.getLastChainedHash();
        if (prevChainedHash == null) {
            prevChainedHash = "0000000000000000000000000000000000000000000000000000000000000000"; // Genesis Block
        }

        // 3. 计算本批次的挂接哈希: SM3(prev + current)
        String chainedHash = sm3Utils.hash(prevChainedHash + currentBatchHash);

        // 4. 保存批次记录
        ArchiveBatch batch = ArchiveBatch.builder()
                .batchNo(batchNo)
                .prevBatchHash(prevChainedHash)
                .currentBatchHash(currentBatchHash)
                .chainedHash(chainedHash)
                .hashAlgo("SM3")
                .itemCount(items.size())
                .operatorId(operatorId)
                .build();

        batchMapper.insert(batch);

        log.info("Security Batch created successfully ID: {}, ChainedHash: {}", batch.getId(), chainedHash);
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

        // 2. 重新计算当前批次数据哈希
        String dataContent = files.stream()
                .map(item -> item.getFileHash() + "|" + item.getId())
                .collect(Collectors.joining(","));
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
        // 全量比对逻辑 (略)
        log.info("Full security chain inspection started...");
    }
}
