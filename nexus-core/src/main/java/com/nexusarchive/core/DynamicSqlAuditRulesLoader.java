// Input: MyBatis-Plus、Spring
// Output: SQL 审计规则动态加载服务
// Pos: nexus-core 合规层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SQL 审计规则动态加载器
 * 
 * [P2-FIX] 从数据库表 sys_sql_audit_rule 动态加载规则，支持热更新
 * 
 * 表结构参考 V69 迁移脚本
 */
@Service
public class DynamicSqlAuditRulesLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSqlAuditRulesLoader.class);
    
    private final AtomicReference<SqlAuditRules> cachedRules = new AtomicReference<>();
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired(required = false)
    public DynamicSqlAuditRulesLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @PostConstruct
    public void init() {
        loadRulesFromDatabase();
    }
    
    /**
     * 每5分钟刷新规则缓存
     */
    @Scheduled(fixedRate = 300000)
    public void refreshRules() {
        loadRulesFromDatabase();
    }
    
    /**
     * 手动刷新规则
     */
    public void forceRefresh() {
        loadRulesFromDatabase();
    }
    
    /**
     * 获取当前生效的规则
     */
    public SqlAuditRules getRules() {
        SqlAuditRules rules = cachedRules.get();
        if (rules == null) {
            return SqlAuditRules.defaults();
        }
        return rules;
    }
    
    private void loadRulesFromDatabase() {
        if (jdbcTemplate == null) {
            LOGGER.warn("JdbcTemplate 未注入，使用默认规则");
            cachedRules.set(SqlAuditRules.defaults());
            return;
        }
        
        try {
            // 检查表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'sys_sql_audit_rule'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);
            if (tableCount == null || tableCount == 0) {
                LOGGER.info("sys_sql_audit_rule 表不存在，使用默认规则");
                cachedRules.set(SqlAuditRules.defaults());
                return;
            }
            
            // 加载受保护的表标记
            List<String> protectedMarkers = jdbcTemplate.queryForList(
                    "SELECT table_pattern FROM sys_sql_audit_rule WHERE rule_type = 'PROTECTED_TABLE' AND enabled = true",
                    String.class);
            
            // 加载必需列
            List<String> requiredColumns = jdbcTemplate.queryForList(
                    "SELECT column_name FROM sys_sql_audit_rule WHERE rule_type = 'REQUIRED_COLUMN' AND enabled = true",
                    String.class);
            
            // 如果数据库中没有规则，使用默认值
            if (protectedMarkers.isEmpty()) {
                protectedMarkers = new ArrayList<>(List.of("acc_archive", "arc_", "bas_fonds", "sys_fonds"));
            }
            if (requiredColumns.isEmpty()) {
                requiredColumns = new ArrayList<>(List.of("fonds_no", "fiscal_year"));
            }
            
            SqlAuditRules rules = SqlAuditRules.of(protectedMarkers, requiredColumns);
            cachedRules.set(rules);
            
            LOGGER.info("[P2-FIX] 从数据库加载 SQL 审计规则: 受保护表={}, 必需列={}", 
                    protectedMarkers.size(), requiredColumns.size());
            
        } catch (Exception e) {
            LOGGER.warn("加载 SQL 审计规则失败，使用默认规则: {}", e.getMessage());
            cachedRules.set(SqlAuditRules.defaults());
        }
    }
}
