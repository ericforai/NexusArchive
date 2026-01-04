INSERT INTO sys_sql_audit_rule (rule_key, rule_value, created_time, last_modified_time)
VALUES ('protected_markers', 'arc_dict,acc_archive', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_sql_audit_rule (rule_key, rule_value, created_time, last_modified_time)
VALUES ('required_columns', 'fonds_no,fiscal_year', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
