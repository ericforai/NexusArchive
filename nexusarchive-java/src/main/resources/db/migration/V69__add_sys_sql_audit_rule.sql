-- Input: SQL 审计规则字典
-- Output: sys_sql_audit_rule 规则表与默认规则
-- Pos: DB migration
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

CREATE TABLE IF NOT EXISTS sys_sql_audit_rule (
  rule_key VARCHAR(64) PRIMARY KEY,
  rule_value VARCHAR(1024) NOT NULL,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN sys_sql_audit_rule.rule_key IS '规则键';
COMMENT ON COLUMN sys_sql_audit_rule.rule_value IS '规则值';
COMMENT ON COLUMN sys_sql_audit_rule.created_time IS '创建时间';
COMMENT ON COLUMN sys_sql_audit_rule.last_modified_time IS '更新时间';

INSERT INTO sys_sql_audit_rule (rule_key, rule_value, created_time, last_modified_time)
SELECT 'protected_markers', 'acc_archive,arc_,bas_fonds,sys_fonds', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM sys_sql_audit_rule WHERE rule_key = 'protected_markers'
);

INSERT INTO sys_sql_audit_rule (rule_key, rule_value, created_time, last_modified_time)
SELECT 'required_columns', 'fonds_no,archive_year', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM sys_sql_audit_rule WHERE rule_key = 'required_columns'
);
