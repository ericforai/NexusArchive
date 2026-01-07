// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: ArchiveService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
public class ArchiveService implements ArchiveReadService, ArchiveWriteService {

    private final ArchiveMapper archiveMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final com.nexusarchive.service.strategy.ArchivalCodeGenerator codeGenerator;
    private final DataScopeService dataScopeService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * 分页查询档案
     *
     * @param page         页码
     * @param limit        每页条数
     * @param search       搜索关键词
     * @param status       状态
     * @param categoryCode 类别号
     * @param orgId        部门ID
     * @param fondsNo      全宗号(显式过滤，可选)
     * @return 分页结果
     */
    @Override
    public Page<Archive> getArchives(int page, int limit, String search, String status, String categoryCode,
            String orgId, String uniqueBizId, String subType, String fondsNo) {
        Page<Archive> pageObj = new Page<>(page, limit);
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();

        if (search != null && !search.isEmpty()) {
            wrapper.and(w -> w.like("title", search)
                    .or().like("archive_code", search)
                    .or().like("fonds_no", search)
                    .or().like("org_name", search));
        }

        if (status != null && !status.isEmpty()) {
            if (status.contains(",")) {
                wrapper.in("status", Arrays.asList(status.split(",")));
            } else {
                wrapper.eq("status", status);
            }
        }

        if (categoryCode != null && !categoryCode.isEmpty()) {
            wrapper.eq("category_code", categoryCode);

            // [FIX P0-CRIT] Accounting Archives Compliance (DA/T 94-2022)
            // If querying an Accounting Category (AC01-AC04) and no status is specified,
            // FORCE strictly 'archived' status. Drafts/Pending items must NOT appear in the Repository.
            if ((status == null || status.isEmpty()) && isAccountingCategory(categoryCode)) {
                wrapper.eq("status", "archived");
            }
        }

        if (orgId != null && !orgId.isEmpty()) {
            wrapper.eq("department_id", orgId);
        }
        if (uniqueBizId != null && !uniqueBizId.isEmpty()) {
            wrapper.eq("unique_biz_id", uniqueBizId);
        }

        // [FIXED P0-1] Dynamic SubType Filter with SQL Injection Protection
        if (subType != null && !subType.isEmpty()) {
            // 白名单校验，防止 SQL 注入 (委托给策略类)
            if (!com.nexusarchive.service.strategy.ArchiveValidationPolicy.isValidSubType(subType, categoryCode)) {
                throw new BusinessException(400, "Invalid subType parameter: " + subType);
            }

            if ("AC02".equals(categoryCode)) {
                // 使用 PostgreSQL JSONB 包含操作符，参数化查询
                wrapper.apply("custom_metadata::jsonb @> {0}::jsonb",
                        String.format("{\"bookType\":\"%s\"}", escapeJson(subType)));
            } else if ("AC03".equals(categoryCode)) {
                wrapper.apply("custom_metadata::jsonb @> {0}::jsonb",
                        String.format("{\"reportType\":\"%s\"}", escapeJson(subType)));
            } else if ("AC04".equals(categoryCode)) {
                wrapper.apply("custom_metadata::jsonb @> {0}::jsonb",
                        String.format("{\"otherType\":\"%s\"}", escapeJson(subType)));
            } else {
                // Fallback: try to match bookType as default if no category context
                wrapper.apply("custom_metadata::jsonb @> {0}::jsonb",
                        String.format("{\"bookType\":\"%s\"}", escapeJson(subType)));
            }
        }

        // [ENHANCED] 显式全宗过滤：如果前端传递 fondsNo，优先使用
        // 这提高了代码可读性，使数据隔离逻辑更加明确
        if (fondsNo != null && !fondsNo.isEmpty()) {
            wrapper.eq("fonds_no", fondsNo);
            log.debug("Explicit fondsNo filter applied: {}", fondsNo);
        } else {
            // 后备：依赖 DataScopeService 的自动隔离机制
            DataScopeContext scope = dataScopeService.resolve();
            dataScopeService.applyArchiveScope(wrapper, scope);
        }

        // Optimize: Use index-friendly sorting
        wrapper.orderByDesc("created_time");

        return archiveMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 根据ID获取档案
     *
     * @param id 档案ID
     * @return 档案详情
     * @throws BusinessException if not found or access denied
     */
    @Override
    public Archive getArchiveById(String idOrCode) {
        Archive archive = archiveMapper.selectById(idOrCode);
        if (archive == null) {
            // Fallback: try archive_code lookup (supporting human-readable codes in URLs)
            QueryWrapper<Archive> wrapper = new QueryWrapper<>();
            wrapper.eq("archive_code", idOrCode);
            archive = archiveMapper.selectOne(wrapper);
        }

        if (archive == null) {
            throw new BusinessException(404, "档案不存在: " + idOrCode);
        }

        DataScopeContext scope = dataScopeService.resolve();
        if (!dataScopeService.canAccessArchive(archive, scope)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION_TO_VIEW_ARCHIVE);
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
     * @param userId  创建人ID
     * @return 创建后的档案
     */
    @Override
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
     * @param id      档案ID
     * @param archive 更新的数据
     */
    @Override
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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArchive(String id) {
        Archive archive = getArchiveById(id);
        // Logic delete is handled by @TableLogic in entity
        archiveMapper.deleteById(id);
    }

    /**
     * 根据唯一业务ID获取档案
     */
    @Override
    public Archive getByUniqueBizId(String uniqueBizId) {
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();
        wrapper.eq("unique_biz_id", uniqueBizId);
        return archiveMapper.selectOne(wrapper);
    }

    /**
     * 获取最近创建的档案
     */
    @Override
    public List<Archive> getRecentArchives(int limit) {
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();
        DataScopeContext scope = dataScopeService.resolve();
        dataScopeService.applyArchiveScope(wrapper, scope);
        wrapper.orderByDesc("created_time").last("LIMIT " + limit);
        return archiveMapper.selectList(wrapper);
    }

    /**
     * 批量获取档案 (受控)
     * [FIXED P1-3] 在 SQL 层面应用数据权限过滤，避免无效查询
     */
    @Override
    public List<Archive> getArchivesByIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        // [FIXED P1-3] 先应用数据权限过滤，再查询
        DataScopeContext scope = dataScopeService.resolve();

        // 如果用户有全部权限，直接查询
        if (scope.isAll()) {
            return archiveMapper.selectBatchIds(ids);
        }

        // 否则，构建带权限过滤的查询
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids);
        dataScopeService.applyArchiveScope(wrapper, scope);

        return archiveMapper.selectList(wrapper);
    }

    /**
     * 根据部门ID列表获取档案ID列表
     * [Sec] 用于跨模块权限校验
     */
    @Override
    public List<String> getArchiveIdsByDepartmentIds(java.util.Collection<String> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return archiveMapper.selectIdsByDepartmentIds(departmentIds);
    }

    /**
     * 获取档案关联的文件列表
     * @param archiveId 档案ID
     * @return 文件列表
     */
    @Override
    public List<ArcFileContent> getFilesByArchiveId(String archiveId) {
        // Check existence and permission
        getArchiveById(archiveId); // This performs checks

        List<ArcFileContent> result = new java.util.ArrayList<>();

        // 1. 原有逻辑：从 arc_file_content 获取直接关联的文件
        QueryWrapper<ArcFileContent> wrapper = new QueryWrapper<>();
        wrapper.eq("item_id", archiveId);
        wrapper.orderByAsc("created_time");
        result.addAll(arcFileContentMapper.selectList(wrapper));

        // 2. 新增逻辑：从 acc_archive_attachment 获取智能匹配关联的文件 (使用强类型 Mapper)
        try {
            List<ArcFileContent> attachments = arcFileContentMapper.selectAttachmentsByArchiveId(archiveId);
            result.addAll(attachments);
        } catch (Exception e) {
            log.warn("Failed to query attachments from acc_archive_attachment: {}", e.getMessage());
        }

        return result;
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
            throw new BusinessException(ErrorCode.ARCHIVE_CODE_EXISTS, code);
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

    // [REFACTORED] isValidSubType 方法已迁移至 ArchiveValidationPolicy 策略类

    /**
     * [ADDED P0-1] JSON 字符串转义
     * 防止 JSON 注入
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Helper to check if category is one of the standard Accounting Archive categories
     */
    private boolean isAccountingCategory(String code) {
        return "AC01".equals(code) || "AC02".equals(code) || "AC03".equals(code) || "AC04".equals(code);
    }
}
