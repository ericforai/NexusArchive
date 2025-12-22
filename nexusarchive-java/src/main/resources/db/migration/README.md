一旦我所属的文件夹有所变化，请更新我。
本目录存放数据库迁移脚本。
用于版本升级迁移。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `V10__compliance_schema_update.sql` | SQL 脚本 | V10__compliance_schema_update 数据库脚本 |
| `V11__add_missing_archive_columns.sql` | SQL 脚本 | V11__add_missing_archive_columns 数据库脚本 |
| `V12__add_missing_timestamps.sql` | SQL 脚本 | V12__add_missing_timestamps 数据库脚本 |
| `V15__add_convert_log_table.sql` | SQL 脚本 | V15__add_convert_log_table 数据库脚本 |
| `V16__add_erp_config_table.sql` | SQL 脚本 | V16__add_erp_config_table 数据库脚本 |
| `V1__init_base_schema.sql` | SQL 脚本 | V1__init_base_schema 数据库脚本 |
| `V2.0.0__init_auth.sql` | SQL 脚本 | V2.0.0__init_auth 数据库脚本 |
| `V20__compliance_enhancement.sql` | SQL 脚本 | V20__compliance_enhancement 数据库脚本 |
| `V21__add_compliance_fields.sql` | SQL 脚本 | V21__add_compliance_fields 数据库脚本 |
| `V22__add_admin_user.sql` | SQL 脚本 | V22__add_admin_user 数据库脚本 |
| `V23__add_signature_log.sql` | SQL 脚本 | V23__add_signature_log 数据库脚本 |
| `V24__enhance_audit_log.sql` | SQL 脚本 | V24__enhance_audit_log 数据库脚本 |
| `V25__add_archive_summary.sql` | SQL 脚本 | V25__add_archive_summary 数据库脚本 |
| `V26__ofd_convert_log.sql` | SQL 脚本 | V26__ofd_convert_log 数据库脚本 |
| `V27__erp_config.sql` | SQL 脚本 | V27__erp_config 数据库脚本 |
| `V28__add_certificate_to_arc_file_content.sql` | SQL 脚本 | V28__add_certificate_to_arc_file_content 数据库脚本 |
| `V29__add_pre_archive_status.sql` | SQL 脚本 | V29__add_pre_archive_status 数据库脚本 |
| `V30__increase_column_length_for_archive_submit.sql` | SQL 脚本 | V30__increase_column_length_for_archive_submit 数据库脚本 |
| `V31__add_org_name_to_approval.sql` | SQL 脚本 | V31__add_org_name_to_approval 数据库脚本 |
| `V32__add_business_doc_no_to_arc_file_content.sql` | SQL 脚本 | V32__add_business_doc_no_to_arc_file_content 数据库脚本 |
| `V33__create_abnormal_voucher_table.sql` | SQL 脚本 | V33__create_abnormal_voucher_table 数据库脚本 |
| `V34__increase_archive_column_lengths_for_sm4.sql` | SQL 脚本 | V34__increase_archive_column_lengths_for_sm4 数据库脚本 |
| `V35__add_yonsuite_salesout_tables.sql` | SQL 脚本 | V35__add_yonsuite_salesout_tables 数据库脚本 |
| `V36__insert_seed_data.sql` | SQL 脚本 | V36__insert_seed_data 数据库脚本 |
| `V37__add_erp_voucher_no.sql` | SQL 脚本 | V37__add_erp_voucher_no 数据库脚本 |
| `V38__add_permission_table.sql` | SQL 脚本 | V38__add_permission_table 数据库脚本 |
| `V39__add_signature_columns.sql` | SQL 脚本 | V39__add_signature_columns 数据库脚本 |
| `V3__smart_parser_tables.sql` | SQL 脚本 | V3__smart_parser_tables 数据库脚本 |
| `V40__add_missing_entity_columns.sql` | SQL 脚本 | V40__add_missing_entity_columns 数据库脚本 |
| `V41__fix_schema_validation.sql` | SQL 脚本 | V41__fix_schema_validation 数据库脚本 |
| `V42__increase_archive_column_lengths.sql` | SQL 脚本 | V42__increase_archive_column_lengths 数据库脚本 |
| `V43__create_erp_scenario_table.sql` | SQL 脚本 | V43__create_erp_scenario_table 数据库脚本 |
| `V44__seed_erp_config.sql` | SQL 脚本 | V44__seed_erp_config 数据库脚本 |
| `V45__add_weaver_config.sql` | SQL 脚本 | V45__add_weaver_config 数据库脚本 |
| `V46__add_weaver_e10_config.sql` | SQL 脚本 | V46__add_weaver_e10_config 数据库脚本 |
| `V47__update_weaver_e10_credentials.sql` | SQL 脚本 | V47__update_weaver_e10_credentials 数据库脚本 |
| `V48__update_weaver_e10_host.sql` | SQL 脚本 | V48__update_weaver_e10_host 数据库脚本 |
| `V49__add_unique_biz_id_unique_index.sql` | SQL 脚本 | V49__add_unique_biz_id_unique_index 数据库脚本 |
| `V4__fix_archive_and_audit_columns.sql` | SQL 脚本 | V4__fix_archive_and_audit_columns 数据库脚本 |
| `V50__add_source_data_column.sql` | SQL 脚本 | V50__add_source_data_column 数据库脚本 |
| `V51__archive_attachment_link.sql` | SQL 脚本 | V51__archive_attachment_link 数据库脚本 |
| `V52__seed_dynamic_book_types.sql` | SQL 脚本 | V52__seed_dynamic_book_types 数据库脚本 |
| `V53__update_yonsuite_config_add_scenario.sql` | SQL 脚本 | V53__update_yonsuite_config_add_scenario 数据库脚本 |
| `V54__seed_boran_group_org.sql` | SQL 脚本 | V54__seed_boran_group_org 数据库脚本 |
| `V55__add_org_id_to_fonds_and_erp.sql` | SQL 脚本 | V55__add_org_id_to_fonds_and_erp 数据库脚本 |
| `V56__fix_payment_sync_config_and_scenario.sql` | SQL 脚本 | V56__fix_payment_sync_config_and_scenario 数据库脚本 |
| `V57__fix_yonsuite_accbook_code.sql` | SQL 脚本 | V57__fix_yonsuite_accbook_code 数据库脚本 |
| `V58__integration_center_enhancement.sql` | SQL 脚本 | V58__integration_center_enhancement 数据库脚本 |
| `V59__integration_audit_enhancement.sql` | SQL 脚本 | V59__integration_audit_enhancement 数据库脚本 |
| `V5__ingest_request_status.sql` | SQL 脚本 | V5__ingest_request_status 数据库脚本 |
| `V60__integration_templates_and_sub_interfaces.sql` | SQL 脚本 | V60__integration_templates_and_sub_interfaces 数据库脚本 |
| `V61__sync_history_compliance_enhancement.sql` | SQL 脚本 | V61__sync_history_compliance_enhancement 数据库脚本 |
| `V62__reconciliation_engine_schema.sql` | SQL 脚本 | V62__reconciliation_engine_schema 数据库脚本 |
| `V63__enhanced_security_hash_chain.sql` | SQL 脚本 | V63__enhanced_security_hash_chain 数据库脚本 |
| `V64__erp_feedback_queue.sql` | SQL 脚本 | V64__erp_feedback_queue 数据库脚本 |
| `V65__fix_foreign_keys_and_schema.sql` | SQL 脚本 | V65__fix_foreign_keys_and_schema 数据库脚本 |
| `V66__fix_erp_template_active_status.sql` | SQL 脚本 | V66__fix_erp_template_active_status 数据库脚本 |
| `V67__reconciliation_record_enhancements.sql` | SQL 脚本 | V67__reconciliation_record_enhancements 数据库脚本 |
| `V6__add_business_modules.sql` | SQL 脚本 | V6__add_business_modules 数据库脚本 |
| `V7__add_archive_approval.sql` | SQL 脚本 | V7__add_archive_approval 数据库脚本 |
| `V8__add_open_appraisal.sql` | SQL 脚本 | V8__add_open_appraisal 数据库脚本 |
| `V9__ensure_metadata_tables.sql` | SQL 脚本 | V9__ensure_metadata_tables 数据库脚本 |
