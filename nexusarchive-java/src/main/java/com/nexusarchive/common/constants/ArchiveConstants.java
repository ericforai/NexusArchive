package com.nexusarchive.common.constants;

/**
 * 档案元数据与业务常量 (Ref: DA/T 94-2022)
 */
public final class ArchiveConstants {

    private ArchiveConstants() {}

    /**
     * 元数据字段名 (数据库/JSON 键名)
     */
    public static final class Fields {
        public static final String FONDS_CODE = "fonds_code";
        public static final String ARCHIVAL_CODE = "archival_code";
        public static final String CATEGORY_CODE = "category_code";
        public static final String TITLE = "title";
        public static final String FISCAL_YEAR = "fiscal_year";
        public static final String FISCAL_PERIOD = "fiscal_period";
        public static final String RETENTION_PERIOD = "retention_period";
        public static final String CREATOR = "creator";
        public static final String UNIQUE_BIZ_ID = "unique_biz_id";
        public static final String FILE_HASH = "file_hash";
        public static final String ORIGINAL_HASH = "original_hash";
        public static final String DESTRUCTION_STATUS = "destruction_status";
        public static final String PRE_ARCHIVE_STATUS = "pre_archive_status";
        public static final String STORAGE_PATH = "storage_path";
        public static final String FILE_NAME = "file_name";
    }

    /**
     * 会计档案类别
     */
    public static final class Categories {
        /** 会计凭证 */
        public static final String VOUCHER = "AC01";
        /** 会计账簿 */
        public static final String BOOK = "AC02";
        /** 财务报告 */
        public static final String REPORT = "AC03";
        /** 其他 */
        public static final String OTHERS = "AC04";
    }

    /**
     * 保管期限
     */
    public static final class Retention {
        public static final String Y10 = "10Y";
        public static final String Y30 = "30Y";
        public static final String PERMANENT = "PERMANENT";
    }

    /**
     * 密级
     */
    public static final class SecurityLevel {
        public static final String INTERNAL = "INTERNAL";
        public static final String SECRET = "SECRET";
        public static final String TOP_SECRET = "TOP_SECRET";
        public static final String PUBLIC = "PUBLIC";
    }

    /**
     * 来源渠道
     */
    public static final class SourceChannel {
        public static final String WEB_UPLOAD = "Web上传";
        public static final String ERP_SYNC = "ERP同步";
        public static final String LEGACY_IMPORT = "历史数据导入";
    }

    /**
     * 哈希算法
     */
    public static final class Algorithms {
        public static final String SM3 = "SM3";
        public static final String SHA256 = "SHA-256";
    }
}
