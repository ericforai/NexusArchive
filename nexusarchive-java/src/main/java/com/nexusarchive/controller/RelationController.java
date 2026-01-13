// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: RelationController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.relation.ComplianceStatusDto;
import com.nexusarchive.dto.relation.LinkedFileDto;
import com.nexusarchive.dto.relation.RelationEdgeDto;
import com.nexusarchive.dto.relation.RelationGraphDto;
import com.nexusarchive.dto.relation.RelationNodeDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveRelation;
import com.nexusarchive.security.FondsContext;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.IAutoAssociationService;
import com.nexusarchive.service.IArchiveRelationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关联关系与全景视图控制器
 */
@RestController
@RequestMapping("/relations")
@RequiredArgsConstructor
public class RelationController {

    private static final Logger log = LoggerFactory.getLogger(RelationController.class);
    private final IAutoAssociationService autoAssociationService;
    private final ArchiveService archiveService;
    private final IArchiveRelationService archiveRelationService;

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
     * 目前基于档案状态进行 Mock，后续应从四性检测报告中获取
     */
    @GetMapping("/{archiveId}/compliance")
    public Result<ComplianceStatusDto> getComplianceStatus(@PathVariable String archiveId) {
        Archive archive = archiveService.getArchiveById(archiveId);
        if (archive == null) {
            return Result.error(404, "Archive not found");
        }

        // 使用 equalsIgnoreCase 忽略大小写
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

        Archive center = archiveService.getArchiveById(archiveId);
        log.debug("[RelationController] Center archive found: id={}, fonds_no={}, code={}",
            center.getId(), center.getFondsNo(), center.getArchiveCode());

        // 使用档案ID查询关联关系，而不是档案编码
        String centerArchiveId = center.getId();
        List<ArchiveRelation> relations = archiveRelationService.list(new LambdaQueryWrapper<ArchiveRelation>()
                .eq(ArchiveRelation::getSourceId, centerArchiveId)
                .or()
                .eq(ArchiveRelation::getTargetId, centerArchiveId));
        log.debug("[RelationController] Found {} relations for archiveId: {}", relations.size(), centerArchiveId);

        Set<String> relatedIds = relations.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getSourceId(), r.getTargetId()))
                .collect(Collectors.toSet());
        relatedIds.add(centerArchiveId);
        log.debug("[RelationController] Related IDs: {}", relatedIds);

        List<Archive> relatedArchives = archiveService.getArchivesByIds(relatedIds);
        log.debug("[RelationController] getArchivesByIds returned {} archives (after permission filter)", relatedArchives.size());

        Map<String, Archive> archiveMap = relatedArchives.stream()
                .collect(Collectors.toMap(Archive::getId, a -> a));
        archiveMap.put(center.getId(), center);

        List<RelationNodeDto> nodes = archiveMap.values().stream()
                .map(this::toNode)
                .toList();

        List<RelationEdgeDto> edges = relations.stream()
                .map(r -> RelationEdgeDto.builder()
                        .from(r.getSourceId())
                        .to(r.getTargetId())
                        .relationType(r.getRelationType())
                        .description(r.getRelationDesc())
                        .build())
                .toList();

        log.debug("[RelationController] Returning graph with {} nodes and {} edges", nodes.size(), edges.size());

        return Result.success(RelationGraphDto.builder()
                .centerId(center.getId())
                .nodes(nodes)
                .edges(edges)
                .build());
    }

    private RelationNodeDto toNode(Archive archive) {
        return RelationNodeDto.builder()
                .id(archive.getId())
                .code(archive.getArchiveCode())
                .name(archive.getTitle())
                .type(resolveType(archive.getArchiveCode()))
                .amount(formatAmount(archive.getAmount()))
                .date(resolveDate(archive))
                .status(archive.getStatus())
                .build();
    }

    /**
     * 根据档案编码前缀解析文档类型
     * HT- 合同, FP- 发票, JZ- 凭证, HD- 回单, BB- 报表, ZB- 账簿
     */
    private String resolveType(String archiveCode) {
        if (archiveCode == null) return "other";
        String prefix = archiveCode.toUpperCase().substring(0, Math.min(2, archiveCode.length()));
        return switch (prefix) {
            case "HT" -> "contract";   // 合同
            case "FP" -> "invoice";    // 发票
            case "JZ", "PZ" -> "voucher";  // 凭证
            case "HD" -> "receipt";    // 回单/银行回单
            case "BB" -> "report";     // 报表
            case "ZB" -> "ledger";     // 账簿
            default -> "other";
        };
    }

    private String resolveDate(Archive archive) {
        if (archive.getDocDate() != null) {
            return archive.getDocDate().toString();
        }
        if (archive.getCreatedTime() != null) {
            return archive.getCreatedTime().toLocalDate().toString();
        }
        return "";
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return null;
        return "¥ " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
