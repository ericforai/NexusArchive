// Input: MyBatis-Plus、Spring Framework、Java 标准库
// Output: OriginalVoucherService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.entity.OriginalVoucherType;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.mapper.OriginalVoucherTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

/**
 * 原始凭证服务
 * <p>
 * 管理原始凭证的生命周期：创建、检索、更新、删除
 * 包含编号生成、版本控制、关联管理
 * </p>
 * Reference: DA/T 94-2022, GB/T 39362-2020
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OriginalVoucherService {

    private final OriginalVoucherMapper voucherMapper;
    private final OriginalVoucherFileMapper fileMapper;
    private final VoucherRelationMapper relationMapper;
    private final OriginalVoucherTypeMapper typeMapper;

    // ===== 查询接口 =====

    /**
     * 分页查询原始凭证
     */
    public Page<OriginalVoucher> getVouchers(int page, int limit, String search,
            String category, String type, String status, String fondsCode, String fiscalYear) {
        Page<OriginalVoucher> pageObj = new Page<>(page, limit);
        LambdaQueryWrapper<OriginalVoucher> wrapper = new LambdaQueryWrapper<>();

        // 只查询最新版本
        wrapper.eq(OriginalVoucher::getIsLatest, true);

        // 条件过滤
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w
                    .like(OriginalVoucher::getVoucherNo, search)
                    .or().like(OriginalVoucher::getCounterparty, search)
                    .or().like(OriginalVoucher::getSummary, search));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(OriginalVoucher::getVoucherCategory, category);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(OriginalVoucher::getVoucherType, type);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(OriginalVoucher::getArchiveStatus, status);
        }
        if (StringUtils.hasText(fondsCode)) {
            wrapper.eq(OriginalVoucher::getFondsCode, fondsCode);
        }
        if (StringUtils.hasText(fiscalYear)) {
            wrapper.eq(OriginalVoucher::getFiscalYear, fiscalYear);
        }

        wrapper.orderByDesc(OriginalVoucher::getCreatedTime);
        return voucherMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 根据ID获取原始凭证详情
     */
    public OriginalVoucher getById(String id) {
        OriginalVoucher voucher = voucherMapper.selectById(id);
        if (voucher == null || voucher.getDeleted() == 1) {
            throw new BusinessException("原始凭证不存在: " + id);
        }
        return voucher;
    }

    /**
     * 获取原始凭证关联的文件列表
     */
    public List<OriginalVoucherFile> getFiles(String voucherId) {
        return fileMapper.findByVoucherId(voucherId);
    }

    /**
     * 获取版本历史
     */
    public List<OriginalVoucher> getVersionHistory(String id) {
        return voucherMapper.findVersionHistory(id);
    }

    /**
     * 获取关联的记账凭证
     */
    public List<VoucherRelation> getAccountingRelations(String voucherId) {
        return relationMapper.findByOriginalVoucherId(voucherId);
    }

    // ===== 创建接口 =====

    /**
     * 创建原始凭证
     * 
     * @param voucher 原始凭证数据
     * @param userId  创建人ID
     * @return 创建后的原始凭证
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

    // ===== 更新接口 =====

    /**
     * 更新原始凭证 (创建新版本)
     * 
     * @param id      原始凭证ID
     * @param updates 更新数据
     * @param reason  变更原因
     * @param userId  操作人ID
     * @return 新版本
     */
    @Transactional
    public OriginalVoucher update(String id, OriginalVoucher updates, String reason, String userId) {
        OriginalVoucher existing = getById(id);

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

    // ===== 删除接口 =====

    /**
     * 逻辑删除原始凭证
     */
    @Transactional
    public void delete(String id, String userId) {
        OriginalVoucher voucher = getById(id);

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

    // ===== 文件管理 =====

    /**
     * 添加文件到原始凭证
     */
    @Transactional
    public OriginalVoucherFile addFile(String voucherId, OriginalVoucherFile file, String userId) {
        // 校验凭证存在
        getById(voucherId);

        file.setId(UUID.randomUUID().toString());
        file.setVoucherId(voucherId);
        file.setCreatedBy(userId);
        file.setCreatedTime(LocalDateTime.now());

        // 设置序号
        List<OriginalVoucherFile> existingFiles = getFiles(voucherId);
        file.setSequenceNo(existingFiles.size() + 1);

        fileMapper.insert(file);
        log.info("Added file {} to voucher: {}", file.getFileName(), voucherId);
        return file;
    }

    // ===== 关联管理 =====

    /**
     * 建立原始凭证与记账凭证的关联
     */
    @Transactional
    public VoucherRelation createRelation(String originalVoucherId, String accountingVoucherId,
            String desc, String userId) {
        // 校验原始凭证存在
        getById(originalVoucherId);

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

    // ===== 类型管理 =====

    /**
     * 获取所有启用的凭证类型
     */
    public List<OriginalVoucherType> getAllTypes() {
        return typeMapper.findAllEnabled();
    }

    /**
     * 按类别获取凭证类型
     */
    public List<OriginalVoucherType> getTypesByCategory(String categoryCode) {
        return typeMapper.findByCategory(categoryCode);
    }

    /**
     * 校验凭证类型有效性
     */
    private void validateVoucherType(String category, String type) {
        OriginalVoucherType typeInfo = typeMapper.findByTypeCode(type);
        if (typeInfo == null || !typeInfo.getEnabled()) {
            throw new BusinessException("无效的凭证类型: " + type);
        }
        if (!typeInfo.getCategoryCode().equals(category)) {
            throw new BusinessException("凭证类型与类别不匹配: " + category + " / " + type);
        }
    }

    // ===== 归档状态管理 =====

    /**
     * 提交归档
     */
    @Transactional
    public void submitForArchive(String id, String userId) {
        OriginalVoucher voucher = getById(id);
        if (!"DRAFT".equals(voucher.getArchiveStatus())) {
            throw new BusinessException("只有草稿状态的凭证可以提交归档");
        }

        // 校验必填项
        validateForArchive(voucher);

        voucher.setArchiveStatus("PENDING");
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
        OriginalVoucher voucher = getById(id);
        if (!"PENDING".equals(voucher.getArchiveStatus())) {
            throw new BusinessException("只有待归档状态的凭证可以确认归档");
        }

        voucher.setArchiveStatus("ARCHIVED");
        voucher.setArchivedTime(LocalDateTime.now());
        voucher.setLastModifiedBy(userId);
        voucher.setLastModifiedTime(LocalDateTime.now());
        voucherMapper.updateById(voucher);

        log.info("Confirmed archive for voucher: {} by user: {}", voucher.getVoucherNo(), userId);
    }

    private void validateForArchive(OriginalVoucher voucher) {
        if (!StringUtils.hasText(voucher.getBusinessDate().toString())) {
            throw new BusinessException("业务日期不能为空");
        }
        if (!StringUtils.hasText(voucher.getFondsCode())) {
            throw new BusinessException("全宗号不能为空");
        }
        // 检查是否至少有一个文件
        List<OriginalVoucherFile> files = getFiles(voucher.getId());
        if (files.isEmpty()) {
            throw new BusinessException("原始凭证必须至少包含一个文件");
        }
    }

    // ===== 统计接口 =====

    /**
     * 获取统计数据
     */
    public OriginalVoucherStats getStats(String fondsCode, String fiscalYear) {
        LambdaQueryWrapper<OriginalVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OriginalVoucher::getIsLatest, true);
        if (StringUtils.hasText(fondsCode)) {
            wrapper.eq(OriginalVoucher::getFondsCode, fondsCode);
        }
        if (StringUtils.hasText(fiscalYear)) {
            wrapper.eq(OriginalVoucher::getFiscalYear, fiscalYear);
        }

        Long total = voucherMapper.selectCount(wrapper);

        LambdaQueryWrapper<OriginalVoucher> archivedWrapper = wrapper.clone();
        archivedWrapper.eq(OriginalVoucher::getArchiveStatus, "ARCHIVED");
        Long archived = voucherMapper.selectCount(archivedWrapper);

        LambdaQueryWrapper<OriginalVoucher> pendingWrapper = wrapper.clone();
        pendingWrapper.eq(OriginalVoucher::getArchiveStatus, "PENDING");
        Long pending = voucherMapper.selectCount(pendingWrapper);

        return new OriginalVoucherStats(total, archived, pending, total - archived - pending);
    }

    /**
     * 统计数据DTO
     */
    public record OriginalVoucherStats(Long total, Long archived, Long pending, Long draft) {
    }
}
