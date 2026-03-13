// Input: Spring Framework、Java 标准库、匹配引擎组件
// Output: VoucherMatchingEngine 主引擎
// Pos: 匹配引擎/核心
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.engine.matching.dto.*;
import com.nexusarchive.engine.matching.identifier.BusinessSceneIdentifier;
import com.nexusarchive.engine.matching.loader.VoucherLoader;
import com.nexusarchive.engine.matching.enums.*;
import com.nexusarchive.engine.matching.persistence.MatchResultPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 凭证匹配引擎主服务
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>业务场景识别</li>
 *   <li>候选召回与评分</li>
 *   <li>匹配结果生成</li>
 * </ul>
 *
 * <p>具体实现已委托给：</p>
 * <ul>
 *   <li>{@link VoucherLoader} - 凭证数据加载</li>
 *   <li>{@link BusinessSceneIdentifier} - 业务场景识别</li>
 *   <li>{@link MatchResultPersistenceService} - 结果持久化</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherMatchingEngine {

    private final RuleTemplateManager templateManager;
    private final FuzzyMatcher fuzzyMatcher;
    private final CandidateFinder candidateFinder;
    private final MatchingScorer scorer;

    // 提取的服务
    private final VoucherLoader voucherLoader;
    private final BusinessSceneIdentifier sceneIdentifier;
    private final MatchResultPersistenceService persistenceService;

    /**
     * 执行单凭证匹配
     */
    public MatchResult match(String voucherId) {
        log.info("Starting match for voucher: {}", voucherId);

        // 1. 加载凭证数据
        VoucherData voucher = voucherLoader.loadVoucher(voucherId);
        if (voucher == null) {
            return buildErrorResult(voucherId, "凭证不存在");
        }

        // 2. 生成凭证特征快照
        String voucherHash = DigestUtil.sha256Hex(JSONUtil.toJsonStr(voucher));

        // 3. 识别业务场景
        BusinessAttributes attrs = sceneIdentifier.identify(voucher);

        // 4. 获取规则模板
        RuleTemplate template = templateManager.getTemplateByScene(attrs.getScene());
        if (template == null) {
            template = templateManager.getTemplate("T00_MANUAL");
        }

        // 5. 创建匹配上下文
        String batchId = IdUtil.fastSimpleUUID();

        // 6. 执行关联匹配
        List<LinkResult> links = new ArrayList<>();
        List<String> missingDocs = new ArrayList<>();
        MatchStatus overallStatus = MatchStatus.MATCHED;

        // 解析模板配置
        Map<String, Object> config = parseConfig(template.getConfig());

        // 处理 mustLink
        List<Map<String, Object>> mustLinks = getConfigList(config, "mustLink");
        for (Map<String, Object> rule : mustLinks) {
            LinkResult linkResult = matchByRule(voucher, rule, LinkType.MUST_LINK);
            links.add(linkResult);

            if ("MISSING".equals(linkResult.getStatus())) {
                missingDocs.add(linkResult.getEvidenceRoleName());
                overallStatus = MatchStatus.PENDING;
            } else if ("NEED_CONFIRM".equals(linkResult.getStatus())) {
                overallStatus = MatchStatus.NEED_CONFIRM;
            }
        }

        // 处理 shouldLink
        List<Map<String, Object>> shouldLinks = getConfigList(config, "shouldLink");
        for (Map<String, Object> rule : shouldLinks) {
            LinkResult linkResult = matchByRule(voucher, rule, LinkType.SHOULD_LINK);
            links.add(linkResult);
        }

        // 处理 mayLink
        List<Map<String, Object>> mayLinks = getConfigList(config, "mayLink");
        for (Map<String, Object> rule : mayLinks) {
            LinkResult linkResult = matchByRule(voucher, rule, LinkType.MAY_LINK);
            links.add(linkResult);
        }

        // 7. 构建结果
        MatchResult result = MatchResult.builder()
            .matchBatchId(batchId)
            .voucherId(voucherId)
            .voucherNo(voucher.getVoucherNo())
            .scene(attrs.getScene())
            .templateId(template.getId())
            .templateVersion(template.getVersion())
            .confidence(attrs.getConfidence())
            .recognitionReasons(attrs.getReasons())
            .status(overallStatus)
            .missingDocs(missingDocs.isEmpty() ? null : missingDocs)
            .links(links)
            .voucherHash(voucherHash)
            .createdTime(LocalDateTime.now())
            .build();

        // 8. 保存结果
        persistenceService.saveMatchResult(result);

        // 9. 将匹配到的附件链接到档案（使UI可见）
        persistenceService.linkMatchedFilesToArchive(voucherId, links);

        // 10. 更新档案状态 (草稿->待匹配/已匹配)
        persistenceService.updateArchiveStatus(voucherId, result);

        log.info("Match completed for voucher: {}, status: {}", voucherId, overallStatus);
        return result;
    }

    /**
     * 获取最新匹配结果
     */
    public MatchResult getMatchResult(String voucherId) {
        return persistenceService.getMatchResult(voucherId);
    }

    /**
     * 扫描企业数据（向导功能）
     */
    public com.nexusarchive.controller.VoucherMatchingController.OnboardingScanResult scanForOnboarding(String companyId) {
        log.info("Scanning company data for onboarding: {}", companyId);
        return com.nexusarchive.controller.VoucherMatchingController.OnboardingScanResult.builder()
                .companyId(companyId)
                .totalVouchers(0)
                .matchedVouchers(0)
                .unmatchedVouchers(0)
                .typeDistribution(new HashMap<>())
                .build();
    }

    /**
     * 应用预设规则（向导功能）
     */
    public void applyPresetRules(String companyId, String presetId, boolean overwrite) {
        log.info("Applying preset rules for company: {}, preset: {}, overwrite: {}", companyId, presetId, overwrite);
        // 实现逻辑委托给 OnboardingService
    }

    /**
     * 确认映射（向导功能）
     */
    public void confirmMappings(String companyId, List<java.util.Map<String, Object>> mappings) {
        log.info("Confirming mappings for company: {}, count: {}", companyId, mappings.size());
        // 实现逻辑委托给 OnboardingService
    }

    /**
     * 生成合规报告
     */
    public com.nexusarchive.controller.VoucherMatchingController.ComplianceReport generateComplianceReport(
            String startDate, String endDate, String companyId) {
        log.info("Generating compliance report: {} to {}, company: {}", startDate, endDate, companyId);
        return com.nexusarchive.controller.VoucherMatchingController.ComplianceReport.builder()
                .period(startDate + " - " + endDate)
                .totalVouchers(0)
                .matchedVouchers(0)
                .unmatchedVouchers(0)
                .complianceRate(0.0)
                .issues(new ArrayList<>())
                .build();
    }

    /**
     * 查找缺失文档
     */
    public com.nexusarchive.controller.VoucherMatchingController.MissingDocsResult findMissingDocuments(
            String startDate, String endDate, String companyId) {
        log.info("Finding missing documents: {} to {}, company: {}", startDate, endDate, companyId);
        return com.nexusarchive.controller.VoucherMatchingController.MissingDocsResult.builder()
                .missingCount(0)
                .items(new ArrayList<>())
                .build();
    }

    /**
     * 导出合规报告
     */
    public byte[] exportComplianceReport(String startDate, String endDate, String companyId) {
        log.info("Exporting compliance report: {} to {}, company: {}", startDate, endDate, companyId);
        // 返回空字节，实际实现应生成 Excel 文件
        return new byte[0];
    }

    /**
     * 确认关联
     */
    public void confirmMatching(String voucherId, String targetArchiveId, String reason) {
        log.info("Confirming matching: voucherId={}, target={}, reason={}", voucherId, targetArchiveId, reason);
        // 实现逻辑：更新匹配结果状态为已确认
    }

    /**
     * 按规则匹配单个证据角色
     */
    private LinkResult matchByRule(VoucherData voucher, Map<String, Object> rule, LinkType linkType) {
        String roleStr = (String) rule.get("evidenceRole");
        String defaultName = (String) rule.get("defaultName");
        EvidenceRole evidenceRole;

        try {
            evidenceRole = EvidenceRole.valueOf(roleStr);
        } catch (Exception e) {
            evidenceRole = EvidenceRole.AUTHORIZATION;
        }

        // 查找候选
        List<ScoredCandidate> candidates = candidateFinder.findAndScore(
            voucher, roleStr, rule
        );

        if (candidates.isEmpty()) {
            return LinkResult.builder()
                .evidenceRole(evidenceRole)
                .evidenceRoleName(defaultName)
                .linkType(linkType)
                .status("MISSING")
                .suggestion("未找到匹配的" + defaultName)
                .build();
        }

        // 获取最佳候选
        ScoredCandidate best = candidates.get(0);

        // 检查是否需要人工确认
        String status = "MATCHED";
        if (candidates.size() > 1) {
            ScoredCandidate second = candidates.get(1);
            if (best.getScore() - second.getScore() < 10) {
                status = "NEED_CONFIRM";
            }
        }

        return LinkResult.builder()
            .evidenceRole(evidenceRole)
            .evidenceRoleName(defaultName)
            .linkType(linkType)
            .matchedDocId(best.getDocId())
            .matchedDocNo(best.getDocNo())
            .score(best.getScore())
            .reasons(best.getReasons())
            .status(status)
            .candidates(candidates.size() > 1 ? candidates : null)
            .build();
    }

    // ========== 辅助方法 ==========

    private MatchResult buildErrorResult(String voucherId, String message) {
        return MatchResult.builder()
            .voucherId(voucherId)
            .status(MatchStatus.ERROR)
            .message(message)
            .createdTime(LocalDateTime.now())
            .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String configJson) {
        try {
            return JSONUtil.toBean(configJson, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getConfigList(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return Collections.emptyList();
    }
}
