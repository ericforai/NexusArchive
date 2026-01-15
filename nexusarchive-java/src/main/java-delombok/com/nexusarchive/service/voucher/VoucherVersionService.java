// Input: MyBatis-Plus, Java 标准库
// Output: VoucherVersionService 类
// Pos: Service Layer

package com.nexusarchive.service.voucher;

import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 原始凭证版本服务
 *
 * 负责凭证版本管理
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VoucherVersionService {

    private final OriginalVoucherMapper voucherMapper;

    /**
     * 创建新版本 (原始凭证的版本控制)
     *
     * @param oldVersion 旧版本
     * @param updates 更新数据
     * @param reason 变更原因
     * @param userId 操作人ID
     * @return 新版本
     */
    @Transactional
    public OriginalVoucher createNewVersion(OriginalVoucher oldVersion, OriginalVoucher updates,
            String reason, String userId) {
        // 1. 标记旧版本为非最新
        oldVersion.setIsLatest(false);
        voucherMapper.updateById(oldVersion);

        // 2. 创建新版本
        OriginalVoucher newVersion = buildNewVersion(oldVersion, updates, reason, userId);
        voucherMapper.insert(newVersion);

        log.info("Created new version {} for voucher: {} by user: {}",
                newVersion.getVersion(), newVersion.getVoucherNo(), userId);

        return newVersion;
    }

    /**
     * 更新凭证字段
     */
    public void updateFields(OriginalVoucher existing, OriginalVoucher updates) {
        if (updates.getBusinessDate() != null)
            existing.setBusinessDate(updates.getBusinessDate());
        if (updates.getAmount() != null)
            existing.setAmount(updates.getAmount());
        if (updates.getCurrency() != null)
            existing.setCurrency(updates.getCurrency());
        if (updates.getCounterparty() != null)
            existing.setCounterparty(updates.getCounterparty());
        if (updates.getSummary() != null)
            existing.setSummary(updates.getSummary());
        if (updates.getAuditor() != null)
            existing.setAuditor(updates.getAuditor());
        if (updates.getBookkeeper() != null)
            existing.setBookkeeper(updates.getBookkeeper());
        if (updates.getApprover() != null)
            existing.setApprover(updates.getApprover());
    }

    /**
     * 构建新版本对象
     */
    private OriginalVoucher buildNewVersion(OriginalVoucher oldVersion, OriginalVoucher updates,
                                           String reason, String userId) {
        return OriginalVoucher.builder()
                .id(UUID.randomUUID().toString())
                .voucherNo(oldVersion.getVoucherNo())
                .voucherCategory(oldVersion.getVoucherCategory())
                .voucherType(oldVersion.getVoucherType())
                .businessDate(updates.getBusinessDate() != null ? updates.getBusinessDate() : oldVersion.getBusinessDate())
                .amount(updates.getAmount() != null ? updates.getAmount() : oldVersion.getAmount())
                .currency(updates.getCurrency() != null ? updates.getCurrency() : oldVersion.getCurrency())
                .counterparty(updates.getCounterparty() != null ? updates.getCounterparty() : oldVersion.getCounterparty())
                .summary(updates.getSummary() != null ? updates.getSummary() : oldVersion.getSummary())
                .creator(oldVersion.getCreator())
                .auditor(updates.getAuditor() != null ? updates.getAuditor() : oldVersion.getAuditor())
                .bookkeeper(updates.getBookkeeper() != null ? updates.getBookkeeper() : oldVersion.getBookkeeper())
                .approver(updates.getApprover() != null ? updates.getApprover() : oldVersion.getApprover())
                .sourceSystem(oldVersion.getSourceSystem())
                .sourceDocId(oldVersion.getSourceDocId())
                .fondsCode(oldVersion.getFondsCode())
                .fiscalYear(oldVersion.getFiscalYear())
                .retentionPeriod(oldVersion.getRetentionPeriod())
                .archiveStatus("DRAFT")
                .version(oldVersion.getVersion() + 1)
                .parentVersionId(oldVersion.getId())
                .versionReason(reason)
                .isLatest(true)
                .createdBy(userId)
                .createdTime(LocalDateTime.now())
                .build();
    }
}
