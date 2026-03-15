// Input: MyBatis-Plus、Spring Framework、Java 标准库
// Output: OriginalVoucherService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.service.helper.OriginalVoucherHelper;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * 原始凭证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OriginalVoucherService {

    private final OriginalVoucherMapper voucherMapper;
    private final OriginalVoucherFileMapper fileMapper;
    private final VoucherRelationMapper relationMapper;
    private final OriginalVoucherTypeMapper typeMapper;
    private final FileStorageService fileStorageService;
    private final com.nexusarchive.service.parser.PdfInvoiceParser pdfInvoiceParser;
    private final com.nexusarchive.service.parser.OfdInvoiceParser ofdInvoiceParser;
    private final DataScopeService dataScopeService;
    private final OriginalVoucherHelper helper;

    public Page<OriginalVoucher> getVouchers(int page, int limit, String search,
            String category, String type, String status, String fondsCode, String fiscalYear, String poolStatus) {
        Page<OriginalVoucher> pageObj = new Page<>(page, limit);
        LambdaQueryWrapper<OriginalVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OriginalVoucher::getIsLatest, true);

        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(OriginalVoucher::getVoucherNo, search)
                    .or().like(OriginalVoucher::getCounterparty, search)
                    .or().like(OriginalVoucher::getSummary, search));
        }
        if (StringUtils.hasText(category)) wrapper.eq(OriginalVoucher::getVoucherCategory, category);
        if (StringUtils.hasText(type)) {
            List<String> typeAliases = helper.getTypeAliases(type);
            if (typeAliases.size() == 1) wrapper.eq(OriginalVoucher::getVoucherType, type);
            else wrapper.in(OriginalVoucher::getVoucherType, typeAliases);
        }

        if (StringUtils.hasText(poolStatus)) {
            if (poolStatus.contains("ARCHIVED")) wrapper.eq(OriginalVoucher::getArchiveStatus, "ARCHIVED");
            else wrapper.eq(OriginalVoucher::getArchiveStatus, "DRAFT");
        } else if (StringUtils.hasText(status)) wrapper.eq(OriginalVoucher::getArchiveStatus, status);

        if (StringUtils.hasText(fondsCode)) wrapper.eq(OriginalVoucher::getFondsCode, fondsCode);
        else dataScopeService.applyOriginalVoucherScope(wrapper, dataScopeService.resolve());
        
        if (StringUtils.hasText(fiscalYear)) wrapper.eq(OriginalVoucher::getFiscalYear, fiscalYear);

        wrapper.orderByDesc(OriginalVoucher::getCreatedTime);
        return voucherMapper.selectPage(pageObj, wrapper);
    }

    public OriginalVoucher getById(String id) {
        OriginalVoucher v = voucherMapper.selectById(id);
        if (v == null) {
            // 支持通过凭证编号查询
            v = voucherMapper.selectOne(new LambdaQueryWrapper<OriginalVoucher>()
                    .eq(OriginalVoucher::getVoucherNo, id)
                    .last("LIMIT 1"));
        }
        if (v == null || v.getDeleted() == 1) throw new BusinessException("原始凭证不存在: " + id);
        return v;
    }

    public List<OriginalVoucherFile> getFiles(String voucherId) {
        // 支持通过凭证编号查询
        OriginalVoucher v = voucherMapper.selectById(voucherId);
        if (v == null) {
            v = voucherMapper.selectOne(new LambdaQueryWrapper<OriginalVoucher>()
                    .eq(OriginalVoucher::getVoucherNo, voucherId)
                    .last("LIMIT 1"));
        }
        if (v == null) {
            throw new BusinessException("原始凭证不存在: " + voucherId);
        }
        return fileMapper.findByVoucherId(v.getId());
    }

    public OriginalVoucherFile getFileById(String fileId) { return fileMapper.selectById(fileId); }

    public ResponseEntity<Resource> downloadFile(String fileId) {
        OriginalVoucherFile f = getFileById(fileId);
        if (f == null || !StringUtils.hasText(f.getStoragePath())) throw new BusinessException(ErrorCode.FILE_NOT_FOUND, fileId);

        Path filePath = fileStorageService.resolvePath(f.getStoragePath());
        if (!fileStorageService.exists(f.getStoragePath())) throw new BusinessException(ErrorCode.PHYSICAL_FILE_NOT_FOUND, f.getStoragePath());

        Resource resource = new FileSystemResource(filePath.toFile());
        String contentType = helper.determineContentType(f.getFileType(), f.getFileName());
        String encodedFileName = URLEncoder.encode(f.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    public List<OriginalVoucher> getVersionHistory(String id) { return voucherMapper.findVersionHistory(id); }

    public List<VoucherRelation> getAccountingRelations(String voucherId) { return relationMapper.findByOriginalVoucherId(voucherId); }

    @Transactional
    public OriginalVoucher create(OriginalVoucher voucher, String userId) {
        helper.validateVoucherType(voucher.getVoucherCategory(), voucher.getVoucherType());
        voucher.setVoucherNo(helper.generateVoucherNo(voucher.getFondsCode(), voucher.getFiscalYear(), voucher.getVoucherCategory()));
        if (voucher.getId() == null) voucher.setId(UUID.randomUUID().toString());
        if (voucher.getBusinessDate() == null) voucher.setBusinessDate(java.time.LocalDate.now());
        voucher.setVersion(1);
        voucher.setIsLatest(true);
        voucher.setArchiveStatus("DRAFT");
        voucher.setCreatedBy(userId);
        voucher.setCreatedTime(LocalDateTime.now());

        if (!StringUtils.hasText(voucher.getRetentionPeriod())) {
            OriginalVoucherType typeInfo = typeMapper.findByTypeCode(voucher.getVoucherType());
            voucher.setRetentionPeriod(typeInfo != null ? typeInfo.getDefaultRetention() : "30Y");
        }

        voucherMapper.insert(voucher);
        return voucher;
    }

    @Transactional
    public OriginalVoucher update(String id, OriginalVoucher updates, String reason, String userId) {
        OriginalVoucher existing = getById(id);
        if ("ARCHIVED".equals(existing.getArchiveStatus())) return createNewVersion(existing, updates, reason, userId);

        updateFields(existing, updates);
        existing.setLastModifiedBy(userId);
        existing.setLastModifiedTime(LocalDateTime.now());
        voucherMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public OriginalVoucher createNewVersion(OriginalVoucher old, OriginalVoucher updates, String reason, String userId) {
        old.setIsLatest(false);
        voucherMapper.updateById(old);

        OriginalVoucher nv = OriginalVoucher.builder()
                .id(UUID.randomUUID().toString()).voucherNo(old.getVoucherNo())
                .voucherCategory(old.getVoucherCategory()).voucherType(old.getVoucherType())
                .businessDate(updates.getBusinessDate() != null ? updates.getBusinessDate() : old.getBusinessDate())
                .amount(updates.getAmount() != null ? updates.getAmount() : old.getAmount())
                .currency(updates.getCurrency() != null ? updates.getCurrency() : old.getCurrency())
                .counterparty(updates.getCounterparty() != null ? updates.getCounterparty() : old.getCounterparty())
                .summary(updates.getSummary() != null ? updates.getSummary() : old.getSummary())
                .creator(old.getCreator())
                .auditor(updates.getAuditor() != null ? updates.getAuditor() : old.getAuditor())
                .bookkeeper(updates.getBookkeeper() != null ? updates.getBookkeeper() : old.getBookkeeper())
                .approver(updates.getApprover() != null ? updates.getApprover() : old.getApprover())
                .sourceSystem(old.getSourceSystem()).sourceDocId(old.getSourceDocId())
                .fondsCode(old.getFondsCode()).fiscalYear(old.getFiscalYear())
                .retentionPeriod(old.getRetentionPeriod()).archiveStatus("DRAFT")
                .version(old.getVersion() + 1).parentVersionId(old.getId())
                .versionReason(reason).isLatest(true).createdBy(userId).createdTime(LocalDateTime.now())
                .build();

        voucherMapper.insert(nv);
        return nv;
    }

    private void updateFields(OriginalVoucher e, OriginalVoucher u) {
        if (u.getBusinessDate() != null) e.setBusinessDate(u.getBusinessDate());
        if (u.getAmount() != null) e.setAmount(u.getAmount());
        if (u.getCurrency() != null) e.setCurrency(u.getCurrency());
        if (u.getCounterparty() != null) e.setCounterparty(u.getCounterparty());
        if (u.getSummary() != null) e.setSummary(u.getSummary());
        if (u.getAuditor() != null) e.setAuditor(u.getAuditor());
        if (u.getBookkeeper() != null) e.setBookkeeper(u.getBookkeeper());
        if (u.getApprover() != null) e.setApprover(u.getApprover());
    }

    @Transactional
    public void delete(String id, String userId) {
        OriginalVoucher v = getById(id);
        if ("ARCHIVED".equals(v.getArchiveStatus())) throw new BusinessException("已归档的原始凭证不允许删除");
        v.setDeleted(1);
        v.setLastModifiedBy(userId);
        v.setLastModifiedTime(LocalDateTime.now());
        voucherMapper.updateById(v);
    }

    @Transactional
    public OriginalVoucherFile addFile(String voucherId, org.springframework.web.multipart.MultipartFile file, String role, String userId) {
        getById(voucherId);
        if (file.isEmpty()) throw new BusinessException("上传文件为空");

        try {
            String originalFilename = file.getOriginalFilename();
            String fileId = UUID.randomUUID().toString();
            String ext = (originalFilename != null && originalFilename.contains(".")) ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String type = ext.replace(".", "").toUpperCase();
            String path = "original-vouchers/" + voucherId + "/" + fileId + ext;
            fileStorageService.saveFile(file.getInputStream(), path);

            OriginalVoucherFile vf = OriginalVoucherFile.builder()
                    .id(fileId).voucherId(voucherId).fileName(originalFilename).fileType(type)
                    .fileSize(file.getSize()).storagePath(path).fileHash(helper.calculateHash(file.getBytes()))
                    .hashAlgorithm("SM3").fileRole(role != null ? role : "PRIMARY")
                    .sequenceNo(getFiles(voucherId).size() + 1).createdBy(userId).createdTime(LocalDateTime.now())
                    .build();

            fileMapper.insert(vf);

            if (("PDF".equals(type) || "OFD".equals(type)) && ("PRIMARY".equals(vf.getFileRole()) || "ORIGINAL".equals(vf.getFileRole()))) {
                try {
                    File physicalFile = fileStorageService.resolvePath(path).toFile();
                    java.util.Map<String, Object> res = "PDF".equals(type) 
                        ? pdfInvoiceParser.parse(physicalFile)
                        : ofdInvoiceParser.parse(physicalFile);
                        
                    OriginalVoucher v = getById(voucherId);
                    boolean upd = false;
                    if (res != null && res.containsKey("total_amount_value") && (v.getAmount() == null || v.getAmount().compareTo(java.math.BigDecimal.ZERO) == 0)) {
                        v.setAmount(new java.math.BigDecimal((String) res.get("total_amount_value")));
                        upd = true;
                    }
                    if (res != null && res.containsKey("invoice_date_value")) {
                        v.setBusinessDate(java.time.LocalDate.parse((String) res.get("invoice_date_value")));
                        upd = true;
                    }
                    if (upd) voucherMapper.updateById(v);
                } catch (Exception e) { log.error("Parsing failed for {}: {}", type, e.getMessage()); }
            }
            return vf;
        } catch (java.io.IOException e) { throw new BusinessException("文件上传失败: " + e.getMessage()); }
    }

    @Transactional
    public VoucherRelation createRelation(String ovId, String avId, String desc, String userId) {
        getById(ovId);
        if (relationMapper.countRelation(ovId, avId) > 0) throw new BusinessException("关联关系已存在");
        VoucherRelation r = VoucherRelation.builder().id(UUID.randomUUID().toString()).originalVoucherId(ovId).accountingVoucherId(avId).relationType("ORIGINAL_TO_ACCOUNTING").relationDesc(desc).createdBy(userId).createdTime(LocalDateTime.now()).build();
        relationMapper.insert(r);
        return r;
    }

    @Transactional
    public void deleteRelation(String id) {
        VoucherRelation r = relationMapper.selectById(id);
        if (r != null) { r.setDeleted(1); relationMapper.updateById(r); }
    }

    public List<OriginalVoucherType> getAllTypes() { return typeMapper.findAllEnabled(); }
    public List<OriginalVoucherType> getTypesByCategory(String cat) { return typeMapper.findByCategory(cat); }

    @Transactional
    public void submitForArchive(String id, String userId) {
        OriginalVoucher v = getById(id);
        if (!"DRAFT".equals(v.getArchiveStatus())) throw new BusinessException("只有草稿状态的凭证可以提交归档");
        validateForArchive(v);
        v.setArchiveStatus(OperationResult.PENDING);
        v.setLastModifiedBy(userId);
        v.setLastModifiedTime(LocalDateTime.now());
        voucherMapper.updateById(v);
    }

    @Transactional
    public void confirmArchive(String id, String userId) {
        OriginalVoucher v = getById(id);
        if (!OperationResult.PENDING.equals(v.getArchiveStatus())) throw new BusinessException("只有待归档状态的凭证可以确认归档");
        v.setArchiveStatus("ARCHIVED");
        v.setArchivedTime(LocalDateTime.now());
        v.setLastModifiedBy(userId);
        v.setLastModifiedTime(LocalDateTime.now());
        voucherMapper.updateById(v);
    }

    private void validateForArchive(OriginalVoucher v) {
        if (!StringUtils.hasText(v.getBusinessDate().toString())) throw new BusinessException("业务日期不能为空");
        if (!StringUtils.hasText(v.getFondsCode())) throw new BusinessException("全宗号不能为空");
        if (getFiles(v.getId()).isEmpty()) throw new BusinessException("原始凭证必须至少包含一个文件");
    }

    public OriginalVoucherStats getStats(String fonds, String year) {
        LambdaQueryWrapper<OriginalVoucher> w = new LambdaQueryWrapper<>();
        w.eq(OriginalVoucher::getIsLatest, true);
        if (StringUtils.hasText(fonds)) w.eq(OriginalVoucher::getFondsCode, fonds);
        else dataScopeService.applyOriginalVoucherScope(w, dataScopeService.resolve());
        if (StringUtils.hasText(year)) w.eq(OriginalVoucher::getFiscalYear, year);

        Long total = voucherMapper.selectCount(w);
        Long archived = voucherMapper.selectCount(w.clone().eq(OriginalVoucher::getArchiveStatus, "ARCHIVED"));
        Long pending = voucherMapper.selectCount(w.clone().eq(OriginalVoucher::getArchiveStatus, OperationResult.PENDING));
        return new OriginalVoucherStats(total, archived, pending, total - archived - pending);
    }

    public record OriginalVoucherStats(Long total, Long archived, Long pending, Long draft) {}
}
