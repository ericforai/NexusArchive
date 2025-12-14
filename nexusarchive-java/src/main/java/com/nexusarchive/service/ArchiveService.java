package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 档案核心业务服务
 * <p>
 * Handles lifecycle of Electronic Accounting Archives:
 * Creation, Retrieval, Update, Deletion (CRUD).
 * Enforces Data Scoping and Business Rules.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveMapper archiveMapper;
    private final com.nexusarchive.service.strategy.ArchivalCodeGenerator codeGenerator;
    private final DataScopeService dataScopeService;

    /**
     * 分页查询档案
     *
     * @param page 页码
     * @param limit 每页条数
     * @param search 搜索关键词
     * @param status 状态
     * @param categoryCode 类别号
     * @param orgId 部门ID
     * @return 分页结果
     */
    public Page<Archive> getArchives(int page, int limit, String search, String status, String categoryCode, String orgId, String uniqueBizId) {
        Page<Archive> pageObj = new Page<>(page, limit);
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();

        if (search != null && !search.isEmpty()) {
            wrapper.and(w -> w.like("title", search)
                    .or().like("archive_code", search)
                    .or().like("fonds_no", search)
                    .or().like("org_name", search));
        }

        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }

        if (categoryCode != null && !categoryCode.isEmpty()) {
            wrapper.eq("category_code", categoryCode);
        }

        if (orgId != null && !orgId.isEmpty()) {
            wrapper.eq("department_id", orgId);
        }
        if (uniqueBizId != null && !uniqueBizId.isEmpty()) {
            wrapper.eq("unique_biz_id", uniqueBizId);
        }

        DataScopeContext scope = dataScopeService.resolve();
        dataScopeService.applyArchiveScope(wrapper, scope);

        // Optimize: Use index-friendly sorting
        wrapper.orderByDesc("created_at");

        return archiveMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 根据ID获取档案
     *
     * @param id 档案ID
     * @return 档案详情
     * @throws BusinessException if not found or access denied
     */
    public Archive getArchiveById(String id) {
        Archive archive = archiveMapper.selectById(id);
        if (archive == null) {
            throw new BusinessException("档案不存在");
        }
        DataScopeContext scope = dataScopeService.resolve();
        if (!dataScopeService.canAccessArchive(archive, scope)) {
            throw new BusinessException("无权查看该档案");
        }
        return archive;
    }

    /**
     * 创建档案
     * <p>
     * Handles auto-generation of Archive Code and ensures uniqueness.
     * </p>
     *
     * @param archive 档案实体
     * @param userId 创建人ID
     * @return 创建后的档案
     */
    @Transactional(rollbackFor = Exception.class)
    public Archive createArchive(Archive archive, String userId) {
        // 如果没有指定档号，尝试自动生成
        if ((archive.getArchiveCode() == null || archive.getArchiveCode().isEmpty()) 
                && archive.getFondsNo() != null 
                && archive.getFiscalYear() != null) {
            String code = codeGenerator.generateNextCode(archive);
            archive.setArchiveCode(code);
        }

        // Double-check uniqueness (Best effort before DB constraint)
        if (archive.getArchiveCode() != null) {
            checkArchiveCodeUnique(archive.getArchiveCode(), null);
        }
        if (archive.getUniqueBizId() != null && !archive.getUniqueBizId().isEmpty()) {
            checkUniqueBizId(archive.getUniqueBizId(), null);
        }

        if (archive.getId() == null) {
            archive.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        
        archive.setCreatedBy(userId);
        if (archive.getStatus() == null) {
            archive.setStatus("draft");
        }
        
        archive.setCreatedTime(LocalDateTime.now());
        archive.setLastModifiedTime(LocalDateTime.now());
        
        try {
            archiveMapper.insert(archive);
        } catch (DuplicateKeyException e) {
            log.error("Duplicate key error during archive creation: {}", e.getMessage());
            throw new BusinessException(409, "保存失败：档号或唯一标识已存在");
        }

        return archive;
    }

    /**
     * 更新档案
     *
     * @param id 档案ID
     * @param archive 更新的数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateArchive(String id, Archive archive) {
        Archive existing = getArchiveById(id);

        // Check if archive code is being changed and if it conflicts
        if (archive.getArchiveCode() != null && !existing.getArchiveCode().equals(archive.getArchiveCode())) {
            checkArchiveCodeUnique(archive.getArchiveCode(), id);
        }
        if (archive.getUniqueBizId() != null && !archive.getUniqueBizId().isEmpty()
                && !archive.getUniqueBizId().equals(existing.getUniqueBizId())) {
            checkUniqueBizId(archive.getUniqueBizId(), id);
        }

        archive.setId(id);
        archive.setLastModifiedTime(LocalDateTime.now());

        try {
            archiveMapper.updateById(archive);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "更新失败：档号或唯一标识已存在");
        }
    }

    /**
     * 删除档案
     *
     * @param id 档案ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteArchive(String id) {
        Archive archive = getArchiveById(id);
        // Logic delete is handled by @TableLogic in entity
        archiveMapper.deleteById(id);
    }

    /**
     * 根据唯一业务ID获取档案
     */
    public Archive getByUniqueBizId(String uniqueBizId) {
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();
        wrapper.eq("unique_biz_id", uniqueBizId);
        return archiveMapper.selectOne(wrapper);
    }

    /**
     * 获取最近创建的档案
     */
    public List<Archive> getRecentArchives(int limit) {
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();
        DataScopeContext scope = dataScopeService.resolve();
        dataScopeService.applyArchiveScope(wrapper, scope);
        wrapper.orderByDesc("created_at").last("LIMIT " + limit);
        return archiveMapper.selectList(wrapper);
    }

    /**
     * 批量获取档案
     */
    public List<Archive> getArchivesByIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return archiveMapper.selectBatchIds(ids);
    }

    /**
     * Helper to check uniqueness
     */
    private void checkArchiveCodeUnique(String code, String excludeId) {
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();
        wrapper.eq("archive_code", code);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (archiveMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("档号已存在: " + code);
        }
    }

    private void checkUniqueBizId(String uniqueBizId, String excludeId) {
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();
        wrapper.eq("unique_biz_id", uniqueBizId);
        wrapper.eq("deleted", 0);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (archiveMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(409, "唯一业务ID已存在: " + uniqueBizId);
        }
    }
}
