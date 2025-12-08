package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 档案服务
 */
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveMapper archiveMapper;
    private final com.nexusarchive.service.strategy.ArchivalCodeGenerator codeGenerator;
    private final DataScopeService dataScopeService;

    /**
     * 分页查询档案
     */
    public Page<Archive> getArchives(int page, int limit, String search, String status, String categoryCode, String orgId) {
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

        DataScopeContext scope = dataScopeService.resolve();
        dataScopeService.applyArchiveScope(wrapper, scope);

        wrapper.orderByDesc("created_at");

        return archiveMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 根据ID获取档案
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
     */
    @Transactional
    public Archive createArchive(Archive archive, String userId) {
        // 如果没有指定档号，尝试自动生成
        if ((archive.getArchiveCode() == null || archive.getArchiveCode().isEmpty()) 
                && archive.getFondsNo() != null 
                && archive.getFiscalYear() != null) {
            String code = codeGenerator.generateNextCode(archive);
            archive.setArchiveCode(code);
        }

        // 检查档号唯一性
        if (archive.getArchiveCode() != null) {
            QueryWrapper<Archive> wrapper = new QueryWrapper<>();
            wrapper.eq("archive_code", archive.getArchiveCode());
            if (archiveMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("档号已存在: " + archive.getArchiveCode());
            }
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
        
        archiveMapper.insert(archive);
        return archive;
    }

    /**
     * 更新档案
     */
    @Transactional
    public void updateArchive(String id, Archive archive) {
        Archive existing = getArchiveById(id);

        // 检查档号唯一性
        if (!existing.getArchiveCode().equals(archive.getArchiveCode())) {
            QueryWrapper<Archive> wrapper = new QueryWrapper<>();
            wrapper.eq("archive_code", archive.getArchiveCode());
            if (archiveMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("档号已存在");
            }
        }

        archive.setId(id);
        archive.setLastModifiedTime(LocalDateTime.now());
        archiveMapper.updateById(archive);
    }

    /**
     * 删除档案
     */
    @Transactional
    public void deleteArchive(String id) {
        Archive archive = getArchiveById(id);
        // 逻辑删除
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
    public java.util.List<Archive> getRecentArchives(int limit) {
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();
        DataScopeContext scope = dataScopeService.resolve();
        dataScopeService.applyArchiveScope(wrapper, scope);
        wrapper.orderByDesc("created_at").last("LIMIT " + limit);
        return archiveMapper.selectList(wrapper);
    }

    /**
     * 批量获取档案
     */
    public java.util.List<Archive> getArchivesByIds(java.util.Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return archiveMapper.selectBatchIds(ids);
    }

}
