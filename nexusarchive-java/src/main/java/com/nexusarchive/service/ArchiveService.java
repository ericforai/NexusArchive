package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
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

    /**
     * 分页查询档案
     */
    public Page<Archive> getArchives(int page, int limit, String search, String status) {
        Page<Archive> pageObj = new Page<>(page, limit);
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();

        if (search != null && !search.isEmpty()) {
            wrapper.and(w -> w.like("title", search)
                    .or().like("archive_code", search)
                    .or().like("fonds_no", search));
        }

        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }

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
        return archive;
    }

    /**
     * 创建档案
     */
    @Transactional
    public Archive createArchive(Archive archive, String userId) {
        // 检查档号唯一性
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();
        wrapper.eq("archive_code", archive.getArchiveCode());
        if (archiveMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("档号已存在");
        }

        if (archive.getId() == null) {
            archive.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        
        archive.setCreatedBy(userId);
        if (archive.getStatus() == null) {
            archive.setStatus("draft");
        }
        
        archive.setCreatedAt(LocalDateTime.now());
        archive.setUpdatedAt(LocalDateTime.now());
        
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
        archive.setUpdatedAt(LocalDateTime.now());
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
}
