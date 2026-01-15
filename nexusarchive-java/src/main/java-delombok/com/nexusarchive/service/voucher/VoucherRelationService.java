// Input: MyBatis-Plus, Spring Framework, Lombok
// Output: VoucherRelationService 类
// Pos: 服务层 - 原始凭证关联服务

package com.nexusarchive.service.voucher;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 原始凭证关联服务
 * <p>
 * 负责管理原始凭证与记账凭证之间的关联关系
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherRelationService {

    private final OriginalVoucherMapper voucherMapper;
    private final VoucherRelationMapper relationMapper;

    /**
     * 建立原始凭证与记账凭证的关联
     */
    @Transactional
    public VoucherRelation createRelation(String originalVoucherId, String accountingVoucherId,
            String desc, String userId) {
        // 校验原始凭证存在
        OriginalVoucher voucher = voucherMapper.selectById(originalVoucherId);
        if (voucher == null || voucher.getDeleted() == 1) {
            throw new BusinessException("原始凭证不存在: " + originalVoucherId);
        }

        // 检查关联是否已存在
        if (relationMapper.countRelation(originalVoucherId, accountingVoucherId) > 0) {
            throw new BusinessException("关联关系已存在");
        }

        VoucherRelation relation = VoucherRelation.builder()
                .id(UUID.randomUUID().toString())
                .originalVoucherId(originalVoucherId)
                .accountingVoucherId(accountingVoucherId)
                .relationType("ORIGINAL_TO_ACCOUNTING")
                .relationDesc(desc)
                .createdBy(userId)
                .createdTime(LocalDateTime.now())
                .build();

        relationMapper.insert(relation);
        log.info("Created relation: {} -> {}", originalVoucherId, accountingVoucherId);
        return relation;
    }

    /**
     * 删除关联关系
     */
    @Transactional
    public void deleteRelation(String relationId) {
        VoucherRelation relation = relationMapper.selectById(relationId);
        if (relation != null) {
            relation.setDeleted(1);
            relationMapper.updateById(relation);
            log.info("Deleted relation: {}", relationId);
        }
    }

    /**
     * 获取原始凭证的关联列表
     */
    public List<VoucherRelation> getRelationsByOriginalVoucher(String voucherId) {
        return relationMapper.findByOriginalVoucherId(voucherId);
    }
}
