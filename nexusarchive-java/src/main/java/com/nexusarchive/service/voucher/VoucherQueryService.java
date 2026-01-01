// Input: MyBatis-Plus, Spring Framework, Lombok
// Output: VoucherQueryService 类
// Pos: 服务层 - 原始凭证查询服务

package com.nexusarchive.service.voucher;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.entity.OriginalVoucherType;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.OriginalVoucherTypeMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 原始凭证查询服务
 * <p>
 * 负责原始凭证的查询操作：
 * - 分页查询
 * - 详情查询
 * - 文件查询
 * - 版本历史查询
 * - 关联查询
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherQueryService {

    private final OriginalVoucherMapper voucherMapper;
    private final OriginalVoucherFileMapper fileMapper;
    private final VoucherRelationMapper relationMapper;
    private final OriginalVoucherTypeMapper typeMapper;

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
     * 根据ID获取文件详情
     */
    public OriginalVoucherFile getFileById(String fileId) {
        return fileMapper.selectById(fileId);
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
}
