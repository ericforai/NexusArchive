// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: RelationController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.helper.RelationGraphHelper;
import com.nexusarchive.dto.relation.ComplianceStatusDto;
import com.nexusarchive.dto.relation.LinkedFileDto;
import com.nexusarchive.dto.relation.RelationGraphDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveRelation;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.security.FondsContext;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.IArchiveRelationService;
import com.nexusarchive.service.IAutoAssociationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * 关联关系与全景视图控制器
 */
@Slf4j
@RestController
@RequestMapping("/relations")
@RequiredArgsConstructor
public class RelationController {

    private final IAutoAssociationService autoAssociationService;
    private final ArchiveService archiveService;
    private final ArchiveMapper archiveMapper;
    private final IArchiveRelationService archiveRelationService;
    private final RelationGraphHelper relationGraphHelper;

    /**
     * 获取凭证关联的文件列表
     */
    @GetMapping("/{archiveId}/files")
    public Result<List<LinkedFileDto>> getLinkedFiles(@PathVariable String archiveId) {
        List<LinkedFileDto> files = autoAssociationService.getLinkedFiles(archiveId);
        return Result.success(files);
    }

    /**
     * 获取合规性检测状态
     */
    @GetMapping("/{archiveId}/compliance")
    public Result<ComplianceStatusDto> getComplianceStatus(@PathVariable String archiveId) {
        Archive archive = archiveService.getArchiveById(archiveId);
        if (archive == null) {
            return Result.error(404, "Archive not found");
        }

        boolean isArchived = "ARCHIVED".equalsIgnoreCase(archive.getStatus());
        
        ComplianceStatusDto status = ComplianceStatusDto.builder()
                .passed(isArchived)
                .score(isArchived ? 100 : 0)
                .checkDate(archive.getCreatedTime() != null ? archive.getCreatedTime().toString() : "")
                .details(ComplianceStatusDto.Details.builder()
                        .authenticity(isArchived)
                        .integrity(isArchived)
                        .usability(isArchived)
                        .safety(isArchived)
                        .build())
                .build();

        return Result.success(status);
    }

    /**
     * 获取档案关系图谱
     */
    @GetMapping("/{archiveId}/graph")
    public Result<RelationGraphDto> getRelationGraph(@PathVariable String archiveId) {
        String currentFonds = FondsContext.getCurrentFondsNo();
        log.debug("[RelationController] getRelationGraph called with archiveId: {}, currentFonds: {}", archiveId, currentFonds);

        Archive inputArchive = archiveMapper.selectById(archiveId);
        if (inputArchive == null) {
            inputArchive = archiveMapper.selectOne(new LambdaQueryWrapper<Archive>().eq(Archive::getArchiveCode, archiveId));
        }
        if (inputArchive == null) {
            OriginalVoucher ov = relationGraphHelper.resolveOriginalVoucher(archiveId);
            if (ov != null) {
                String accVoucherId = relationGraphHelper.findRelatedAccountingVoucherId(ov.getId());
                if (accVoucherId != null) {
                    inputArchive = archiveMapper.selectById(accVoucherId);
                    if (inputArchive == null) {
                        inputArchive = archiveMapper.selectOne(new LambdaQueryWrapper<Archive>().eq(Archive::getArchiveCode, accVoucherId));
                    }
                }
            }
        }
        if (inputArchive == null) {
            return Result.error(404, "Archive not found: " + archiveId);
        }
        
        String centerArchiveId;
        String originalQueryId = null;
        boolean autoRedirected = false;
        String redirectMessage = null;
        
        if (relationGraphHelper.isVoucher(inputArchive.getArchiveCode())) {
            centerArchiveId = inputArchive.getId();
        } else {
            originalQueryId = inputArchive.getId();
            centerArchiveId = findRelatedVoucher(inputArchive.getId());
            
            if (centerArchiveId != null) {
                autoRedirected = true;
                redirectMessage = String.format("已自动切换到关联的记账凭证查看完整业务链路（原始查询：%s）", inputArchive.getArchiveCode());
            } else {
                centerArchiveId = inputArchive.getId();
                originalQueryId = null;
            }
        }

        Archive center = archiveMapper.selectById(centerArchiveId);
        if (center == null) {
            center = archiveMapper.selectOne(new LambdaQueryWrapper<Archive>().eq(Archive::getArchiveCode, centerArchiveId));
        }
        if (center == null) {
            return Result.error(404, "Center archive not found: " + centerArchiveId);
        }

        RelationGraphDto graph = relationGraphHelper.buildGraph(center, originalQueryId, autoRedirected, redirectMessage, currentFonds);
        return Result.success(graph);
    }

    @Cacheable(value = "archiveVoucherMapping", key = "'archive:voucher:' + #archiveId", unless = "#result == null")
    private String findRelatedVoucher(String archiveId) {
        List<ArchiveRelation> relations = archiveRelationService.list(
            new LambdaQueryWrapper<ArchiveRelation>()
                .eq(ArchiveRelation::getSourceId, archiveId)
                .eq(ArchiveRelation::getRelationType, "ORIGINAL_VOUCHER")
                .last("LIMIT 5")
        );
        
        for (ArchiveRelation relation : relations) {
            Archive target = archiveMapper.selectById(relation.getTargetId());
            if (target != null && relationGraphHelper.isVoucher(target.getArchiveCode())) {
                return target.getId();
            }
        }
        
        return findVoucherInRelationChainBatch(archiveId);
    }

    private String findVoucherInRelationChainBatch(String archiveId) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(archiveId);
        visited.add(archiveId);
        
        int maxDepth = 3;
        int depth = 0;
        
        while (!queue.isEmpty() && depth <= maxDepth) {
            int levelSize = queue.size();
            depth++;
            List<String> currentLevelIds = new ArrayList<>();
            for (int i = 0; i < levelSize; i++) {
                currentLevelIds.add(queue.poll());
            }
            
            if (!currentLevelIds.isEmpty()) {
                List<Archive> archives = archiveMapper.selectBatchIds(currentLevelIds);
                for (Archive archive : archives) {
                    if (archive != null && relationGraphHelper.isVoucher(archive.getArchiveCode())) {
                        return archive.getId();
                    }
                }
            }
            
            if (depth > maxDepth) break;
            
            List<ArchiveRelation> relations = archiveRelationService.list(
                new LambdaQueryWrapper<ArchiveRelation>()
                    .in(ArchiveRelation::getSourceId, currentLevelIds)
                    .or()
                    .in(ArchiveRelation::getTargetId, currentLevelIds)
            );
            
            Set<String> nextLevelIds = new HashSet<>();
            for (ArchiveRelation relation : relations) {
                if (currentLevelIds.contains(relation.getSourceId()) && !visited.contains(relation.getTargetId())) {
                    nextLevelIds.add(relation.getTargetId());
                    visited.add(relation.getTargetId());
                }
                if (currentLevelIds.contains(relation.getTargetId()) && !visited.contains(relation.getSourceId())) {
                    nextLevelIds.add(relation.getSourceId());
                    visited.add(relation.getSourceId());
                }
            }
            for (String nextId : nextLevelIds) queue.offer(nextId);
        }
        return null;
    }
}
