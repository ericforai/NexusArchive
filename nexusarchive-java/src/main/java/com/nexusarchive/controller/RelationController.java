package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.relation.ComplianceStatusDto;
import com.nexusarchive.dto.relation.LinkedFileDto;
import com.nexusarchive.dto.relation.RelationEdgeDto;
import com.nexusarchive.dto.relation.RelationGraphDto;
import com.nexusarchive.dto.relation.RelationNodeDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveRelation;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.IAutoAssociationService;
import com.nexusarchive.service.IArchiveRelationService;
import lombok.RequiredArgsConstructor;
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
        Archive center = archiveService.getArchiveById(archiveId);

        List<ArchiveRelation> relations = archiveRelationService.list(new QueryWrapper<ArchiveRelation>()
                .eq("source_id", archiveId)
                .or()
                .eq("target_id", archiveId));

        Set<String> relatedIds = relations.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getSourceId(), r.getTargetId()))
                .collect(Collectors.toSet());
        relatedIds.add(archiveId);

        Map<String, Archive> archiveMap = archiveService.getArchivesByIds(relatedIds).stream()
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
                .type(resolveType(archive.getCategoryCode()))
                .amount(formatAmount(archive.getAmount()))
                .date(resolveDate(archive))
                .status(archive.getStatus())
                .build();
    }

    private String resolveType(String categoryCode) {
        if (categoryCode == null) return "other";
        return switch (categoryCode.toUpperCase()) {
            case "AC01" -> "voucher";
            case "AC02" -> "ledger";
            case "AC03" -> "report";
            case "AC04" -> "invoice";
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
