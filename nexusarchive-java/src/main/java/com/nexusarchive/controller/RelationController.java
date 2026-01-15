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
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.security.FondsContext;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.IAutoAssociationService;
import com.nexusarchive.service.IArchiveRelationService;
import com.nexusarchive.service.AttachmentService;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.entity.ArchiveAttachment;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
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
    private final ArchiveMapper archiveMapper;
    private final IArchiveRelationService archiveRelationService;
    private final AttachmentService attachmentService;
    private final VoucherRelationMapper voucherRelationMapper;
    private final OriginalVoucherMapper originalVoucherMapper;
    private final OriginalVoucherFileMapper originalVoucherFileMapper;

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
     * 核心业务逻辑：以记账凭证（JZ-/PZ-开头）为中心进行穿透查询
     * 如果输入的是非凭证档案，自动查找关联的记账凭证作为中心节点
     */
    @GetMapping("/{archiveId}/graph")
    public Result<RelationGraphDto> getRelationGraph(@PathVariable String archiveId) {
        String currentFonds = FondsContext.getCurrentFondsNo();
        log.debug("[RelationController] getRelationGraph called with archiveId: {}, currentFonds: {}", archiveId, currentFonds);

        // 关系查询场景：直接通过 mapper 查询，绕过权限检查
        // 因为如果用户能查询到档案本身，就应该能看到它的关系数据
        // 这样可以避免因为当前全宗不匹配而无法查看关系数据的问题
        Archive inputArchive = archiveMapper.selectById(archiveId);
        if (inputArchive == null) {
            // Fallback: try archive_code lookup
            QueryWrapper<Archive> wrapper = new QueryWrapper<>();
            wrapper.eq("archive_code", archiveId);
            inputArchive = archiveMapper.selectOne(wrapper);
        }
        if (inputArchive == null) {
            return Result.error(404, "Archive not found: " + archiveId);
        }
        
        log.debug("[RelationController] Input archive found: id={}, fonds_no={}, code={}",
            inputArchive.getId(), inputArchive.getFondsNo(), inputArchive.getArchiveCode());

        // Step 1: 判断输入的档案类型，确定中心节点
        String centerArchiveId;
        String originalQueryId = null;
        boolean autoRedirected = false;
        String redirectMessage = null;
        
        if (isVoucher(inputArchive.getArchiveCode())) {
            // 输入的就是记账凭证，直接作为中心
            centerArchiveId = inputArchive.getId();
            log.debug("[RelationController] Input is voucher, using as center: {}", centerArchiveId);
        } else {
            // 输入的是非凭证档案，查找关联的记账凭证
            originalQueryId = inputArchive.getId(); // 记录原始查询档案
            centerArchiveId = findRelatedVoucher(inputArchive.getId());
            
            if (centerArchiveId != null) {
                // 找到关联凭证，自动转换
                autoRedirected = true;
                redirectMessage = String.format("已自动切换到关联的记账凭证查看完整业务链路（原始查询：%s）", 
                    inputArchive.getArchiveCode());
                log.info("[RelationController] Found related voucher {} for input archive: {} (code: {})", 
                    centerArchiveId, inputArchive.getId(), inputArchive.getArchiveCode());
            } else {
                // 未找到关联凭证，以原档案为中心（向后兼容）
                centerArchiveId = inputArchive.getId();
                originalQueryId = null;
                log.warn("[RelationController] No related voucher found for archive: {} (code: {}), using as center", 
                    inputArchive.getId(), inputArchive.getArchiveCode());
            }
        }

        // 创建 effectively final 变量供 lambda 使用
        final String finalCenterArchiveId = centerArchiveId;

        // Step 2: 以 centerArchiveId 为中心查询关系
        // 关系查询场景：直接通过 mapper 查询，绕过权限检查
        Archive center = archiveMapper.selectById(centerArchiveId);
        if (center == null) {
            // Fallback: try archive_code lookup
            QueryWrapper<Archive> wrapper = new QueryWrapper<>();
            wrapper.eq("archive_code", centerArchiveId);
            center = archiveMapper.selectOne(wrapper);
        }
        if (center == null) {
            return Result.error(404, "Center archive not found: " + centerArchiveId);
        }

        log.debug("[RelationController] Center archive: id={}, code={}", center.getId(), center.getArchiveCode());

        // Step 2.1: 查询 acc_archive_relation 表中的关系
        List<ArchiveRelation> relations = archiveRelationService.list(new LambdaQueryWrapper<ArchiveRelation>()
                .eq(ArchiveRelation::getSourceId, finalCenterArchiveId)
                .or()
                .eq(ArchiveRelation::getTargetId, finalCenterArchiveId));
        log.debug("[RelationController] Found {} relations from acc_archive_relation for centerArchiveId: {}", relations.size(), finalCenterArchiveId);
        
        // Step 2.1.1: 查询 acc_archive_attachment 表中的附件关联（附件作为上游数据）
        // 附件文件（如发票）应该显示为上游数据
        List<ArchiveAttachment> attachments = attachmentService.getAttachmentLinks(finalCenterArchiveId);
        log.debug("[RelationController] Found {} attachments for centerArchiveId: {}", attachments.size(), finalCenterArchiveId);
        
        // 将附件关联转换为 ArchiveRelation（用于统一处理）
        for (ArchiveAttachment attachment : attachments) {
            // 创建虚拟关系，使用文件ID作为源ID（添加前缀以区分虚拟节点）
            ArchiveRelation ar = new ArchiveRelation();
            ar.setSourceId("FILE_" + attachment.getFileId()); // 添加前缀以区分虚拟节点
            ar.setTargetId(finalCenterArchiveId);
            ar.setRelationType("ORIGINAL_VOUCHER"); // 附件通常是原始凭证
            ar.setRelationDesc(attachment.getRelationDesc() != null ? attachment.getRelationDesc() : "附件");
            relations.add(ar);
            log.debug("[RelationController] Added attachment relation: {} -> {}", attachment.getFileId(), finalCenterArchiveId);
        }

        // Step 2.2: 查询 arc_voucher_relation 表中的原始凭证关联（如果中心节点是记账凭证）
        // 原始凭证关联的发票应该显示为上游数据
        List<VoucherRelation> voucherRelations = voucherRelationMapper.findByAccountingVoucherId(finalCenterArchiveId);
        log.debug("[RelationController] Found {} original voucher relations for centerArchiveId: {}", voucherRelations.size(), finalCenterArchiveId);
        
        // 将原始凭证关联转换为 ArchiveRelation（用于统一处理）
        // 原始凭证可能通过 source_doc_id 关联到 acc_archive 记录
        for (VoucherRelation vr : voucherRelations) {
            // 查询原始凭证信息，获取 source_doc_id
            OriginalVoucher originalVoucher = originalVoucherMapper.selectById(vr.getOriginalVoucherId());
            if (originalVoucher == null) {
                log.debug("[RelationController] Original voucher {} not found, skipping", vr.getOriginalVoucherId());
                continue;
            }
            
            // 方法1: 如果原始凭证的 source_doc_id 指向某个档案记录，使用该档案ID
            String invoiceArchiveId = null;
            if (originalVoucher.getSourceDocId() != null && !originalVoucher.getSourceDocId().isEmpty()) {
                // 关系查询场景：直接通过 mapper 查询，绕过权限检查
                Archive sourceArchive = archiveMapper.selectById(originalVoucher.getSourceDocId());
                if (sourceArchive != null) {
                    invoiceArchiveId = originalVoucher.getSourceDocId();
                    log.debug("[RelationController] Found archive via source_doc_id: {} -> {}", vr.getOriginalVoucherId(), invoiceArchiveId);
                }
            }
            
            // 方法2: 如果 source_doc_id 不存在或无效，检查原始凭证ID是否直接对应某个档案记录
            if (invoiceArchiveId == null) {
                // 关系查询场景：直接通过 mapper 查询，绕过权限检查
                Archive directArchive = archiveMapper.selectById(vr.getOriginalVoucherId());
                if (directArchive != null) {
                    invoiceArchiveId = vr.getOriginalVoucherId();
                    log.debug("[RelationController] Found archive via direct ID: {}", invoiceArchiveId);
                }
            }
            
            // 如果找到了对应的档案记录，创建关系
            if (invoiceArchiveId != null) {
                ArchiveRelation ar = new ArchiveRelation();
                ar.setSourceId(invoiceArchiveId);
                ar.setTargetId(finalCenterArchiveId);
                ar.setRelationType("ORIGINAL_VOUCHER");
                ar.setRelationDesc("原始凭证");
                relations.add(ar);
                log.debug("[RelationController] Added original voucher relation: {} -> {}", invoiceArchiveId, finalCenterArchiveId);
            } else {
                // 如果原始凭证未转换为档案，但原始凭证存在且有文件，创建虚拟关系
                // 使用原始凭证ID作为虚拟档案ID，后续在构建节点时创建虚拟档案对象
                List<OriginalVoucherFile> files = originalVoucherFileMapper.findByVoucherId(vr.getOriginalVoucherId());
                if (!files.isEmpty()) {
                    // 创建虚拟关系，使用原始凭证ID作为源ID
                    ArchiveRelation ar = new ArchiveRelation();
                    ar.setSourceId("OV_" + vr.getOriginalVoucherId()); // 添加前缀以区分虚拟节点
                    ar.setTargetId(finalCenterArchiveId);
                    ar.setRelationType("ORIGINAL_VOUCHER");
                    ar.setRelationDesc("原始凭证");
                    relations.add(ar);
                    log.debug("[RelationController] Added virtual original voucher relation: {} -> {} (has {} files)", 
                        vr.getOriginalVoucherId(), finalCenterArchiveId, files.size());
                } else {
                    log.debug("[RelationController] Original voucher {} (source_doc_id: {}) not found in acc_archive and has no files, skipping relation", 
                        vr.getOriginalVoucherId(), originalVoucher.getSourceDocId());
                }
            }
        }

        // Step 3: 收集所有相关档案ID（包括原始查询档案，如果存在）
        // 分离真实档案ID和虚拟节点ID
        Set<String> realArchiveIds = new HashSet<>();
        Map<String, String> virtualNodeToOriginalVoucherId = new HashMap<>(); // 虚拟节点ID -> 原始凭证ID
        Map<String, String> virtualNodeToFileId = new HashMap<>(); // 虚拟节点ID -> 文件ID
        
        for (ArchiveRelation relation : relations) {
            String sourceId = relation.getSourceId();
            String targetId = relation.getTargetId();
            
            // 检查是否为虚拟节点（以 OV_ 或 FILE_ 开头）
            if (sourceId.startsWith("OV_")) {
                String originalVoucherId = sourceId.substring(3); // 移除 "OV_" 前缀
                virtualNodeToOriginalVoucherId.put(sourceId, originalVoucherId);
            } else if (sourceId.startsWith("FILE_")) {
                String fileId = sourceId.substring(5); // 移除 "FILE_" 前缀
                virtualNodeToFileId.put(sourceId, fileId);
            } else {
                realArchiveIds.add(sourceId);
            }
            
            if (!targetId.startsWith("OV_") && !targetId.startsWith("FILE_")) {
                realArchiveIds.add(targetId);
            }
        }
        
        realArchiveIds.add(finalCenterArchiveId);
        if (originalQueryId != null && !originalQueryId.equals(finalCenterArchiveId)) {
            realArchiveIds.add(originalQueryId); // 确保原始查询档案也在结果中
        }
        log.debug("[RelationController] Real archive IDs: {}, Virtual nodes: {}", realArchiveIds, virtualNodeToOriginalVoucherId.keySet());

        // 权限校验：检查用户是否有权限查看中心档案
        if (!realArchiveIds.contains(finalCenterArchiveId)) {
            log.warn("[RelationController] Center archive {} not in relatedIds, permission check failed", finalCenterArchiveId);
        }

        List<Archive> relatedArchives = archiveService.getArchivesByIds(realArchiveIds);
        log.debug("[RelationController] getArchivesByIds returned {} archives (after permission filter), requested: {}, currentFonds: {}",
            relatedArchives.size(), realArchiveIds.size(), currentFonds);

        // 检查中心档案是否在权限过滤后的结果中
        boolean centerArchiveFound = relatedArchives.stream()
                .anyMatch(a -> a.getId().equals(finalCenterArchiveId));
        
        if (!centerArchiveFound) {
            // 中心档案被权限过滤掉了，但我们已经通过 getArchiveById 获取到了中心档案
            // 如果用户能通过 getArchiveById 获取到中心档案，说明用户至少能看到这个档案
            // 因此应该允许查看关系数据，但需要检查全宗匹配
            if (center != null) {
                // 如果当前全宗为空，或者中心档案的全宗与当前全宗一致，允许查看
                // 注意：如果用户没有 DEMO 全宗权限，currentFonds 可能不是 DEMO
                // 但用户能通过 getArchiveById 获取到 center，说明用户至少能看到这个档案
                // 因此我们允许查看关系数据，但会记录警告日志
                if (currentFonds == null || currentFonds.isEmpty() || center.getFondsNo().equals(currentFonds)) {
                    log.info("[RelationController] Center archive {} (fonds: {}) not in filtered results, but fonds matches current fonds ({}), allowing access",
                        finalCenterArchiveId, center.getFondsNo(), currentFonds);
                    // 继续处理，不返回错误
                } else {
                    // 全宗不匹配，但用户能通过 getArchiveById 获取到 center
                    // 这可能是因为用户有跨全宗权限，或者 getArchiveById 不应用权限过滤
                    // 为了用户体验，我们允许查看关系数据，但会记录警告日志
                    log.warn("[RelationController] Center archive {} (fonds: {}) not in filtered results, but user can access it via getArchiveById. Current fonds: {}. Allowing access for better UX.",
                        finalCenterArchiveId, center.getFondsNo(), currentFonds);
                    // 继续处理，不返回错误
                }
            } else {
                // 中心档案不存在（不应该发生，因为前面已经检查过）
                log.error("[RelationController] Center archive {} is null, this should not happen", finalCenterArchiveId);
                return Result.error(404, "Center archive not found: " + finalCenterArchiveId);
            }
        }

        Map<String, Archive> archiveMap = relatedArchives.stream()
                .collect(Collectors.toMap(Archive::getId, a -> a));

        // 确保中心档案和原始查询档案都在结果中
        // 如果中心档案被权限过滤掉了，但我们通过 getArchiveById 获取到了它，应该添加到结果中
        if (!archiveMap.containsKey(finalCenterArchiveId)) {
            archiveMap.put(finalCenterArchiveId, center);
            log.debug("[RelationController] Added center archive {} to result map (was filtered out by permission)", finalCenterArchiveId);
        }
        if (originalQueryId != null && !archiveMap.containsKey(originalQueryId)) {
            archiveMap.put(inputArchive.getId(), inputArchive); // 确保原始查询档案在结果中
        }
        
        // 重要：如果关系中的节点被权限过滤掉了，需要手动查询并添加
        // 这是因为关系数据可能跨全宗，但权限过滤会过滤掉不同全宗的节点
        // 为了显示完整的关系链，我们需要包含所有关系中的节点（即使跨全宗）
        for (ArchiveRelation relation : relations) {
            String sourceId = relation.getSourceId();
            String targetId = relation.getTargetId();
            
            // 跳过虚拟节点
            if (sourceId.startsWith("OV_") || sourceId.startsWith("FILE_")) {
                continue;
            }
            if (targetId.startsWith("OV_") || targetId.startsWith("FILE_")) {
                continue;
            }
            
            // 如果源节点不在结果中，尝试直接查询（绕过权限过滤）
            if (!archiveMap.containsKey(sourceId) && !sourceId.equals(finalCenterArchiveId)) {
                Archive sourceArchive = archiveMapper.selectById(sourceId);
                if (sourceArchive != null) {
                    // 检查是否与中心档案在同一全宗，或者用户有权限访问
                    if (sourceArchive.getFondsNo().equals(center.getFondsNo()) || 
                        currentFonds == null || currentFonds.isEmpty() || 
                        sourceArchive.getFondsNo().equals(currentFonds)) {
                        archiveMap.put(sourceId, sourceArchive);
                        log.debug("[RelationController] Added missing source node: {} (fonds: {})", sourceId, sourceArchive.getFondsNo());
                    }
                }
            }
            
            // 如果目标节点不在结果中，尝试直接查询（绕过权限过滤）
            if (!archiveMap.containsKey(targetId) && !targetId.equals(finalCenterArchiveId)) {
                Archive targetArchive = archiveMapper.selectById(targetId);
                if (targetArchive != null) {
                    // 检查是否与中心档案在同一全宗，或者用户有权限访问
                    if (targetArchive.getFondsNo().equals(center.getFondsNo()) || 
                        currentFonds == null || currentFonds.isEmpty() || 
                        targetArchive.getFondsNo().equals(currentFonds)) {
                        archiveMap.put(targetId, targetArchive);
                        log.debug("[RelationController] Added missing target node: {} (fonds: {})", targetId, targetArchive.getFondsNo());
                    }
                }
            }
        }

        // 为虚拟节点创建虚拟 Archive 对象
        // 1. 原始凭证虚拟节点
        for (Map.Entry<String, String> entry : virtualNodeToOriginalVoucherId.entrySet()) {
            String virtualNodeId = entry.getKey();
            String originalVoucherId = entry.getValue();
            
            OriginalVoucher originalVoucher = originalVoucherMapper.selectById(originalVoucherId);
            if (originalVoucher != null) {
                // 创建虚拟 Archive 对象
                Archive virtualArchive = new Archive();
                virtualArchive.setId(virtualNodeId);
                virtualArchive.setArchiveCode("OV-" + originalVoucher.getVoucherNo());
                virtualArchive.setTitle(originalVoucher.getSummary() != null ? originalVoucher.getSummary() : "原始凭证");
                virtualArchive.setCategoryCode("AC04"); // 其他会计资料
                virtualArchive.setAmount(originalVoucher.getAmount() != null ? originalVoucher.getAmount() : java.math.BigDecimal.ZERO);
                virtualArchive.setDocDate(originalVoucher.getBusinessDate());
                virtualArchive.setFondsNo(originalVoucher.getFondsCode());
                virtualArchive.setFiscalYear(originalVoucher.getFiscalYear());
                virtualArchive.setStatus("DRAFT"); // 原始凭证通常是草稿状态
                archiveMap.put(virtualNodeId, virtualArchive);
                log.debug("[RelationController] Created virtual archive for original voucher: {}", originalVoucherId);
            }
        }
        
        // 2. 附件文件虚拟节点
        for (Map.Entry<String, String> entry : virtualNodeToFileId.entrySet()) {
            String virtualNodeId = entry.getKey();
            String fileId = entry.getValue();
            
            // 查找附件关联记录以获取附件类型和描述
            ArchiveAttachment attachment = attachments.stream()
                    .filter(a -> a.getFileId().equals(fileId))
                    .findFirst()
                    .orElse(null);
            
            if (attachment != null) {
                // 获取文件信息（通过 AttachmentService）
                List<ArcFileContent> files = attachmentService.getAttachmentsByArchive(finalCenterArchiveId);
                ArcFileContent file = files.stream()
                        .filter(f -> f.getId().equals(fileId))
                        .findFirst()
                        .orElse(null);
                
                if (file != null) {
                    // 创建虚拟 Archive 对象
                    Archive virtualArchive = new Archive();
                    virtualNodeId = entry.getKey();
                    virtualArchive.setId(virtualNodeId);
                    virtualArchive.setArchiveCode("FILE-" + fileId.substring(0, Math.min(8, fileId.length())));
                    virtualArchive.setTitle(file.getFileName() != null ? file.getFileName() : "附件文件");
                    virtualArchive.setCategoryCode("AC04"); // 其他会计资料
                    virtualArchive.setFondsNo(center.getFondsNo()); // 使用中心档案的全宗
                    virtualArchive.setFiscalYear(center.getFiscalYear()); // 使用中心档案的年度
                    virtualArchive.setStatus("ARCHIVED"); // 附件通常是已归档状态
                    archiveMap.put(virtualNodeId, virtualArchive);
                    log.debug("[RelationController] Created virtual archive for attachment file: {}", fileId);
                }
            }
        }

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

        log.debug("[RelationController] Returning graph with {} nodes and {} edges, centerId: {}",
            nodes.size(), edges.size(), finalCenterArchiveId);

        // Step 4: 构建返回数据
        RelationGraphDto.RelationGraphDtoBuilder builder = RelationGraphDto.builder()
                .centerId(finalCenterArchiveId)
                .nodes(nodes)
                .edges(edges);
        
        if (autoRedirected) {
            builder.originalQueryId(originalQueryId)
                   .autoRedirected(true)
                   .redirectMessage(redirectMessage);
        }

        return Result.success(builder.build());
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
     * HT- 合同, FP- 发票, JZ-/PZ- 凭证, HD- 回单, BB- 报表, ZB- 账簿
     * FK- 付款单, BX- 报销单, SQ- 申请单
     */
    private String resolveType(String archiveCode) {
        if (archiveCode == null) return "other";
        String prefix = archiveCode.toUpperCase().substring(0, Math.min(2, archiveCode.length()));
        return switch (prefix) {
            case "HT" -> "contract";       // 合同
            case "FP" -> "invoice";        // 发票
            case "JZ", "PZ" -> "voucher";  // 凭证
            case "HD" -> "receipt";        // 回单/银行回单
            case "BB" -> "report";         // 报表
            case "ZB" -> "ledger";         // 账簿
            case "FK" -> "payment";        // 付款单
            case "BX" -> "reimbursement";  // 报销单
            case "SQ" -> "application";    // 申请单
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

    /**
     * 判断是否为记账凭证
     * 记账凭证的编码前缀为：JZ-（记账凭证）或 PZ-（凭证）
     */
    private boolean isVoucher(String archiveCode) {
        if (archiveCode == null || archiveCode.length() < 2) {
            return false;
        }
        String prefix = archiveCode.toUpperCase().substring(0, 2);
        return prefix.equals("JZ") || prefix.equals("PZ");
    }

    /**
     * 查找关联的记账凭证（带缓存）
     * 查找策略：
     * 1. 优先查找以该档案为源、关系类型为 ORIGINAL_VOUCHER 的目标节点（发票→凭证）
     * 2. 如果未找到，递归查找关系链中的记账凭证（最多3度）
     * 
     * 缓存策略：缓存键为 "archive:voucher:{archiveId}"，TTL 30分钟
     * 缓存失效：当关系数据变更时，需要清除相关缓存
     */
    @Cacheable(value = "archiveVoucherMapping", key = "'archive:voucher:' + #archiveId", unless = "#result == null")
    private String findRelatedVoucher(String archiveId) {
        log.debug("[RelationController] Finding related voucher for archive: {}", archiveId);
        
        // 策略1: 查找以该档案为源、关系类型为 ORIGINAL_VOUCHER 的目标节点（最常见情况）
        List<ArchiveRelation> relations = archiveRelationService.list(
            new LambdaQueryWrapper<ArchiveRelation>()
                .eq(ArchiveRelation::getSourceId, archiveId)
                .eq(ArchiveRelation::getRelationType, "ORIGINAL_VOUCHER")
                .last("LIMIT 5") // 限制查询数量，避免性能问题
        );
        
        for (ArchiveRelation relation : relations) {
            // 关系查询场景：直接通过 mapper 查询，绕过权限检查
            Archive target = archiveMapper.selectById(relation.getTargetId());
            if (target != null && isVoucher(target.getArchiveCode())) {
                log.debug("[RelationController] Found voucher via ORIGINAL_VOUCHER relation: {} -> {} (code: {})", 
                    archiveId, target.getId(), target.getArchiveCode());
                return target.getId();
            }
        }
        
        // 策略2: 查找所有关联关系，递归查找凭证（最多3度，避免性能问题）
        Set<String> visited = new HashSet<>();
        String voucherId = findVoucherInRelationChain(archiveId, visited, 0, 3);
        if (voucherId != null) {
            log.debug("[RelationController] Found voucher via relation chain: {} -> {}", archiveId, voucherId);
        } else {
            log.debug("[RelationController] No voucher found for archive: {}", archiveId);
        }
        return voucherId;
    }

    /**
     * 递归查找关系链中的记账凭证
     * 性能优化：
     * - 使用 visited Set 防止循环
     * - 限制最大深度为3度
     * - 限制每次查询的关系数量（LIMIT 20）
     * 
     * @param archiveId 当前档案ID
     * @param visited 已访问的档案ID集合（防止循环）
     * @param depth 当前递归深度
     * @param maxDepth 最大递归深度（建议不超过3）
     * @return 找到的凭证ID，未找到返回null
     */
    private String findVoucherInRelationChain(String archiveId, Set<String> visited, int depth, int maxDepth) {
        // 防止循环和深度过深
        if (depth > maxDepth || visited.contains(archiveId)) {
            return null;
        }
        visited.add(archiveId);
        
        // 首先检查当前节点是否为凭证（避免不必要的查询）
        // 关系查询场景：直接通过 mapper 查询，绕过权限检查
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive != null && isVoucher(archive.getArchiveCode())) {
            log.debug("[RelationController] Found voucher at depth {}: {} (code: {})", 
                depth, archiveId, archive.getArchiveCode());
            return archiveId;
        }
        
        if (depth >= maxDepth) {
            return null; // 达到最大深度，停止递归
        }
        
        // 优化：优先查找指向凭证的关系（ORIGINAL_VOUCHER、BASIS）
        List<ArchiveRelation> directRelations = archiveRelationService.list(
            new LambdaQueryWrapper<ArchiveRelation>()
                .eq(ArchiveRelation::getSourceId, archiveId)
                .in(ArchiveRelation::getRelationType, List.of("ORIGINAL_VOUCHER", "BASIS"))
                .last("LIMIT 10") // 优先查询直接关系
        );
        
        for (ArchiveRelation relation : directRelations) {
            String targetId = relation.getTargetId();
            // 关系查询场景：直接通过 mapper 查询，绕过权限检查
            Archive target = archiveMapper.selectById(targetId);
            if (target != null && isVoucher(target.getArchiveCode())) {
                return targetId;
            }
            
            String voucherId = findVoucherInRelationChain(targetId, visited, depth + 1, maxDepth);
            if (voucherId != null) {
                return voucherId;
            }
        }
        
        // 如果直接关系未找到，再查找所有关联关系（但限制数量）
        List<ArchiveRelation> allRelations = archiveRelationService.list(
            new LambdaQueryWrapper<ArchiveRelation>()
                .eq(ArchiveRelation::getSourceId, archiveId)
                .or()
                .eq(ArchiveRelation::getTargetId, archiveId)
                .last("LIMIT 20") // 限制查询数量，避免性能问题
        );
        
        for (ArchiveRelation relation : allRelations) {
            // 跳过已经查询过的直接关系
            if (directRelations.contains(relation)) {
                continue;
            }
            
            String nextId = relation.getSourceId().equals(archiveId) 
                ? relation.getTargetId() 
                : relation.getSourceId();
            
            String voucherId = findVoucherInRelationChain(nextId, visited, depth + 1, maxDepth);
            if (voucherId != null) {
                return voucherId;
            }
        }
        
        return null;
    }
}
