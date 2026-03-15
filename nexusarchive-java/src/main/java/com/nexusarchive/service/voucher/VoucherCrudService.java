// Input: MyBatis-Plus, Spring Framework, Lombok
// Output: VoucherCrudService 类
// Pos: 服务层 - 原始凭证 CRUD 服务

package com.nexusarchive.service.voucher;

import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherType;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.OriginalVoucherTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

/**
 * 原始凭证 CRUD 服务
 * <p>
 * 负责原始凭证的创建、更新、删除操作：
 * - 创建凭证
 * - 更新凭证
 * - 创建新版本
 * - 删除凭证
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherCrudService {

    private final OriginalVoucherMapper voucherMapper;
    private final OriginalVoucherTypeMapper typeMapper;

    /**
     * 创建原始凭证
     */
    @Transactional
    public OriginalVoucher create(OriginalVoucher voucher, String userId) {
        // 1. 校验类型
        validateVoucherType(voucher.getVoucherCategory(), voucher.getVoucherType());

        // 2. 生成凭证编号
        String voucherNo = generateVoucherNo(voucher.getFondsCode(),
                voucher.getFiscalYear(), voucher.getVoucherCategory());
        voucher.setVoucherNo(voucherNo);

        // 3. 设置默认值
        if (voucher.getId() == null) {
            voucher.setId(UUID.randomUUID().toString());
        }
        if (voucher.getBusinessDate() == null) {
            voucher.setBusinessDate(java.time.LocalDate.now());
        }
        voucher.setVersion(1);
        voucher.setIsLatest(true);
        voucher.setArchiveStatus("DRAFT");
        voucher.setCreatedBy(userId);
        voucher.setCreatedTime(LocalDateTime.now());

        // 4. 根据类型设置默认保管期限
        if (!StringUtils.hasText(voucher.getRetentionPeriod())) {
            OriginalVoucherType typeInfo = typeMapper.findByTypeCode(voucher.getVoucherType());
            if (typeInfo != null) {
                voucher.setRetentionPeriod(typeInfo.getDefaultRetention());
            } else {
                voucher.setRetentionPeriod("30Y");
            }
        }

        voucherMapper.insert(voucher);
        log.info("Created original voucher: {} by user: {}", voucherNo, userId);
        return voucher;
    }

    /**
     * 更新原始凭证 (创建新版本)
     */
    @Transactional
    public OriginalVoucher update(String id, OriginalVoucher updates, String reason, String userId) {
        OriginalVoucher existing = voucherMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException("原始凭证不存在: " + id);
        }

        // 已归档的凭证不允许直接修改，需要创建新版本
        if ("ARCHIVED".equals(existing.getArchiveStatus())) {
            return createNewVersion(existing, updates, reason, userId);
        }

        // 草稿/待归档状态可以直接修改
        updateFields(existing, updates);
        existing.setLastModifiedBy(userId);
        existing.setLastModifiedTime(LocalDateTime.now());
        voucherMapper.updateById(existing);

        log.info("Updated original voucher: {} by user: {}", existing.getVoucherNo(), userId);
        return existing;
    }

    /**
     * 创建新版本 (原始凭证的版本控制)
     */
    @Transactional
    public OriginalVoucher createNewVersion(OriginalVoucher oldVersion, OriginalVoucher updates,
            String reason, String userId) {
        // 1. 标记旧版本为非最新
        oldVersion.setIsLatest(false);
        voucherMapper.updateById(oldVersion);

        // 2. 创建新版本
        OriginalVoucher newVersion = OriginalVoucher.builder()
                .id(UUID.randomUUID().toString())
                .voucherNo(oldVersion.getVoucherNo())
                .voucherCategory(oldVersion.getVoucherCategory())
                .voucherType(oldVersion.getVoucherType())
                .businessDate(
                        updates.getBusinessDate() != null ? updates.getBusinessDate() : oldVersion.getBusinessDate())
                .amount(updates.getAmount() != null ? updates.getAmount() : oldVersion.getAmount())
                .currency(updates.getCurrency() != null ? updates.getCurrency() : oldVersion.getCurrency())
                .counterparty(
                        updates.getCounterparty() != null ? updates.getCounterparty() : oldVersion.getCounterparty())
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

        voucherMapper.insert(newVersion);
        log.info("Created new version {} for voucher: {} by user: {}",
                newVersion.getVersion(), newVersion.getVoucherNo(), userId);

        return newVersion;
    }

    /**
     * 逻辑删除原始凭证
     */
    @Transactional
    public void delete(String id, String userId) {
        OriginalVoucher voucher = voucherMapper.selectById(id);
        if (voucher == null || voucher.getDeleted() == 1) {
            throw new BusinessException("原始凭证不存在: " + id);
        }

        // 已归档的凭证不允许删除
        if ("ARCHIVED".equals(voucher.getArchiveStatus())) {
            throw new BusinessException("已归档的原始凭证不允许删除");
        }

        voucher.setDeleted(1);
        voucher.setLastModifiedBy(userId);
        voucher.setLastModifiedTime(LocalDateTime.now());
        voucherMapper.updateById(voucher);

        log.info("Deleted original voucher: {} by user: {}", voucher.getVoucherNo(), userId);
    }

    /**
     * 提交归档
     */
    @Transactional
    public void submitForArchive(String id, String userId) {
        OriginalVoucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new BusinessException("原始凭证不存在: " + id);
        }
        if (!"DRAFT".equals(voucher.getArchiveStatus())) {
            throw new BusinessException("只有草稿状态的凭证可以提交归档");
        }

        voucher.setArchiveStatus(OperationResult.PENDING);
        voucher.setLastModifiedBy(userId);
        voucher.setLastModifiedTime(LocalDateTime.now());
        voucherMapper.updateById(voucher);

        log.info("Submitted voucher for archive: {} by user: {}", voucher.getVoucherNo(), userId);
    }

    /**
     * 确认归档
     */
    @Transactional
    public void confirmArchive(String id, String userId) {
        OriginalVoucher voucher = voucherMapper.selectById(id);
        if (voucher == null) {
            throw new BusinessException("原始凭证不存在: " + id);
        }
        if (!OperationResult.PENDING.equals(voucher.getArchiveStatus())) {
            throw new BusinessException("只有待归档状态的凭证可以确认归档");
        }

        voucher.setArchiveStatus("ARCHIVED");
        voucher.setArchivedTime(LocalDateTime.now());
        voucher.setLastModifiedBy(userId);
        voucher.setLastModifiedTime(LocalDateTime.now());
        voucherMapper.updateById(voucher);

        log.info("Confirmed archive for voucher: {} by user: {}", voucher.getVoucherNo(), userId);
    }

    /**
     * 生成原始凭证编号
     * 格式: OV-{年度}-{类型简码}-{6位序号}
     */
    private synchronized String generateVoucherNo(String fondsCode, String fiscalYear, String category) {
        if (!StringUtils.hasText(fiscalYear)) {
            fiscalYear = String.valueOf(Year.now().getValue());
        }

        // 获取并更新序号
        Long seq = voucherMapper.getNextSequence(fondsCode, fiscalYear, category);
        voucherMapper.updateSequence(
                UUID.randomUUID().toString(),
                fondsCode, fiscalYear, category, seq);

        // 类型简码映射
        String typeCode = getCategoryShortCode(category);

        return String.format("OV-%s-%s-%06d", fiscalYear, typeCode, seq);
    }

    private String getCategoryShortCode(String category) {
        return switch (category) {
            case "INVOICE" -> "INV";
            case "BANK" -> "BNK";
            case "DOCUMENT" -> "DOC";
            case "CONTRACT" -> "CON";
            default -> "OTH";
        };
    }

    private void updateFields(OriginalVoucher existing, OriginalVoucher updates) {
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

    private void validateVoucherType(String category, String type) {
        OriginalVoucherType typeInfo = typeMapper.findByTypeCode(type);
        if (typeInfo == null || !typeInfo.getEnabled()) {
            throw new BusinessException("无效的凭证类型: " + type);
        }
        if (!typeInfo.getCategoryCode().equals(category)) {
            throw new BusinessException("凭证类型与类别不匹配: " + category + " / " + type);
        }
    }
}
