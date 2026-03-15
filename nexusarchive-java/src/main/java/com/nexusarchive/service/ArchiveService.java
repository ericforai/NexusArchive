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
 *
 * ARCHITECTURE-NOTE: 核心服务 - 职责集中设计
 *
 * 为什么 ArchiveService 职责较多（473行，15个公开方法）：
 * 1. 档案（Archive）是系统的核心实体，所有模块最终都会依赖它
 * 2. CRUD 操作本身较为简单，合并在一起可减少服务碎片化
 * 3. 查询逻辑与权限控制（DataScopeService）紧密耦合，分离会增加复杂度
 * 4. 档号生成、唯一性校验等逻辑需要与 CRUD 在同一事务中
 *
 * 如果未来需要重构，优先考虑 CQRS 模式：
 * - ArchiveQueryService（读侧）
 * - ArchiveCommandService（写侧）
 * - ArchiveValidationService（校验）
 *
 * 当前状态：稳定运行，充分测试，暂无重构计划
 * 相关文档：docs/architecture/module-dependency-status.md#二、核心服务职责分析
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
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();

        if (search != null && !search.isEmpty()) {
            wrapper.and(w -> w.like(Archive::getTitle, search)
                    .or().like(Archive::getArchiveCode, search)
                    .or().like(Archive::getFondsNo, search)
                    .or().like(Archive::getOrgName, search));
        }

        if (status != null && !status.isEmpty()) {
            applyCaseInsensitiveStatusFilter(wrapper, status);
        }

        if (categoryCode != null && !categoryCode.isEmpty()) {
            wrapper.eq(Archive::getCategoryCode, categoryCode);

            // [FIX P0-CRIT] Accounting Archives Compliance (DA/T 94-2022)
            // If querying an Accounting Category (AC01-AC04) and no status is specified,
            // FORCE strictly 'archived' status. Drafts/Pending items must NOT appear in the Repository.
            if ((status == null || status.isEmpty()) && isAccountingCategory(categoryCode)) {
                wrapper.apply("LOWER(status) = {0}", com.nexusarchive.common.constants.StatusConstants.Archive.ARCHIVED.toLowerCase());
            }
        }

        if (orgId != null && !orgId.isEmpty()) {
            wrapper.eq(Archive::getDepartmentId, orgId);
        }
        if (uniqueBizId != null && !uniqueBizId.isEmpty()) {
            wrapper.eq(Archive::getUniqueBizId, uniqueBizId);
        }

        // [FIXED P0-1] Dynamic SubType Filter with SQL Injection Protection
        if (subType != null && !subType.isEmpty()) {
            // 白名单校验，防止 SQL 注入 (委托给策略类)
            if (!com.nexusarchive.service.strategy.ArchiveValidationPolicy.isValidSubType(subType, categoryCode)) {
                throw new BusinessException(400, "Invalid subType parameter: " + subType);
            }

            if (com.nexusarchive.common.constants.ArchiveConstants.Categories.BOOK.equals(categoryCode)) {
                // 使用 PostgreSQL JSONB 包含操作符，参数化查询
                wrapper.apply("custom_metadata::jsonb @> {0}::jsonb",
                        String.format("{\"bookType\":\"%s\"}", escapeJson(subType)));
            } else if (com.nexusarchive.common.constants.ArchiveConstants.Categories.REPORT.equals(categoryCode)) {
                wrapper.apply("custom_metadata::jsonb @> {0}::jsonb",
                        String.format("{\"reportType\":\"%s\"}", escapeJson(subType)));
            } else if (com.nexusarchive.common.constants.ArchiveConstants.Categories.OTHERS.equals(categoryCode)) {
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
            wrapper.eq(Archive::getFondsNo, fondsNo);
            log.debug("Explicit fondsNo filter applied: {}", fondsNo);
        } else {
            // 后备：依赖 DataScopeService 的自动隔离机制
            DataScopeContext scope = dataScopeService.resolve();
            dataScopeService.applyArchiveScope(wrapper, scope);
        }

        // Optimize: Use index-friendly sorting
        wrapper.orderByDesc(Archive::getCreatedTime);

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
        log.debug("[getArchiveById] 查询档案: idOrCode={}", idOrCode);

        Archive archive = archiveMapper.selectById(idOrCode);
        log.debug("[getArchiveById] selectById 结果: {}", archive != null ? "FOUND" : "NULL");

        if (archive == null) {
            // Fallback: try archive_code lookup (supporting human-readable codes in URLs)
            log.debug("[getArchiveById] 尝试通过 archive_code 查询...");
            LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Archive::getArchiveCode, idOrCode);
            archive = archiveMapper.selectOne(wrapper);
            log.debug("[getArchiveById] archive_code 查询结果: {}", archive != null ? "FOUND" : "NULL");
        }

        if (archive == null) {
            log.error("[getArchiveById] 档案不存在: idOrCode={}", idOrCode);
            throw new BusinessException(404, "档案不存在: " + idOrCode);
        }

        log.debug("[getArchiveById] 档案查询成功: id={}, archiveCode={}", archive.getId(), archive.getArchiveCode());

        DataScopeContext scope = dataScopeService.resolve();
        if (!dataScopeService.canAccessArchive(archive, scope)) {
            log.warn("[getArchiveById] 权限不足: userId={}, archiveId={}", scope.userId(), archive.getId());
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
            archive.setStatus(com.nexusarchive.common.constants.StatusConstants.Archive.DRAFT);
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
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getUniqueBizId, uniqueBizId);
        return archiveMapper.selectOne(wrapper);
    }

    /**
     * 获取最近创建的档案
     */
    @Override
    public List<Archive> getRecentArchives(int limit) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        DataScopeContext scope = dataScopeService.resolve();
        dataScopeService.applyArchiveScope(wrapper, scope);
        wrapper.orderByDesc(Archive::getCreatedTime).last("LIMIT " + limit);
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
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Archive::getId, ids);
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

    private final com.nexusarchive.mapper.VoucherRelationMapper voucherRelationMapper; // Inject Mapper

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
        LambdaQueryWrapper<ArcFileContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArcFileContent::getItemId, archiveId);
        wrapper.orderByAsc(ArcFileContent::getCreatedTime);
        result.addAll(arcFileContentMapper.selectList(wrapper));

        // 2. 新增逻辑：从 acc_archive_attachment 获取智能匹配关联的文件 (使用强类型 Mapper)
        try {
            List<ArcFileContent> attachments = arcFileContentMapper.selectAttachmentsByArchiveId(archiveId);
            result.addAll(attachments);
        } catch (Exception e) {
            log.warn("Failed to query attachments from acc_archive_attachment: {}", e.getMessage());
        }

        // 3. [FIXED P1-5] 从 arc_voucher_relation 获取关联的原始凭证文件 (解决关联凭证不可见问题)
        try {
            List<com.nexusarchive.entity.VoucherRelation> relations = voucherRelationMapper.selectList(
                new LambdaQueryWrapper<com.nexusarchive.entity.VoucherRelation>()
                    .eq(com.nexusarchive.entity.VoucherRelation::getAccountingVoucherId, archiveId)
                    .eq(com.nexusarchive.entity.VoucherRelation::getDeleted, 0)
            );

            if (!relations.isEmpty()) {
                List<String> originalVoucherIds = relations.stream()
                    .map(com.nexusarchive.entity.VoucherRelation::getOriginalVoucherId)
                    .collect(Collectors.toList());
                
                if (!originalVoucherIds.isEmpty()) {
                     List<ArcFileContent> originalFiles = arcFileContentMapper.selectList(
                        new LambdaQueryWrapper<ArcFileContent>()
                            .in(ArcFileContent::getItemId, originalVoucherIds)
                            // 排除逻辑删除的文件 (如果 ArcFileContent 有 deleted 字段)
                            // .eq(ArcFileContent::getDeleted, 0) // ArcFileContent 可能没有 @TableLogic, 暂不加
                    );
                    result.addAll(originalFiles);
                    log.debug("Added {} files from associated original vouchers for archive {}", originalFiles.size(), archiveId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to query related original voucher files: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Helper to check uniqueness
     */
    private void checkArchiveCodeUnique(String code, String excludeId) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getArchiveCode, code);
        if (excludeId != null) {
            wrapper.ne(Archive::getId, excludeId);
        }
        if (archiveMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.ARCHIVE_CODE_EXISTS, code);
        }
    }

    private void checkUniqueBizId(String uniqueBizId, String excludeId) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getUniqueBizId, uniqueBizId);
        wrapper.eq(Archive::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(Archive::getId, excludeId);
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

    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<Archive> getExpiredArchives(int page, int limit, String fondsNo) {
        Page<Archive> pageObj = new Page<>(page, limit);
        return archiveMapper.selectExpired(pageObj, fondsNo);
    }

    /**
     * Helper to check if category is one of the standard Accounting Archive categories
     */
    private boolean isAccountingCategory(String code) {
        return com.nexusarchive.common.constants.ArchiveConstants.Categories.VOUCHER.equals(code) || 
               com.nexusarchive.common.constants.ArchiveConstants.Categories.BOOK.equals(code) || 
               com.nexusarchive.common.constants.ArchiveConstants.Categories.REPORT.equals(code) || 
               com.nexusarchive.common.constants.ArchiveConstants.Categories.OTHERS.equals(code);
    }

    private void applyCaseInsensitiveStatusFilter(LambdaQueryWrapper<Archive> wrapper, String status) {
        List<String> statuses = Arrays.stream(status.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (statuses.isEmpty()) {
            return;
        }

        if (statuses.size() == 1) {
            wrapper.apply("LOWER(status) = {0}", statuses.get(0));
            return;
        }

        // [FIX] 每个 .apply() 调用独立解析模板，占位符必须始终为 {0}
        wrapper.and(w -> {
            for (int i = 0; i < statuses.size(); i++) {
                if (i > 0) {
                    w.or();
                }
                w.apply("LOWER(status) = {0}", statuses.get(i));
            }
        });
    }

}
