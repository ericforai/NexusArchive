// Input: Spring JdbcTemplate, VoucherData
// Output: VoucherLoader
// Pos: Matching Engine
// 负责从数据库加载凭证数据并解析科目角色

package com.nexusarchive.engine.matching.loader;

import com.nexusarchive.engine.matching.dto.VoucherData;
import com.nexusarchive.engine.matching.enums.AccountRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 凭证数据加载器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>从数据库加载凭证数据</li>
 *   <li>解析分录 JSON 提取科目角色</li>
 *   <li>科目代码/名称 → 角色映射</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VoucherLoader {

    private final JdbcTemplate jdbcTemplate;
    private final AccountRoleMapper roleMapper;

    private static final String DEFAULT_VOUCHER_WORD = "记";

    /**
     * 加载凭证数据
     *
     * @param voucherId 凭证 ID
     * @return 凭证数据，如果不存在返回 null
     */
    public VoucherData loadVoucher(String voucherId) {
        try {
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

            return VoucherData.builder()
                .voucherId(voucherId)
                .voucherNo(getString(row, "archive_code"))
                .summary(getString(row, "summary"))
                .amount(getBigDecimal(row, "amount"))
                .docDate(getLocalDate(row, "doc_date"))
                .counterpartyId(null)
                .counterpartyName(null)
                .voucherWord(DEFAULT_VOUCHER_WORD)
                .debitRoles(debitRoles)
                .creditRoles(creditRoles)
                .build();
        } catch (Exception e) {
            log.error("Failed to load voucher: {}", voucherId, e);
            return null;
        }
    }

    /**
     * 从分录 JSON 中提取科目角色
     *
     * @param json 分录 JSON
     * @param isDebit 是否为借方
     * @return 科目角色集合
     */
    public Set<AccountRole> extractRoles(String json, boolean isDebit) {
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
                for (com.fasterxml.jackson.databind.JsonNode entry : root.get("entries")) {
                    processEntryNode(entry, isDebit, roles);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse accounting entries JSON: {}", e.getMessage());
        }
        return roles;
    }

    /**
     * 处理分录节点
     */
    private void processEntryNode(com.fasterxml.jackson.databind.JsonNode entry,
                                  boolean isDebit,
                                  Set<AccountRole> roles) {
        // 检查金额方向
        BigDecimal amount = BigDecimal.ZERO;
        if (isDebit && entry.has("debit_org")) {
            amount = entry.get("debit_org").decimalValue();
        } else if (!isDebit && entry.has("credit_org")) {
            amount = entry.get("credit_org").decimalValue();
        }

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            // 解析科目
            if (entry.has("accsubject")) {
                com.fasterxml.jackson.databind.JsonNode subject = entry.get("accsubject");
                String code = subject.has("code") ? subject.get("code").asText() : "";
                String name = subject.has("name") ? subject.get("name").asText() : "";

                AccountRole role = roleMapper.mapSubjectToRole(code, name);
                if (role != null) {
                    roles.add(role);
                }
            }
        }
    }

    // ========== Row mapping helpers ==========

    /**
     * 从数据库行获取字符串值
     */
    public String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 从数据库行获取 BigDecimal 值
     */
    public BigDecimal getBigDecimal(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return null;
    }

    /**
     * 从数据库行获取 LocalDate 值
     */
    public LocalDate getLocalDate(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof LocalDate) return (LocalDate) value;
        return null;
    }
}
