// Input: Spring Framework、Java 标准库、匹配引擎组件
// Output: VoucherMatchingEngine 主引擎
// Pos: 匹配引擎/核心
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.engine.matching.dto.*;
import com.nexusarchive.engine.matching.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 凭证匹配引擎主服务
 * 
 * 核心功能：
 * 1. 业务场景识别
 * 2. 候选召回与评分
 * 3. 匹配结果生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherMatchingEngine {
    
    private final JdbcTemplate jdbcTemplate;
    private final RuleTemplateManager templateManager;
    private final FuzzyMatcher fuzzyMatcher;
    private final CandidateFinder candidateFinder;
    private final MatchingScorer scorer;
    
    /**
     * 执行单凭证匹配
     */
    public MatchResult match(String voucherId) {
        log.info("Starting match for voucher: {}", voucherId);
        
        // 1. 加载凭证数据
        VoucherData voucher = loadVoucher(voucherId);
        if (voucher == null) {
            return buildErrorResult(voucherId, "凭证不存在");
        }
        
        // 2. 生成凭证特征快照
        String voucherHash = DigestUtil.sha256Hex(JSONUtil.toJsonStr(voucher));
        
        // 3. 识别业务场景
        BusinessAttributes attrs = identifyBusinessScene(voucher);
        
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
        saveMatchResult(result);
        
        // 9. 将匹配到的附件链接到档案（使UI可见）
        linkMatchedFilesToArchive(voucherId, links);
        
        // 10. 更新档案状态 (草稿->待匹配/已匹配)
        updateArchiveStatus(voucherId, result);
        
        log.info("Match completed for voucher: {}, status: {}", voucherId, overallStatus);
        return result;
    }
    
    /**
     * 识别业务场景
     */
    private BusinessAttributes identifyBusinessScene(VoucherData voucher) {
        List<String> reasons = new ArrayList<>();
        BusinessScene scene = BusinessScene.UNKNOWN;
        BigDecimal confidence = BigDecimal.ZERO;
        
        // 基于科目角色组合识别
        Set<AccountRole> debitRoles = voucher.getDebitRoles();
        Set<AccountRole> creditRoles = voucher.getCreditRoles();
        String summary = voucher.getSummary() != null ? voucher.getSummary() : "";
        
        // 付款：贷方银行/现金，借方应付/费用
        if (creditRoles.contains(AccountRole.BANK) || creditRoles.contains(AccountRole.CASH)) {
            if (debitRoles.contains(AccountRole.PAYABLE)) {
                scene = BusinessScene.PAYMENT;
                confidence = new BigDecimal("0.9");
                reasons.add("贷方银行存款 + 借方应付账款");
            } else if (debitRoles.contains(AccountRole.EXPENSE)) {
                if (summary.contains("报销")) {
                    scene = BusinessScene.EXPENSE;
                    confidence = new BigDecimal("0.95");
                    reasons.add("贷方银行存款 + 借方费用 + 摘要含'报销'");
                } else {
                    scene = BusinessScene.PAYMENT;
                    confidence = new BigDecimal("0.85");
                    reasons.add("贷方银行存款 + 借方费用");
                }
            } else if (debitRoles.contains(AccountRole.SALARY)) {
                scene = BusinessScene.SALARY_PAYMENT;
                confidence = new BigDecimal("0.95");
                reasons.add("贷方银行存款 + 借方应付职工薪酬");
            } else if (debitRoles.contains(AccountRole.TAX)) {
                scene = BusinessScene.TAX_PAYMENT;
                confidence = new BigDecimal("0.95");
                reasons.add("贷方银行存款 + 借方应交税费");
            }
        }
        
        // 收款：借方银行/现金，贷方应收
        if (debitRoles.contains(AccountRole.BANK) || debitRoles.contains(AccountRole.CASH)) {
            if (creditRoles.contains(AccountRole.RECEIVABLE)) {
                scene = BusinessScene.RECEIPT;
                confidence = new BigDecimal("0.9");
                reasons.add("借方银行存款 + 贷方应收账款");
            } else if (creditRoles.contains(AccountRole.REVENUE)) {
                scene = BusinessScene.SALES_OUT;
                confidence = new BigDecimal("0.85");
                reasons.add("借方银行存款 + 贷方收入");
            }
        }
        
        // 关键词增强
        if (summary.contains("付款") || summary.contains("支付")) {
            if (scene == BusinessScene.UNKNOWN) {
                scene = BusinessScene.PAYMENT;
                confidence = new BigDecimal("0.7");
            }
            reasons.add("摘要含'付款/支付'");
        }
        
        return BusinessAttributes.builder()
            .scene(scene)
            .templateId(scene.getDefaultTemplateId())
            .confidence(confidence)
            .reasons(reasons)
            .build();
    }

    /**
     * 获取最新匹配结果
     */
    public MatchResult getMatchResult(String voucherId) {
        String sql = """
            SELECT * FROM voucher_match_result 
            WHERE voucher_id = ? 
            ORDER BY created_time DESC 
            LIMIT 1
            """;
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, voucherId);
            if (rows.isEmpty()) {
                return null;
            }
            return mapRowToMatchResult(rows.get(0));
        } catch (Exception e) {
            log.warn("Failed to get match result for {}: {}", voucherId, e.getMessage());
            return null;
        }
    }

    private MatchResult mapRowToMatchResult(Map<String, Object> row) {
        String detailsJson = getString(row, "match_details");
        List<LinkResult> links = Collections.emptyList();
        if (detailsJson != null) {
            links = JSONUtil.toList(detailsJson, LinkResult.class);
        }
        
        return MatchResult.builder()
            .voucherId(getString(row, "voucher_id"))
            .voucherHash(getString(row, "voucher_hash"))
            .templateId(getString(row, "template_id"))
            .templateVersion(getString(row, "template_version"))
            .scene(BusinessScene.valueOf(getString(row, "scene")))
            .status(MatchStatus.valueOf(getString(row, "status")))
            .links(links)
            .createdTime(row.get("created_time") instanceof java.sql.Timestamp ? 
                ((java.sql.Timestamp) row.get("created_time")).toLocalDateTime() : LocalDateTime.now())
            .build();
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
    
    private VoucherData loadVoucher(String voucherId) {
        try {
            // 从 Archive 表加载凭证数据
            String sql = """
                SELECT id, archive_code, title, summary, amount, doc_date, 
                       custom_metadata::text as custom_metadata_json
                FROM acc_archive 
                WHERE id::text = ? OR archive_code = ?
                LIMIT 1
                """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, voucherId, voucherId);
            
            if (rows.isEmpty()) {
                log.warn("Voucher not found: {}", voucherId);
                return null;
            }
            
            Map<String, Object> row = rows.get(0);
            String customMetadataJson = getString(row, "custom_metadata_json");
            log.info("DEBUG: Loaded Voucher: {}, JSON: {}", voucherId, customMetadataJson);
            
            // 解析分录获取角色
            Set<AccountRole> debitRoles = extractRoles(customMetadataJson, true);
            Set<AccountRole> creditRoles = extractRoles(customMetadataJson, false);
            log.info("DEBUG: Roles for {}: Debit={}, Credit={}", voucherId, debitRoles, creditRoles);

            // 尝试从 JSON 中提取辅助信息 (兼容旧逻辑，虽然 custom_metadata 是数组，但为了健壮性保留)
            // 如果是数组，这里提取会失败，但不影响核心逻辑
            String counterpartyId = null;
            String counterpartyName = null;
            String voucherWord = "记"; // 默认
            
            return VoucherData.builder()
                .voucherId(voucherId)
                .voucherNo(getString(row, "archive_code"))
                .summary(getString(row, "summary"))
                .amount(getBigDecimal(row, "amount"))
                .docDate(getLocalDate(row, "doc_date"))
                .counterpartyId(counterpartyId)
                .counterpartyName(counterpartyName)
                .voucherWord(voucherWord)
                .debitRoles(debitRoles)
                .creditRoles(creditRoles)
                .build();
        } catch (Exception e) {
            log.error("Failed to load voucher: {}", voucherId, e);
            return null;
        }
    }

    private Set<AccountRole> extractRoles(String json, boolean isDebit) {
        if (json == null || json.isBlank()) {
            return Collections.emptySet();
        }
        
        Set<AccountRole> roles = new HashSet<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
            
            if (root.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode entry : root) {
                    processEntryNode(entry, isDebit, roles);
                }
            } else if (root.has("entries") && root.get("entries").isArray()) {
                // 兼容对象结构 {"entries": [...]}
                for (com.fasterxml.jackson.databind.JsonNode entry : root.get("entries")) {
                    processEntryNode(entry, isDebit, roles);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse accounting entries JSON: {}", e.getMessage());
        }
        return roles;
    }

    private void processEntryNode(com.fasterxml.jackson.databind.JsonNode entry, boolean isDebit, Set<AccountRole> roles) {
        // 检查金额方向
        java.math.BigDecimal amount = java.math.BigDecimal.ZERO;
        if (isDebit && entry.has("debit_org")) {
            amount = entry.get("debit_org").decimalValue();
        } else if (!isDebit && entry.has("credit_org")) {
            amount = entry.get("credit_org").decimalValue();
        }
        
        if (amount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            // 解析科目
            if (entry.has("accsubject")) {
                com.fasterxml.jackson.databind.JsonNode subject = entry.get("accsubject");
                String code = subject.has("code") ? subject.get("code").asText() : "";
                String name = subject.has("name") ? subject.get("name").asText() : "";
                
                AccountRole role = mapSubjectToRole(code, name);
                if (role != null) {
                    roles.add(role);
                }
            }
        }
    }

    private AccountRole mapSubjectToRole(String code, String name) {
        if (code == null) code = "";
        if (name == null) name = "";
        
        // 1. 基于编码规则 (优先)
        if (code.startsWith("1001")) return AccountRole.CASH;
        if (code.startsWith("1002")) return AccountRole.BANK;
        if (code.startsWith("1122") || code.startsWith("1123")) return AccountRole.RECEIVABLE;
        if (code.startsWith("2202") || code.startsWith("2203")) return AccountRole.PAYABLE;
        if (code.startsWith("66") || code.startsWith("5")) return AccountRole.EXPENSE; // 管理/销售/生产成本
        if (code.startsWith("60") || code.startsWith("63")) return AccountRole.REVENUE; // 主营/其他收入
        if (code.startsWith("2221")) return AccountRole.TAX;
        if (code.startsWith("2211")) return AccountRole.SALARY;
        if (code.startsWith("14")) return AccountRole.INVENTORY;
        if (code.startsWith("16")) return AccountRole.ASSET;
        
        // 2. 基于名称关键词 (兜底)
        if (name.contains("银行")) return AccountRole.BANK;
        if (name.contains("现金")) return AccountRole.CASH;
        if (name.contains("薪酬") || name.contains("工资") || name.contains("奖金")) return AccountRole.SALARY;
        if (name.contains("税")) return AccountRole.TAX;
        if (name.contains("应收")) return AccountRole.RECEIVABLE;
        if (name.contains("应付")) return AccountRole.PAYABLE;
        if (name.contains("费") || name.contains("折旧")) return AccountRole.EXPENSE; // 费用, 差旅费, 办公费
        if (name.contains("收入")) return AccountRole.REVENUE;
        
        return null; // 未识别
    }
    
    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }
    
    private BigDecimal getBigDecimal(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return null;
    }
    
    private LocalDate getLocalDate(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof LocalDate) return (LocalDate) value;
        return null;
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
    
    private void saveMatchResult(MatchResult result) {
        String sql = """
            INSERT INTO voucher_match_result 
            (match_batch_id, voucher_id, voucher_hash, template_id, template_version, scene, status, match_details, missing_docs, created_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, NOW())
            """;
        try {
            jdbcTemplate.update(sql,
                result.getMatchBatchId(),
                result.getVoucherId(),
                result.getVoucherHash(),
                result.getTemplateId(),
                result.getTemplateVersion(),
                result.getScene() != null ? result.getScene().name() : null,
                result.getStatus() != null ? result.getStatus().name() : null,
                JSONUtil.toJsonStr(result.getLinks()),
                result.getMissingDocs() != null ? result.getMissingDocs().toArray(new String[0]) : null
            );
        } catch (Exception e) {
            log.warn("Failed to save match result: {}", e.getMessage());
        }
    }
    
    /**
     * 将匹配到的原始凭证文件链接到记账凭证档案
     * 这样前端"关联附件"Tab才能看到文件
     */
    private void linkMatchedFilesToArchive(String archiveId, List<LinkResult> links) {
        if (links == null || links.isEmpty()) {
            return;
        }
        
        for (LinkResult link : links) {
            if (!"MATCHED".equals(link.getStatus()) || link.getMatchedDocId() == null) {
                continue;
            }
            
            // 查找原始凭证关联的文件
            String findFilesSql = """
                SELECT id FROM arc_original_voucher_file 
                WHERE voucher_id = ? AND deleted = 0
                """;
            
            try {
                List<String> fileIds = jdbcTemplate.queryForList(findFilesSql, String.class, link.getMatchedDocId());
                
                for (String fileId : fileIds) {
                    // 插入到 acc_archive_attachment 表
                    String insertSql = """
                        INSERT INTO acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, NOW())
                        ON CONFLICT (id) DO NOTHING
                        """;
                    
                    String linkId = IdUtil.fastSimpleUUID();
                    String attachmentType = link.getEvidenceRole() != null ? link.getEvidenceRole().name() : "OTHER";
                    String desc = "智能匹配关联: " + (link.getEvidenceRoleName() != null ? link.getEvidenceRoleName() : "");
                    
                    jdbcTemplate.update(insertSql, linkId, archiveId, fileId, attachmentType, desc, "system");
                    log.info("Linked file {} to archive {} as {}", fileId, archiveId, attachmentType);
                }
            } catch (Exception e) {
                log.warn("Failed to link matched files for {}: {}", link.getMatchedDocId(), e.getMessage());
            }
        }
    }
    
    private MatchResult buildErrorResult(String voucherId, String message) {
        return MatchResult.builder()
            .voucherId(voucherId)
            .status(MatchStatus.ERROR)
            .message(message)
            .createdTime(LocalDateTime.now())
            .build();
    }
    /**
     * 更新档案状态与匹配得分（状态驱动流）
     */
    private void updateArchiveStatus(String archiveId, MatchResult matchResult) {
        String status = "MATCH_PENDING";
        MatchStatus matchStatus = matchResult.getStatus();
        if (matchStatus == MatchStatus.MATCHED || matchStatus == MatchStatus.CONFIRMED) {
            status = "MATCHED";
        }
        
        Integer score = matchResult.getConfidence() != null ? matchResult.getConfidence().intValue() : 0;
        String method = matchResult.getRecognitionReasons() != null && !matchResult.getRecognitionReasons().isEmpty() 
                ? String.join(",", matchResult.getRecognitionReasons()) 
                : "智能算法";

        jdbcTemplate.update(
            "UPDATE acc_archive SET status = ?, match_score = ?, match_method = ?, last_modified_time = NOW() WHERE id = ?",
            status, score, method, archiveId
        );
        log.debug("Updated archive {} status to {}, score {}", archiveId, status, score);
    }
}
