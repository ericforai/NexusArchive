// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: ArchivalCodeGeneratorImpl 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.strategy.impl;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
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

    /**
     * 生成下一个档号
     * <p>
     * <b>事务传播策略说明：</b>
     * 使用 REQUIRES_NEW 创建独立事务，原因如下：
     * <ol>
     * <li><b>序列号递增的不可逆性：</b>档号序列一旦递增就应该提交，即使主业务失败也不回滚，
     *    这样可以避免"重号"（duplicate numbers）风险。</li>
     * <li><b>避免长事务锁定：</b>档号生成使用悲观锁（SELECT FOR UPDATE），如果放在主事务中，
     *    会导致长事务持有序列锁，影响并发性能。</li>
     * <li><b>允许断号优于重号：</b>根据《会计档案管理办法》，"断号"（sequence gaps）后果轻于"重号"。
     *    如果业务需要严格连续，应该使用串行生成策略而非事务回滚。</li>
     * <li><b>独立性保证：</b>档号生成是基础服务，其结果不应受上层业务逻辑影响。</li>
     * </ol>
     *
     * <b>业务权衡：</b>
     * <ul>
     * <li>优点：避免重号、减少锁竞争、提高并发性</li>
     * <li>缺点：可能产生断号（当主业务回滚时）</li>
     * </ul>
     * </p>
     *
     * @param archive 档案实体（包含全宗号、年度、分类等信息）
     * @return 生成的档号，格式：[全宗号]-[年度]-[保管期限]-[机构]-[分类]-[件号]
     */
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
            throw new BusinessException(ErrorCode.ARCHIVAL_CODE_GENERATION_FAILED);
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
        if (archive.getFondsNo() == null) throw new BusinessException(ErrorCode.MISSING_FONDS_CODE);
        if (archive.getFiscalYear() == null) throw new BusinessException(ErrorCode.MISSING_FISCAL_YEAR);
        if (archive.getCategoryCode() == null) throw new BusinessException(ErrorCode.MISSING_CATEGORY_CODE);
        if (archive.getRetentionPeriod() == null) throw new BusinessException(ErrorCode.MISSING_RETENTION_PERIOD);
    }
}
