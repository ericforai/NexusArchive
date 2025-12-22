# 变更清单

列出本次所有变更文件、类型、原因与是否需要人工确认。

| 文件路径 | 变更 | 原因 | 需人工确认 |
| --- | --- | --- | --- |
| `.dockerignore` | 修改 | 补齐配置文件头注释 | 否 |
| `.env` | 修改 | 补齐配置文件头注释 | 否 |
| `.env.example` | 修改 | 补齐配置文件头注释 | 否 |
| `.env.local` | 修改 | 补齐配置文件头注释 | 否 |
| `.env.template` | 修改 | 补齐配置文件头注释 | 否 |
| `.github/workflows/permission-tests.yml` | 修改 | 补齐配置文件头注释 | 否 |
| `.gitignore` | 修改 | 补齐配置文件头注释 | 否 |
| `Dockerfile.frontend` | 修改 | 补齐 Dockerfile 头注释 | 否 |
| `README.md` | 修改 | 更新文档自洽规则/忽略白名单/链接修正 | 否 |
| `data/init_collection_scenario.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/build.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/build_incremental.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/build_offline_package.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/build_patch.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/build_security_patch.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/deploy.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/docker-compose.yml` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/helm/Chart.yaml` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/helm/templates/deployment-backend.yaml` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/helm/templates/deployment-frontend.yaml` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/helm/templates/secret-license.yaml` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/helm/templates/service-backend.yaml` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/helm/templates/service-frontend.yaml` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/helm/values.yaml` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/init_server.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/install_db.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/monitoring/prometheus.yml` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/nexusarchive.service` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/nginx.conf` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/offline/bin/GenLicense.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `deploy/offline/config/nexusarchive.service` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/offline/config/nginx.conf` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/offline/db/migration/V10__compliance_schema_update.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V11__add_missing_archive_columns.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V12__add_missing_timestamps.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V15__add_convert_log_table.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V16__add_erp_config_table.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V1__init_base_schema.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V2.0.0__init_auth.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V20__compliance_enhancement.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V21__add_compliance_fields.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V22__add_admin_user.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V23__add_signature_log.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V24__enhance_audit_log.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V25__add_archive_summary.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V26__ofd_convert_log.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V27__erp_config.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V28__add_certificate_to_arc_file_content.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V29__add_pre_archive_status.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V30__increase_column_length_for_archive_submit.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V31__add_org_name_to_approval.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V32__add_business_doc_no_to_arc_file_content.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V33__create_abnormal_voucher_table.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V34__increase_archive_column_lengths_for_sm4.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V3__smart_parser_tables.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V4__fix_archive_and_audit_columns.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V5__ingest_request_status.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V6__add_business_modules.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V7__add_archive_approval.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V8__add_open_appraisal.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/db/migration/V9__ensure_metadata_tables.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `deploy/offline/deps/README.md` | 修改 | 补齐目录 MD 头部声明与文件清单 | 否 |
| `deploy/offline/install.conf.template` | 修改 | 补齐配置文件头注释 | 否 |
| `deploy/offline/install.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/offline/uninstall.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/offline/upgrade.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/setup_demo_aip.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/setup_ssl.sh` | 修改 | 补齐脚本头注释 | 否 |
| `deploy/tools/GenLicense.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `deploy/tools/LicenseGenerator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `docker-compose.dev.yml` | 修改 | 补齐配置文件头注释 | 否 |
| `docs/README.md` | 修改 | 补齐目录 MD 头部声明与文件清单 | 否 |
| `docs/agents/README.md` | 修改 | 补齐目录 MD 头部声明与文件清单 | 否 |
| `docs/ai-brain/README.md` | 修改 | 补齐目录 MD 头部声明与文件清单 | 否 |
| `docs/database/auth_schema.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `docs/database/auth_schema_dameng.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `docs/database/auth_schema_kingbase.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `docs/database/auth_schema_mysql.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `docs/database/compliance_schema.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `docs/knowledge/README.md` | 修改 | 补齐目录 MD 头部声明与文件清单 | 否 |
| `index.html` | 修改 | 补齐 HTML/XML 头注释 | 否 |
| `mock_data_attachments.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/.dockerignore` | 修改 | 补齐文件头注释 | 否 |
| `nexusarchive-java/Dockerfile` | 修改 | 补齐 Dockerfile 头注释 | 否 |
| `nexusarchive-java/README.md` | 修改 | 补齐目录 MD 头部声明与文件清单 | 否 |
| `nexusarchive-java/pom.xml` | 修改 | 补齐 HTML/XML 头注释 | 否 |
| `nexusarchive-java/scripts/generate_jwt_keys.sh` | 修改 | 补齐脚本头注释 | 否 |
| `nexusarchive-java/scripts/generate_keystore.sh` | 修改 | 补齐脚本头注释 | 否 |
| `nexusarchive-java/scripts/generate_keystore_docker.sh` | 修改 | 补齐脚本头注释 | 否 |
| `nexusarchive-java/scripts/generate_keystore_simple.sh` | 修改 | 补齐脚本头注释 | 否 |
| `nexusarchive-java/scripts/health_check.sh` | 修改 | 补齐脚本头注释 | 否 |
| `nexusarchive-java/scripts/insert_demo_data.sh` | 修改 | 补齐脚本头注释 | 否 |
| `nexusarchive-java/setup.sh` | 修改 | 补齐脚本头注释 | 否 |
| `nexusarchive-java/simulate_webhook.py` | 修改 | 补齐脚本头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/NexusArchiveApplication.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/annotation/ArchivalAudit.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/aspect/ArchivalAuditAspect.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/Result.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/constant/ErrorCode.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/enums/ArchiveFileType.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/enums/BorrowingStatus.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/enums/DataScopeType.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/enums/DirectionType.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/enums/RoleCategory.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/enums/VoucherType.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/exception/BusinessException.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/result/BatchOperationResult.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/result/Result.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/AsyncConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/CorsConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/ElasticsearchConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/EntitySchemaValidator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/GlobalExceptionHandler.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/JwtAuthenticationFilter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/LicenseValidationFilter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/MigrationGatekeeperInterceptor.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/MyBatisPlusConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/MybatisConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/PostgresJsonTypeHandler.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/RateLimitFilter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/RedisConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/ResilientFlywayRunner.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/RestAccessDeniedHandler.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/RestAuthenticationEntryPoint.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/SecurityConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/SecurityConfigValidator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/SimpleCorsFilter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/WebMvcConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/WebMvcCorsConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/XssFilter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/mybatis/EncryptTypeHandler.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/AbnormalVoucherController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/AdminOrgController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/AdminPermissionController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/AdminRoleController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/AdminUserController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/ArchiveApprovalController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/ArchiveController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/ArchiveExportController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/ArchiveFileController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/AttachmentController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/AuditLogController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/AuthController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/BankReceiptController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/BasFondsController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/BorrowingController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/CertificateController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/ComplianceController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/DestructionController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpConfigController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/ErpScenarioController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/GlobalSearchController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/HealthController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/IngestController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/LicenseController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/MonitoringController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/NavController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/NotificationController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/OfdConvertController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/OpenAppraisalController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/OpsController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/PoolController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/PositionController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/ReconciliationController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/RelationController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/SignatureController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/StatsController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/SystemConfigController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/TicketSyncController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/TimestampController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/UserController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/VolumeController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/WarehouseController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/WebIngestController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/WorkflowController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/ArchiveRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/ErpScenarioDTO.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/FileUploadResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/GlobalSearchDTO.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/IntegrationChannelDTO.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/MetadataUpdateDTO.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/PoolItemDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/aip/AipAccountingXml.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/aip/AipIndexFile.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/aip/AipIndexXml.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/aip/EepXmlStructure.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/monitoring/IntegrationMonitoringDTO.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/notification/NotificationDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/parser/ParsedInvoice.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/reconciliation/ReconciliationRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/relation/ComplianceStatusDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/relation/LinkedFileDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/relation/RelationEdgeDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/relation/RelationGraphDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/relation/RelationNodeDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/request/CreatePositionRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/request/CreateUserRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/request/LoginRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/request/OrgImportResult.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/request/ResetPasswordRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/request/UpdatePositionRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/request/UpdateUserRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/request/UpdateUserStatusRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/response/LoginResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/response/UserResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/search/ArchiveDocument.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/search/SearchResult.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/SignResult.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/VerifyResult.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/AccountingSipDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/AttachmentDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/IngestResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/VoucherEntryDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/VoucherHeadDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/report/CheckItem.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/report/FourNatureReport.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/report/OverallStatus.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/stats/ArchivalTrendDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/stats/DashboardStatsDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/stats/StorageStatsDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/stats/TaskStatusStatsDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/workflow/WorkflowTaskDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/engine/ErpMappingEngine.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/AbnormalVoucher.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ArcFileContent.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ArcFileMetadataIndex.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ArcSignatureLog.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ArchivalCodeSequence.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/Archive.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ArchiveApproval.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ArchiveAttachment.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ArchiveBatch.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ArchiveRelation.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/AuditInspectionLog.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/BasFonds.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/Borrowing.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ConvertLog.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/Destruction.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ErpConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ErpScenario.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ErpSubInterface.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/IngestRequestStatus.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/Location.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/OpenAppraisal.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/Org.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/Permission.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/Position.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/ReconciliationRecord.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/Role.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/SyncHistory.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/SysAuditLog.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/SystemSetting.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/User.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/Volume.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/enums/PreArchiveStatus.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/es/ArchiveDocument.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/event/CheckPassedEvent.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/event/VoucherReceivedEvent.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/core/connector/SourceConnector.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/core/context/SyncContext.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/core/model/FileAttachmentDTO.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/core/model/UnifiedDocumentDTO.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/ErpAdapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/ErpAdapterFactory.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/GenericErpAdapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/KingdeeAdapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/WeaverAdapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/WeaverE10Adapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/YonSuiteErpAdapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/controller/ErpIngestController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/AccountSummaryDTO.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/AttachmentDTO.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/BatchIngestRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/BatchIngestResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/ConnectionTestResult.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/ErpConfig.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/ErpPageRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/ErpPageResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/ErpVoucherDto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/FeedbackResult.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/IngestResult.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/VoucherDTO.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/impl/GenericErpAdapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/impl/KingdeeAdapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/mock/YonSuiteMockController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/service/UniversalSyncEngine.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/client/YonSuiteClient.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/connector/YonSuiteConnector.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/controller/YonPaymentTestController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/controller/YonSuiteCollectionController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/controller/YonSuiteVoucherController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/controller/YonSuiteWebhookController.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonAttachmentListResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonCollectionBillRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonCollectionBillResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonCollectionDetailResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonCollectionFileRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonCollectionFileResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonPaymentDetailResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonVoucherDetailRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonVoucherDetailResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonVoucherListRequest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/YonVoucherListResponse.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/event/YonSuiteVoucherEvent.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/event/YonSuiteVoucherEventListener.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/mapper/YonVoucherMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/security/WebhookNonceStore.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/security/YonSuiteEventCrypto.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/security/YonSuiteSignatureValidator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/service/YonAuthService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/service/YonPaymentFileService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/service/YonPaymentListService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/service/YonSuiteVoucherSyncService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/listener/ComplianceListener.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/listener/ProcessingListener.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/listener/SignatureTimestampListener.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/AbnormalVoucherMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArcFileContentMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArcFileMetadataIndexMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArcSignatureLogMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArchivalCodeSequenceMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArchiveApprovalMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArchiveAttachmentMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArchiveBatchMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArchiveMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArchiveRelationMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/AuditInspectionLogMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/BasFondsMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/BorrowingMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ConvertLogMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/DestructionMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ErpConfigMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ErpScenarioMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ErpSubInterfaceMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/IngestRequestStatusMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/LocationMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/OpenAppraisalMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/OrgMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/PermissionMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/PositionMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/ReconciliationRecordMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/RoleMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/SyncHistoryMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/SysAuditLogMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/SystemSettingMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/UserMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/VolumeMapper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/repository/es/ArchiveSearchRepository.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/security/CustomUserDetails.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/AbnormalVoucherService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchivalCodeGenerator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchivalPackageService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveApprovalService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveExportService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveHealthCheckService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveRelationService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveSearchService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveSecurityService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/AttachmentService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/AuditLogQueryService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/AuditLogService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/AuthService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/AutoAssociationService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/BasFondsService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/BorrowingService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ComplianceCheckService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/CustomUserDetailsService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/DataScopeService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/DestructionService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/DigitalSignatureService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ErpDiagnosisService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ErpScenarioService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/FileStorageService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/FourNatureCheckService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/FourNatureCoreService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/GlobalSearchService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/IArchiveRelationService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/IAutoAssociationService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/IngestService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/LicenseService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/LoginAttemptService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/MonitoringService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/NotificationService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/OfdConvertService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/OpenAppraisalService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/OrgService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/PasswordPolicyValidator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/PermissionService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/PositionService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/PreArchiveCheckService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/PreArchiveSubmitService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ReconciliationService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/RoleService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/RoleValidationService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/SmartParserService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/StandardReportGenerator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/StatsService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/SystemSettingService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/TimestampService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/TokenBlacklistService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/UserService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/VolumeService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/VoucherPdfGeneratorService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/WarehouseService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/WorkflowService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/adapter/VirusScanAdapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/adapter/impl/ClamAvAdapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/adapter/impl/MockVirusScanner.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/converter/OfdConverterHelper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/AbnormalVoucherServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ArchivalPackageServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ArchiveApprovalServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ArchiveExportServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ArchiveSearchServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ArchiveSecurityServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/AttachmentServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/BasFondsServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/BorrowingServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/DestructionServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/FileStorageServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/FourNatureCheckServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/FourNatureCoreServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/GlobalSearchServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/IngestServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/MonitoringServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/NotificationServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/OfdConvertServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/OpenAppraisalServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/PositionServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/ReconciliationServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/SmartParserServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/StatsServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/WarehouseServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/WorkflowServiceImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ofd/OfdConvertService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/parser/InvoiceParser.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/parser/impl/PdfInvoiceParser.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/parser/impl/XmlInvoiceParser.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/search/ArchiveIndexService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/OfdSignatureHelper.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/SignatureAdapter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/Sm2SignatureService.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/strategy/AmountDateMatchStrategy.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/strategy/ArchivalCodeGenerator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/strategy/ExactMatchStrategy.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/strategy/MatchingStrategy.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/strategy/impl/ArchivalCodeGeneratorImpl.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/AmountValidator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/FileHashUtil.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/JwtUtil.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/PasswordHashGenerator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/PasswordPolicyValidator.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/PasswordUtil.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/PathSecurityUtils.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/SM3Utils.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/SM4Utils.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/SecurityUtil.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/XssFilter.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/application-dev.yml` | 修改 | 补齐配置文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/application.properties` | 修改 | 补齐配置文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/application.yml` | 修改 | 补齐配置文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/demo/demo_archive_features.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/demo_aip_data.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V10__compliance_schema_update.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V11__add_missing_archive_columns.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V12__add_missing_timestamps.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V15__add_convert_log_table.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V16__add_erp_config_table.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V1__init_base_schema.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V2.0.0__init_auth.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V20__compliance_enhancement.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V21__add_compliance_fields.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V22__add_admin_user.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V23__add_signature_log.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V24__enhance_audit_log.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V25__add_archive_summary.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V26__ofd_convert_log.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V27__erp_config.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V28__add_certificate_to_arc_file_content.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V29__add_pre_archive_status.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V30__increase_column_length_for_archive_submit.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V31__add_org_name_to_approval.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V32__add_business_doc_no_to_arc_file_content.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V33__create_abnormal_voucher_table.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V34__increase_archive_column_lengths_for_sm4.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V35__add_yonsuite_salesout_tables.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V36__insert_seed_data.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V37__add_erp_voucher_no.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V38__add_permission_table.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V39__add_signature_columns.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V3__smart_parser_tables.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V40__add_missing_entity_columns.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V41__fix_schema_validation.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V42__increase_archive_column_lengths.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V43__create_erp_scenario_table.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V44__seed_erp_config.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V45__add_weaver_config.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V46__add_weaver_e10_config.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V47__update_weaver_e10_credentials.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V48__update_weaver_e10_host.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V49__add_unique_biz_id_unique_index.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V4__fix_archive_and_audit_columns.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V50__add_source_data_column.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V51__archive_attachment_link.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V52__seed_dynamic_book_types.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V53__update_yonsuite_config_add_scenario.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V54__seed_boran_group_org.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V55__add_org_id_to_fonds_and_erp.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V56__fix_payment_sync_config_and_scenario.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V57__fix_yonsuite_accbook_code.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V58__integration_center_enhancement.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V59__integration_audit_enhancement.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V5__ingest_request_status.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V60__integration_templates_and_sub_interfaces.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V61__sync_history_compliance_enhancement.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V62__reconciliation_engine_schema.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V63__enhanced_security_hash_chain.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V64__erp_feedback_queue.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V65__fix_foreign_keys_and_schema.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V66__fix_erp_template_active_status.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V67__reconciliation_record_enhancements.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V6__add_business_modules.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V7__add_archive_approval.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V8__add_open_appraisal.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/V9__ensure_metadata_tables.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/main/resources/sql/schema-postgresql.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/controller/AdminRoleControllerTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/controller/AdminUserControllerTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/controller/ArchiveControllerTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/controller/AuthControllerTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/controller/BorrowingControllerTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/controller/ComplianceControllerIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/controller/DestructionControllerTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/controller/IngestFlowTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/controller/VolumeControllerIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/AbnormalVoucherControllerIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/ArchiveManagementIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/AuthenticationIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/BorrowingIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/ComplianceIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/PermissionIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/SecurityHardeningIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/SecurityInputValidationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/ThreeRoleExclusionIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/UserManagementIntegrationTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/listener/ComplianceListenerTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/listener/ProcessingListenerTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/ArchiveRelationServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/ArchiveServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/AuditLogServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/AuthServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/AutoAssociationServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/BorrowingServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/ComplianceCheckServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/DestructionServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/DigitalSignatureServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/FourNatureCheckServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/OrgServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/VolumeServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/impl/ArchiveExportServiceImplTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/impl/IngestServiceTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/impl/ReconciliationServiceImplTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/util/SM4UtilsTest.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/verification/VerifySignatureFix.java` | 修改 | 补齐 Java 文件头注释 | 否 |
| `nexusarchive-java/src/test/resources/application-test.yml` | 修改 | 补齐配置文件头注释 | 否 |
| `nexusarchive-java/src/test/resources/application.yml` | 修改 | 补齐配置文件头注释 | 否 |
| `nexusarchive-java/src/test/resources/db/migration/V1__init_test_schema.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `nexusarchive-java/src/test/resources/schema.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `playwright.config.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `public/robots.txt` | 修改 | 补齐 robots.txt 头注释 | 否 |
| `public/sitemap.xml` | 修改 | 补齐 HTML/XML 头注释 | 否 |
| `scripts/auth_smoke.sh` | 修改 | 补齐脚本头注释 | 否 |
| `scripts/backup_postgres.sh` | 修改 | 补齐脚本头注释 | 否 |
| `scripts/create_demo_files.sh` | 修改 | 补齐脚本头注释 | 否 |
| `scripts/delivery_gatekeeper.cjs` | 修改 | 补齐脚本/源码头注释 | 否 |
| `scripts/delivery_gatekeeper_v2.cjs` | 修改 | 补齐脚本/源码头注释 | 否 |
| `scripts/dev-start.sh` | 修改 | 补齐脚本头注释 | 否 |
| `scripts/restart-services.sh` | 修改 | 补齐脚本头注释 | 否 |
| `scripts/restore_postgres.sh` | 修改 | 补齐脚本头注释 | 否 |
| `scripts/seed_roles.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `scripts/self_check.sh` | 修改 | 补齐脚本头注释 | 否 |
| `scripts/start.sh` | 修改 | 补齐脚本头注释 | 否 |
| `scripts/stop.sh` | 修改 | 补齐脚本头注释 | 否 |
| `scripts/update_role_perms.sql` | 修改 | 补齐 SQL 文件头注释 | 否 |
| `scripts/verify_cold_start.cjs` | 修改 | 补齐脚本/源码头注释 | 否 |
| `scripts/verify_health_resilience.cjs` | 修改 | 补齐脚本/源码头注释 | 否 |
| `setup.sh` | 修改 | 补齐脚本头注释 | 否 |
| `src/App.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/SystemApp.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/__tests__/api/client.test.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/__tests__/components/ArchiveListView.test.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/__tests__/components/LoginView.test.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/__tests__/permissions/MenuPermission.test.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/__tests__/setup.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/__tests__/store/useAuthStore.test.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/abnormal.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/admin.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/archiveApproval.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/archives.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/attachments.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/audit.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/auth.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/autoAssociation.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/borrowing.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/client.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/destruction.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/erp.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/fonds.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/license.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/nav.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/notifications.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/openAppraisal.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/pool.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/search.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/stats.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/warehouse.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/api/workflow.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/AbnormalDataView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/ArchivalPanoramaView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/ArchiveApprovalView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/ArchiveListView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/AuditLogView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/BorrowingView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/ComplianceReportView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/Dashboard.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/DestructionRepositoryView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/DestructionView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/FourNatureReportView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/GlobalSearch.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/LoginView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/OCRProcessingView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/OnlineReceptionView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/OpenAppraisalView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/OpenInventoryView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/ProductWebsite.css` | 修改 | 补齐样式文件头注释 | 否 |
| `src/components/ProductWebsite.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/RelationshipQueryView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/RelationshipView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/RelationshipVisualizer.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/SettingsView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/Sidebar.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/StatsView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/TopBar.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/VolumeManagement.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/WarehouseView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/admin/AdminLayout.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/admin/FondsManagement.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/admin/PositionManagement.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/archive/AddRecordModal.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/archive/ComplianceModal.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/archive/LinkModal.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/archive/MatchPreviewModal.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/archive/RuleConfigModal.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/archive/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/auth/ProtectedRoute.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/common/ComplianceRadar.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/common/DemoBadge.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/common/ErrorBoundary.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/common/MetadataEditModal.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/common/OfdViewer.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/common/ReconciliationReport.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/common/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/debug/PaymentFileTestView.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/layout/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/org/Tree.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/panorama/ArchiveStructureTree.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/panorama/EvidencePreview.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/panorama/VoucherDetailCard.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/panorama/VoucherPlayer.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/settings/BasicSettings.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/settings/IntegrationSettings.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/settings/LicenseSettings.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/settings/OrgSettings.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/settings/RoleSettings.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/settings/SecuritySettings.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/settings/SettingsLayout.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/settings/UserSettings.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/components/settings/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/constants.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/features/archives/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/features/borrowing/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/features/compliance/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/features/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/features/settings/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/hooks/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/hooks/useArchives.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/hooks/usePermissions.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/index.css` | 修改 | 补齐样式文件头注释 | 否 |
| `src/index.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/layouts/SystemLayout.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/queryClient.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/routes/ActivationPage.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/routes/index.tsx` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/routes/paths.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/store/index.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/store/useAppStore.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/store/useAuthStore.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/store/useThemeStore.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/types.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/utils/audit.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/utils/notificationService.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/utils/storage.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `src/utils/taskScheduler.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/README.md` | 修改 | 补齐目录 MD 头部声明与文件清单 | 否 |
| `tests/playwright/api/archive_concurrency.spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/authz_audit.spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/backup_restore_full_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/backup_restore_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/batch_import_export_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/erp_sync.spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/four_integrities.spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/health_resilience_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/ocr_metadata_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/report_version_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/search_index_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/sign_timestamp_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/signature_timestamp_full_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/storage_protection_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/storage_quota_guard_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/api/workflow_borrow_destroy_spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/delivery_v2.spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/ui/search_preview.spec.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `tests/playwright/utils/auth.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `vite.config.ts` | 修改 | 补齐脚本/源码头注释 | 否 |
| `.agent/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `.agent/knowledge/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `.agent/rules/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `.agent/workflows/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `.claude/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `.github/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `.github/workflows/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `.vscode/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `.vscode/settings.json` | 新增 | 补齐文件头注释 | 否 |
| `data/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/helm/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/helm/templates/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/monitoring/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/monitoring/grafana/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/monitoring/grafana/dashboards/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/offline/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/offline/bin/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/offline/config/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/offline/db/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/offline/db/migration/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/offline/docs/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/offline/frontend/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/offline/frontend/assets/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `deploy/tools/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `docs/api/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `docs/architecture/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `docs/change-list.md` | 新增 | 新增变更清单输出 | 否 |
| `docs/database/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `docs/deployment/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `docs/guides/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `docs/images/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `docs/implementation/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `docs/planning/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `docs/references/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `docs/troubleshooting/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/scripts/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/annotation/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/aspect/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/constant/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/enums/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/exception/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/common/result/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/config/mybatis/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/controller/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/aip/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/monitoring/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/notification/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/parser/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/reconciliation/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/relation/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/request/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/response/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/search/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/signature/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/sip/report/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/stats/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/dto/workflow/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/engine/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/enums/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/entity/es/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/event/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/core/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/core/connector/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/core/context/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/core/model/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/controller/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/impl/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/mock/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/service/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/client/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/connector/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/controller/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/event/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/mapper/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/security/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/service/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/listener/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/mapper/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/repository/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/repository/es/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/security/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/adapter/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/adapter/impl/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/converter/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/ofd/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/parser/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/parser/impl/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/search/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/signature/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/strategy/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/service/strategy/impl/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/java/com/nexusarchive/util/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/resources/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/resources/db/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/resources/db/demo/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/resources/db/migration/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/resources/fonts/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/resources/keystore/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/resources/sql/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/main/resources/templates/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/java/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/java/com/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/controller/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/integration/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/listener/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/service/impl/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/util/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/java/com/nexusarchive/verification/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/resources/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/resources/db/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/resources/db/migration/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `nexusarchive-java/src/test/resources/mockito-extensions/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `public/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `scripts/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/__tests__/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/__tests__/api/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/__tests__/components/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/__tests__/permissions/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/__tests__/store/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/api/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/components/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/components/admin/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/components/archive/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/components/auth/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/components/common/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/components/debug/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/components/layout/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/components/org/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/components/panorama/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/components/settings/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/data/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/features/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/features/archives/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/features/borrowing/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/features/compliance/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/features/settings/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/hooks/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/layouts/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/routes/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/store/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `src/utils/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `tests/fixtures/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `tests/playwright/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `tests/playwright/api/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `tests/playwright/ui/README.md` | 新增 | 目录 MD 缺失 | 否 |
| `tests/playwright/utils/README.md` | 新增 | 目录 MD 缺失 | 否 |
