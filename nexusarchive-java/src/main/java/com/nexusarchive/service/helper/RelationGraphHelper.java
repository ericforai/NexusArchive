// Input: 关系图数据
// Output: RelationGraphDto
// Pos: Service Helper 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.relation.LinkedFileDto;
import com.nexusarchive.dto.relation.RelationEdgeDto;
import com.nexusarchive.dto.relation.RelationGraphDto;
import com.nexusarchive.dto.relation.RelationNodeDto;
import com.nexusarchive.entity.*;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.AttachmentService;
import com.nexusarchive.service.IArchiveRelationService;
import com.nexusarchive.service.relation.RelationDirectionResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelationGraphHelper {

    private final ArchiveMapper archiveMapper;
    private final ArchiveService archiveService;
    private final IArchiveRelationService archiveRelationService;
    private final AttachmentService attachmentService;
    private final VoucherRelationMapper voucherRelationMapper;
    private final OriginalVoucherMapper originalVoucherMapper;
    private final OriginalVoucherFileMapper originalVoucherFileMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final RelationDirectionResolver relationDirectionResolver;

    private void fetchRelationsRecursive(String nodeId, Set<ArchiveRelation> allRelations, Set<String> allNodeIds, int depth) {
        if (depth <= 0) return;

        List<ArchiveRelation> currentLevelRelations = archiveRelationService.list(new LambdaQueryWrapper<ArchiveRelation>()
                .eq(ArchiveRelation::getSourceId, nodeId)
                .or()
                .eq(ArchiveRelation::getTargetId, nodeId));

        for (ArchiveRelation rel : currentLevelRelations) {
            if (allRelations.add(rel)) {
                String nextNodeId = rel.getSourceId().equals(nodeId) ? rel.getTargetId() : rel.getSourceId();
                if (!nextNodeId.startsWith("FILE_") && !nextNodeId.startsWith("OV_")) {
                    allNodeIds.add(nextNodeId);
                    fetchRelationsRecursive(nextNodeId, allRelations, allNodeIds, depth - 1);
                }
            }
        }
    }

    public RelationGraphDto buildGraph(Archive inputArchive, String originalQueryId, boolean autoRedirected, String redirectMessage, String currentFonds) {
        String centerArchiveId = inputArchive.getId();
        if (centerArchiveId == null || centerArchiveId.isBlank()) {
            throw new IllegalArgumentException("Archive ID cannot be null or empty");
        }

        Set<ArchiveRelation> allRelations = new HashSet<>();
        Set<String> allNodeIds = new HashSet<>();
        allNodeIds.add(centerArchiveId);

        // 递归获取所有相关关系
        fetchRelationsRecursive(centerArchiveId, allRelations, allNodeIds, 3);

        // 附加附件关系
        List<ArchiveAttachment> attachments = attachmentService.getAttachmentLinks(centerArchiveId);
        for (ArchiveAttachment attachment : attachments) {
            ArchiveRelation ar = new ArchiveRelation();
            ar.setSourceId("FILE_" + attachment.getFileId());
            ar.setTargetId(centerArchiveId);
            ar.setRelationType("ORIGINAL_VOUCHER");
            ar.setRelationDesc(attachment.getRelationDesc() != null ? attachment.getRelationDesc() : "附件");
            allRelations.add(ar);
        }

        // 处理原始凭证关联
        List<VoucherRelation> voucherRelations = voucherRelationMapper.findByAccountingVoucherId(centerArchiveId);
        for (VoucherRelation vr : voucherRelations) {
            ArchiveRelation ar = new ArchiveRelation();
            ar.setSourceId("OV_" + vr.getOriginalVoucherId());
            ar.setTargetId(centerArchiveId);
            ar.setRelationType("ORIGINAL_VOUCHER");
            ar.setRelationDesc("原始凭证");
            allRelations.add(ar);
        }

        List<ArchiveRelation> relations = new ArrayList<>(allRelations);
        Map<String, String> virtualNodeToOriginalVoucherId = new HashMap<>();
        Map<String, String> virtualNodeToFileId = new HashMap<>();
        
        collectIds(relations, allNodeIds, virtualNodeToOriginalVoucherId, virtualNodeToFileId);
        
        if (originalQueryId != null && !originalQueryId.equals(centerArchiveId)) {
            allNodeIds.add(originalQueryId);
        }

        List<Archive> relatedArchives = archiveService.getArchivesByIds(allNodeIds);
        Map<String, Archive> archiveMap = relatedArchives.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Archive::getId, a -> a));

        if (!archiveMap.containsKey(centerArchiveId)) {
            archiveMap.put(centerArchiveId, inputArchive);
        }

        fillMissingNodes(relations, archiveMap, inputArchive, currentFonds);
        createVirtualNodes(virtualNodeToOriginalVoucherId, virtualNodeToFileId, archiveMap, attachments, centerArchiveId);

        return RelationGraphDto.builder()
                .centerId(centerArchiveId)
                .nodes(archiveMap.values().stream().map(this::toNode).collect(Collectors.toList()))
                .edges(relations.stream().map(rel -> RelationEdgeDto.builder()
                        .from(rel.getSourceId())
                        .to(rel.getTargetId())
                        .relationType(rel.getRelationType())
                        .description(rel.getRelationDesc())
                        .build()).collect(Collectors.toList()))
                .autoRedirected(autoRedirected)
                .redirectMessage(redirectMessage)
                .directionalView(relationDirectionResolver.resolve(centerArchiveId, 
                    relations.stream().map(rel -> RelationEdgeDto.builder()
                        .from(rel.getSourceId()).to(rel.getTargetId()).build()).collect(Collectors.toList())))
                .build();
    }

    private void collectIds(List<ArchiveRelation> relations, Set<String> realArchiveIds, 
                            Map<String, String> virtualNodeToOriginalVoucherId, Map<String, String> virtualNodeToFileId) {
        for (ArchiveRelation relation : relations) {
            String sourceId = relation.getSourceId();
            String targetId = relation.getTargetId();
            if (sourceId.startsWith("OV_")) virtualNodeToOriginalVoucherId.put(sourceId, sourceId.substring(3));
            else if (sourceId.startsWith("FILE_")) virtualNodeToFileId.put(sourceId, sourceId.substring(5));
            else realArchiveIds.add(sourceId);
            if (!targetId.startsWith("OV_") && !targetId.startsWith("FILE_")) realArchiveIds.add(targetId);
        }
    }

    private void fillMissingNodes(List<ArchiveRelation> relations, Map<String, Archive> archiveMap, Archive center, String currentFonds) {
        Set<String> missingNodeIds = new HashSet<>();
        for (ArchiveRelation relation : relations) {
            String sourceId = relation.getSourceId();
            String targetId = relation.getTargetId();
            if (sourceId.startsWith("OV_") || sourceId.startsWith("FILE_") || targetId.startsWith("OV_") || targetId.startsWith("FILE_")) continue;
            if (!archiveMap.containsKey(sourceId)) missingNodeIds.add(sourceId);
            if (!archiveMap.containsKey(targetId)) missingNodeIds.add(targetId);
        }
        if (!missingNodeIds.isEmpty()) {
            List<Archive> missingArchives = archiveMapper.selectBatchIds(new ArrayList<>(missingNodeIds));
            for (Archive archive : missingArchives) {
                if (archive == null) continue;
                if (archive.getFondsNo().equals(center.getFondsNo()) || currentFonds == null || currentFonds.isEmpty() || archive.getFondsNo().equals(currentFonds)) {
                    archiveMap.put(archive.getId(), archive);
                }
            }
        }
    }

    private void createVirtualNodes(Map<String, String> virtualNodeToOriginalVoucherId, Map<String, String> virtualNodeToFileId, 
                                   Map<String, Archive> archiveMap, List<ArchiveAttachment> attachments, String centerId) {
        for (Map.Entry<String, String> entry : virtualNodeToOriginalVoucherId.entrySet()) {
            OriginalVoucher ov = originalVoucherMapper.selectById(entry.getValue());
            if (ov != null) {
                Archive va = new Archive();
                va.setId(entry.getKey());
                va.setArchiveCode(ov.getVoucherNo());
                va.setTitle(ov.getSummary() != null ? ov.getSummary() : "原始凭证");
                va.setCategoryCode("AC04");
                va.setAmount(ov.getAmount() != null ? ov.getAmount() : java.math.BigDecimal.ZERO);
                va.setDocDate(ov.getBusinessDate());
                va.setStatus("DRAFT");
                archiveMap.put(entry.getKey(), va);
            }
        }
        for (Map.Entry<String, String> entry : virtualNodeToFileId.entrySet()) {
            String fileId = entry.getValue();
            List<ArcFileContent> files = attachmentService.getAttachmentsByArchive(centerId);
            ArcFileContent file = files.stream().filter(f -> f.getId().equals(fileId)).findFirst().orElse(null);
            if (file != null) {
                Archive va = new Archive();
                va.setId(entry.getKey());
                va.setArchiveCode(fileId.substring(0, Math.min(8, fileId.length())));
                va.setTitle(file.getFileName());
                va.setCategoryCode("AC04");
                va.setStatus("ARCHIVED");
                archiveMap.put(entry.getKey(), va);
            }
        }
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

    private String resolveType(String archiveCode) {
        if (archiveCode == null) return "other";
        String prefix = archiveCode.toUpperCase().substring(0, Math.min(2, archiveCode.length()));
        return switch (prefix) {
            case "HT" -> "contract";
            case "FP" -> "invoice";
            case "JZ", "PZ" -> "voucher";
            case "HD" -> "receipt";
            case "FK" -> "payment";
            case "BX" -> "reimbursement";
            case "SQ" -> "application";
            default -> "other";
        };
    }

    private String resolveDate(Archive archive) {
        if (archive.getDocDate() != null) return archive.getDocDate().toString();
        if (archive.getCreatedTime() != null) return archive.getCreatedTime().toLocalDate().toString();
        return "";
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return null;
        return "¥ " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public OriginalVoucher resolveOriginalVoucher(String idOrVoucherNo) {
        OriginalVoucher ov = originalVoucherMapper.selectById(idOrVoucherNo);
        if (ov != null) return ov;
        ov = originalVoucherMapper.selectOne(new LambdaQueryWrapper<OriginalVoucher>().eq(OriginalVoucher::getVoucherNo, idOrVoucherNo));
        if (ov != null) return ov;
        ArcFileContent content = arcFileContentMapper.selectOne(new LambdaQueryWrapper<ArcFileContent>()
                .eq(ArcFileContent::getArchivalCode, idOrVoucherNo)
                .orderByDesc(ArcFileContent::getCreatedTime)
                .last("LIMIT 1"));
        if (content != null && content.getItemId() != null && !content.getItemId().isBlank()) return originalVoucherMapper.selectById(content.getItemId());
        return null;
    }

    public String findRelatedAccountingVoucherId(String originalVoucherId) {
        List<VoucherRelation> relations = voucherRelationMapper.findByOriginalVoucherId(originalVoucherId);
        if (relations == null || relations.isEmpty()) return null;
        return relations.stream().map(VoucherRelation::getAccountingVoucherId).filter(Objects::nonNull).filter(id -> !id.isBlank()).findFirst().orElse(null);
    }

    public boolean isVoucher(String archiveCode) {
        if (archiveCode == null || archiveCode.length() < 2) return false;
        String prefix = archiveCode.toUpperCase().substring(0, 2);
        return prefix.equals("JZ") || prefix.equals("PZ");
    }
}
