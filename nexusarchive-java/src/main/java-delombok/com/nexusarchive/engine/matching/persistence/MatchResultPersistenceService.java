// Input: JdbcTemplate, MatchResult, LinkResult
// Output: MatchResultPersistenceService
// Pos: Matching Engine
// 负责匹配结果的持久化和档案操作

package com.nexusarchive.engine.matching.persistence;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.engine.matching.dto.LinkResult;
import com.nexusarchive.engine.matching.dto.MatchResult;
import com.nexusarchive.engine.matching.enums.MatchStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 匹配结果持久化服务
 *
 * <p>职责：</p>
 * <ul>
 *   <li>保存匹配结果到数据库</li>
 *   <li>查询匹配结果</li>
 *   <li>将匹配的文件链接到档案</li>
 *   <li>更新档案状态</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MatchResultPersistenceService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 保存匹配结果
     *
     * @param result 匹配结果
     */
    public void saveMatchResult(MatchResult result) {
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
     * 获取最新匹配结果
     *
     * @param voucherId 凭证 ID
     * @return 匹配结果，如果不存在返回 null
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

    /**
     * 将匹配到的原始凭证文件链接到记账凭证档案
     *
     * @param archiveId 档案 ID
     * @param links 匹配链接列表
     */
    public void linkMatchedFilesToArchive(String archiveId, List<LinkResult> links) {
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

    /**
     * 更新档案状态与匹配得分
     *
     * @param archiveId 档案 ID
     * @param matchResult 匹配结果
     */
    public void updateArchiveStatus(String archiveId, MatchResult matchResult) {
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

    /**
     * 将数据库行映射为匹配结果
     */
    private MatchResult mapRowToMatchResult(Map<String, Object> row) {
        String detailsJson = getString(row, "match_details");
        List<LinkResult> links = List.of();
        if (detailsJson != null) {
            links = JSONUtil.toList(detailsJson, LinkResult.class);
        }

        return MatchResult.builder()
            .voucherId(getString(row, "voucher_id"))
            .voucherHash(getString(row, "voucher_hash"))
            .templateId(getString(row, "template_id"))
            .templateVersion(getString(row, "template_version"))
            .scene(com.nexusarchive.engine.matching.enums.BusinessScene.valueOf(getString(row, "scene")))
            .status(com.nexusarchive.engine.matching.enums.MatchStatus.valueOf(getString(row, "status")))
            .links(links)
            .createdTime(row.get("created_time") instanceof java.sql.Timestamp ?
                ((java.sql.Timestamp) row.get("created_time")).toLocalDateTime() : LocalDateTime.now())
            .build();
    }

    /**
     * 从数据库行获取字符串值
     */
    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }
}
