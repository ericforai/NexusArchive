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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 *
 * <p>提供档案关联关系查询和全景视图功能</p>
 */
@Tag(name = "档案关联关系", description = """
    档案关联关系与全景视图接口。

    **功能说明:**
    - 获取凭证关联的文件列表
    - 获取合规性检测状态
    - 获取档案关系图谱

    **关联类型:**
    - 凭证-附件: 凭证与原始附件的关联
    - 凭证-凭证: 业务关联的凭证
    - 凭证-单据: 凭证与业务单据的关联

    **图谱结构:**
    - 节点 (Node): 档案实体
    - 边 (Edge): 关联关系

    **节点类型:**
    - contract: 合同
    - invoice: 发票
    - voucher: 凭证
    - receipt: 回单/银行回单
    - report: 报表
    - ledger: 账簿
    - other: 其他

    **合规性检测:**
    - 真实性 (Authenticity)
    - 完整性 (Integrity)
    - 可用性 (Usability)
    - 安全性 (Safety)

    **使用场景:**
    - 档案关联查看
    - 全景关系图谱
    - 合规性检查结果
    - 附件文件管理

    **权限要求:**
    - 需登录认证
    - 自动按全宗过滤数据
    """)
@SecurityRequirement(name = "Bearer Authentication")
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
    @Operation(
        summary = "获取凭证关联的文件列表",
        description = """
            获取指定档案关联的所有文件列表。

            **路径参数:**
            - archiveId: 档案 ID

            **返回数据包括:**
            - fileId: 文件 ID
            - fileName: 文件名
            - fileType: 文件类型
            - fileSize: 文件大小
            - uploadTime: 上传时间
            - relationType: 关联类型

            **关联类型:**
            - ATTACHMENT: 附件
            - RELATED: 关联文件
            - SOURCE: 原始文件

            **使用场景:**
            - 凭证附件查看
            - 关联文件下载
            - 文件清单导出
            """,
        operationId = "getLinkedFiles",
        tags = {"档案关联关系"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    public Result<List<LinkedFileDto>> getLinkedFiles(
            @Parameter(description = "档案ID", required = true, example = "123456") @PathVariable String archiveId) {
        List<LinkedFileDto> files = autoAssociationService.getLinkedFiles(archiveId);
        return Result.success(files);
    }

    /**
     * 获取合规性检测状态
     * 目前基于档案状态进行 Mock，后续应从四性检测报告中获取
     */
    @GetMapping("/{archiveId}/compliance")
    @Operation(
        summary = "获取合规性检测状态",
        description = """
            获取指定档案的合规性检测状态。

            **路径参数:**
            - archiveId: 档案 ID

            **返回数据包括:**
            - passed: 是否通过检测
            - score: 检测得分（0-100）
            - checkDate: 检测日期
            - details: 详细检测结果
              - authenticity: 真实性
              - integrity: 完整性
              - usability: 可用性
              - safety: 安全性

            **检测标准:**
            - DA/T 92-2022: 四性检测规范
            - 已归档档案默认为通过

            **使用场景:**
            - 合规状态展示
            - 检测结果查看
            - 审计证据获取
            """,
        operationId = "getComplianceStatus",
        tags = {"档案关联关系"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    public Result<ComplianceStatusDto> getComplianceStatus(
            @Parameter(description = "档案ID", required = true, example = "123456") @PathVariable String archiveId) {
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
    @Operation(
        summary = "获取档案关系图谱",
        description = """
            获取以指定档案为中心的关系图谱。

            **路径参数:**
            - archiveId: 中心档案 ID

            **返回数据包括:**
            - centerId: 中心节点 ID
            - nodes: 节点列表
              - id: 节点 ID
              - code: 档案编码
              - name: 档案名称
              - type: 节点类型
              - amount: 金额
              - date: 日期
              - status: 状态
            - edges: 边列表
              - from: 源节点 ID
              - to: 目标节点 ID
              - relationType: 关系类型
              - description: 关系描述

            **关系类型:**
            - REFERENCE: 引用
            - ATTACHMENT: 附件
            - DERIVED: 派生
            - RELATED: 关联

            **节点类型识别:**
            - HT-: 合同
            - FP-: 发票
            - JZ-/PZ-: 凭证
            - HD-: 回单
            - BB-: 报表
            - ZB-: 账簿

            **使用场景:**
            - 全景关系图谱
            - 关联关系分析
            - 数据追溯
            """,
        operationId = "getRelationGraph",
        tags = {"档案关联关系"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    public Result<RelationGraphDto> getRelationGraph(
            @Parameter(description = "档案ID", required = true, example = "123456") @PathVariable String archiveId) {
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
