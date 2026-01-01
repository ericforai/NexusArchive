// Input: Spring Framework, Lombok
// Output: OriginalVoucherFacade 类
// Pos: 服务层 - 原始凭证门面

package com.nexusarchive.service.voucher;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.entity.OriginalVoucherType;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 原始凭证门面服务
 * <p>
 * 协调各个原始凭证处理模块，统一对外接口
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OriginalVoucherFacade {

    private final VoucherQueryService queryService;
    private final VoucherCrudService crudService;
    private final VoucherFileManager fileManager;
    private final VoucherRelationService relationService;

    // ===== 查询接口 =====

    public Page<OriginalVoucher> getVouchers(int page, int limit, String search,
            String category, String type, String status, String fondsCode, String fiscalYear) {
        return queryService.getVouchers(page, limit, search, category, type, status, fondsCode, fiscalYear);
    }

    public OriginalVoucher getById(String id) {
        return queryService.getById(id);
    }

    public List<OriginalVoucherFile> getFiles(String voucherId) {
        return queryService.getFiles(voucherId);
    }

    public OriginalVoucherFile getFileById(String fileId) {
        return queryService.getFileById(fileId);
    }

    public List<OriginalVoucher> getVersionHistory(String id) {
        return queryService.getVersionHistory(id);
    }

    public List<VoucherRelation> getAccountingRelations(String voucherId) {
        return queryService.getAccountingRelations(voucherId);
    }

    public List<OriginalVoucherType> getAllTypes() {
        return queryService.getAllTypes();
    }

    public List<OriginalVoucherType> getTypesByCategory(String categoryCode) {
        return queryService.getTypesByCategory(categoryCode);
    }

    // ===== 创建接口 =====

    @Transactional
    public OriginalVoucher create(OriginalVoucher voucher, String userId) {
        return crudService.create(voucher, userId);
    }

    // ===== 更新接口 =====

    @Transactional
    public OriginalVoucher update(String id, OriginalVoucher updates, String reason, String userId) {
        return crudService.update(id, updates, reason, userId);
    }

    // ===== 删除接口 =====

    @Transactional
    public void delete(String id, String userId) {
        crudService.delete(id, userId);
    }

    // ===== 文件管理 =====

    @Transactional
    public OriginalVoucherFile addFile(String voucherId, MultipartFile file, String fileRole, String userId) {
        return fileManager.addFile(voucherId, file, fileRole, userId);
    }

    public ResponseEntity<Resource> downloadFile(String fileId) {
        return fileManager.downloadFile(fileId);
    }

    // ===== 关联管理 =====

    @Transactional
    public VoucherRelation createRelation(String originalVoucherId, String accountingVoucherId,
            String desc, String userId) {
        return relationService.createRelation(originalVoucherId, accountingVoucherId, desc, userId);
    }

    @Transactional
    public void deleteRelation(String relationId) {
        relationService.deleteRelation(relationId);
    }

    // ===== 归档状态管理 =====

    @Transactional
    public void submitForArchive(String id, String userId) {
        crudService.submitForArchive(id, userId);
    }

    @Transactional
    public void confirmArchive(String id, String userId) {
        crudService.confirmArchive(id, userId);
    }

    // ===== 统计接口 =====

    public OriginalVoucherStats getStats(String fondsCode, String fiscalYear) {
        // Statistics logic - can be extracted to a separate service if needed
        return null; // Placeholder
    }

    /**
     * 统计数据DTO
     */
    public record OriginalVoucherStats(Long total, Long archived, Long pending, Long draft) {
    }
}
