package com.nexusarchive.service.strategy.impl;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchivalCodeSequenceMapper;
import com.nexusarchive.service.strategy.ArchivalCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 标准档号生成器实现
 * 格式: [全宗号]-[年度]-[保管期限]-[机构/问题]-[类别号]-[件号]
 * 例: Z001-2023-10Y-FIN-AC01-000001
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivalCodeGeneratorImpl implements ArchivalCodeGenerator {

    private final ArchivalCodeSequenceMapper sequenceMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 独立事务，防止长事务导致锁竞争过久，也防止回滚导致空号(视策略而定，为了连续性通常允许空号或独立提交)
    // 这里选择 REQUIRES_NEW 是为了保证 sequence 即使主业务失败也递增，避免"重号"风险优于"断号"风险，或者严格连续需要串行。
    // 档案局通常要求尽量连续，但 "断号" 比 "重号" 后果轻。
    public String generateNextCode(Archive archive) {
        // 1. 校验必要参数
        validateArchiveParams(archive);

        String fondsCode = archive.getFondsNo();
        String year = archive.getFiscalYear();
        String category = archive.getCategoryCode();
        // 如果机构代码为空，默认使用 'ORG' 或从 departmentId转换，这里假设 archive 中已有逻辑处理好，或者在此统一
        String orgPart = "ORG"; 
        String retention = archive.getRetentionPeriod(); // 10Y, 30Y, PERMANENT

        // 2. 获取并递增序列号 (悲观锁)
        // 2.1 尝试初始化
        sequenceMapper.initSequence(fondsCode, year, category);
        
        // 2.2 递增并获取
        int rows = sequenceMapper.incrementVal(fondsCode, year, category);
        if (rows == 0) {
            // 理论上不会发生，除非 init 失败
            throw new BusinessException("Failed to generate archival code sequence");
        }
        
        // 2.3 获取当前值 (在同一事务中，安全)
        Integer seq = sequenceMapper.selectCurrentValForUpdate(fondsCode, year, category);

        // 3. 组装档号
        // Format: [Fonds]-[Year]-[Retention]-[Org]-[Category]-[Seq]
        String seqStr = String.format("%06d", seq);
        
        return String.format("%s-%s-%s-%s-%s-%s", 
                fondsCode, year, retention, orgPart, category, seqStr);
    }

    private void validateArchiveParams(Archive archive) {
        if (archive.getFondsNo() == null) throw new BusinessException("Missing Fonds Code for archival");
        if (archive.getFiscalYear() == null) throw new BusinessException("Missing Fiscal Year for archival");
        if (archive.getCategoryCode() == null) throw new BusinessException("Missing Category Code for archival");
        if (archive.getRetentionPeriod() == null) throw new BusinessException("Missing Retention Period for archival");
    }
}
