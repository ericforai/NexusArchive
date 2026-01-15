// Input: Spring Framework、JDBC、Java 标准库
// Output: OnboardingService 初始化向导服务
// Pos: 匹配引擎/Service
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching;

import com.nexusarchive.engine.matching.dto.*;
import com.nexusarchive.engine.matching.enums.AccountRole;
import com.nexusarchive.engine.matching.enums.EvidenceRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 初始化向导服务
 * 
 * 扫描客户数据，应用预置规则，自动猜测映射
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 扫描客户现有数据
     */
    public OnboardingSummary scanExistingData(Long companyId) {
        // 统计已有科目数量（从 Archive 表推断）
        int totalAccounts = countDistinctAccounts(companyId);
        int matchedAccounts = countMappedAccounts(companyId);
        
        // 统计已有单据类型
        int totalDocTypes = countDistinctDocTypes(companyId);
        int matchedDocTypes = countMappedDocTypes(companyId);
        
        return OnboardingSummary.builder()
            .totalAccounts(totalAccounts)
            .matchedAccounts(matchedAccounts)
            .unmatchedAccounts(totalAccounts - matchedAccounts)
            .accountMatchRate(totalAccounts > 0 ? (double) matchedAccounts / totalAccounts : 0)
            .totalDocTypes(totalDocTypes)
            .matchedDocTypes(matchedDocTypes)
            .unmatchedDocTypes(totalDocTypes - matchedDocTypes)
            .docTypeMatchRate(totalDocTypes > 0 ? (double) matchedDocTypes / totalDocTypes : 0)
            .status(matchedAccounts == totalAccounts && matchedDocTypes == totalDocTypes ? "COMPLETED" : "PENDING")
            .build();
    }
    
    /**
     * 应用预置规则
     */
    @Transactional
    public AutoMappingResult applyPreset(Long companyId, String kitId) {
        List<AutoMappingResult.MappedItem> accountMappings = new ArrayList<>();
        List<AutoMappingResult.MappedItem> docTypeMappings = new ArrayList<>();
        
        // 加载预置规则
        List<Map<String, Object>> accountPresets = loadAccountPresets(kitId);
        List<Map<String, Object>> docPresets = loadDocPresets(kitId);
        
        // 获取客户的科目列表（模拟从现有数据推断）
        List<String> customerAccounts = getCustomerAccounts(companyId);
        for (String accountCode : customerAccounts) {
            String matchedRole = matchAccountRole(accountCode, accountPresets);
            if (matchedRole != null) {
                saveAccountMapping(companyId, accountCode, matchedRole, "PRESET");
                accountMappings.add(AutoMappingResult.MappedItem.builder()
                    .sourceCode(accountCode)
                    .targetRole(matchedRole)
                    .confidence(1.0)
                    .matchRule("正则匹配")
                    .build());
            }
        }
        
        // 获取客户的单据类型
        List<String> customerDocTypes = getCustomerDocTypes(companyId);
        for (String docType : customerDocTypes) {
            String matchedRole = matchDocRole(docType, docPresets);
            if (matchedRole != null) {
                saveDocTypeMapping(companyId, docType, matchedRole, "PRESET");
                docTypeMappings.add(AutoMappingResult.MappedItem.builder()
                    .sourceCode(docType)
                    .sourceName(docType)
                    .targetRole(matchedRole)
                    .confidence(1.0)
                    .matchRule("关键词匹配")
                    .build());
            }
        }
        
        return AutoMappingResult.builder()
            .kitId(kitId)
            .kitName("通用行业预置包")
            .accountsMapped(accountMappings.size())
            .accountsPending(customerAccounts.size() - accountMappings.size())
            .accountMappings(accountMappings)
            .docTypesMapped(docTypeMappings.size())
            .docTypesPending(customerDocTypes.size() - docTypeMappings.size())
            .docTypeMappings(docTypeMappings)
            .build();
    }
    
    /**
     * 获取待确认项
     */
    public List<UnmatchedItem> getPendingItems(Long companyId) {
        List<UnmatchedItem> items = new ArrayList<>();
        
        // 查找未映射的科目
        String sql = """
            SELECT DISTINCT account_code 
            FROM (SELECT 'mock_account' as account_code) t
            WHERE account_code NOT IN (
                SELECT account_code FROM cfg_account_role_mapping WHERE company_id = ?
            )
            """;
        // 实际实现中应从业务表获取
        
        return items;
    }
    
    /**
     * 确认映射
     */
    @Transactional
    public void confirmMappings(Long companyId, List<MappingConfirmation> mappings) {
        for (MappingConfirmation mapping : mappings) {
            if ("ACCOUNT".equals(mapping.getType())) {
                saveAccountMapping(companyId, mapping.getCode(), mapping.getRole(), "MANUAL");
            } else if ("DOC_TYPE".equals(mapping.getType())) {
                saveDocTypeMapping(companyId, mapping.getCode(), mapping.getRole(), "MANUAL");
            }
        }
        log.info("Confirmed {} mappings for company {}", mappings.size(), companyId);
    }
    
    // ========== 私有方法 ==========
    
    private int countDistinctAccounts(Long companyId) {
        // 实际实现中应查询业务表
        return 50;  // Mock
    }
    
    private int countMappedAccounts(Long companyId) {
        try {
            String sql = "SELECT COUNT(*) FROM cfg_account_role_mapping WHERE company_id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, companyId);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int countDistinctDocTypes(Long companyId) {
        return 10;  // Mock
    }
    
    private int countMappedDocTypes(Long companyId) {
        try {
            String sql = "SELECT COUNT(*) FROM cfg_doc_type_mapping WHERE company_id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, companyId);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private List<Map<String, Object>> loadAccountPresets(String kitId) {
        try {
            return jdbcTemplate.queryForList(
                "SELECT account_pattern, account_role, priority FROM cfg_account_role_preset WHERE kit_id = ? ORDER BY priority DESC",
                kitId
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    private List<Map<String, Object>> loadDocPresets(String kitId) {
        try {
            return jdbcTemplate.queryForList(
                "SELECT doc_type_pattern, keywords, evidence_role FROM cfg_doc_type_preset WHERE kit_id = ?",
                kitId
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    private List<String> getCustomerAccounts(Long companyId) {
        // Mock: 返回常见科目编码
        return Arrays.asList("1001", "100201", "100202", "1122", "2202", "6601", "6602");
    }
    
    private List<String> getCustomerDocTypes(Long companyId) {
        // Mock: 返回常见单据类型
        return Arrays.asList("付款审批单", "银行回单", "增值税专用发票", "采购合同");
    }
    
    private String matchAccountRole(String accountCode, List<Map<String, Object>> presets) {
        for (Map<String, Object> preset : presets) {
            String pattern = (String) preset.get("account_pattern");
            if (Pattern.matches(pattern, accountCode)) {
                return (String) preset.get("account_role");
            }
        }
        return null;
    }
    
    private String matchDocRole(String docType, List<Map<String, Object>> presets) {
        for (Map<String, Object> preset : presets) {
            Object keywordsObj = preset.get("keywords");
            if (keywordsObj != null) {
                // PostgreSQL Array 转换
                String[] keywords = keywordsObj instanceof String[] ? 
                    (String[]) keywordsObj : 
                    keywordsObj.toString().replaceAll("[{}]", "").split(",");
                for (String keyword : keywords) {
                    if (docType.contains(keyword.trim())) {
                        return (String) preset.get("evidence_role");
                    }
                }
            }
        }
        return null;
    }
    
    private void saveAccountMapping(Long companyId, String accountCode, String role, String source) {
        String sql = """
            INSERT INTO cfg_account_role_mapping (company_id, account_code, aux_type, account_role, source, created_time, updated_time)
            VALUES (?, ?, 'NONE', ?, ?, NOW(), NOW())
            ON CONFLICT (company_id, account_code, aux_type) DO UPDATE SET
                account_role = EXCLUDED.account_role,
                source = EXCLUDED.source,
                updated_time = NOW()
            """;
        try {
            jdbcTemplate.update(sql, companyId, accountCode, role, source);
        } catch (Exception e) {
            log.warn("Failed to save account mapping: {}", e.getMessage());
        }
    }
    
    private void saveDocTypeMapping(Long companyId, String docType, String role, String source) {
        String sql = """
            INSERT INTO cfg_doc_type_mapping (company_id, customer_doc_type, evidence_role, source, created_time, updated_time)
            VALUES (?, ?, ?, ?, NOW(), NOW())
            ON CONFLICT (company_id, customer_doc_type) DO UPDATE SET
                evidence_role = EXCLUDED.evidence_role,
                source = EXCLUDED.source,
                updated_time = NOW()
            """;
        try {
            jdbcTemplate.update(sql, companyId, docType, role, source);
        } catch (Exception e) {
            log.warn("Failed to save doc type mapping: {}", e.getMessage());
        }
    }
}
