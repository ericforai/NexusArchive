package com.nexusarchive.common.constants;

/**
 * 系统状态常量定义
 */
public final class StatusConstants {

    private StatusConstants() {}

    /**
     * 档案生命周期状态
     */
    public static final class Archive {
        public static final String DRAFT = "DRAFT";
        public static final String PENDING = "PENDING";
        public static final String ARCHIVED = "ARCHIVED";
        /** 已接收 */
        public static final String RECEIVED = "RECEIVED";
        /** 已取消 */
        public static final String CANCELLED = "CANCELLED";
        /** 待匹配 */
        public static final String MATCH_PENDING = "MATCH_PENDING";
    }

    /**
     * 预归档/电子凭证池状态
     */
    public static final class PreArchive {
        public static final String PENDING_CHECK = "PENDING_CHECK";
        public static final String PENDING_METADATA = "PENDING_METADATA";
        public static final String CHECKING = "CHECKING";
        public static final String CHECK_PASSED = "CHECK_PASSED";
        public static final String CHECK_FAILED = "CHECK_FAILED";
        public static final String SUBMITTED = "SUBMITTED";
        /** 已完成/已归档 */
        public static final String COMPLETED = "COMPLETED";
    }

    /**
     * 销毁审批状态
     */
    public static final class Destruction {
        /** 待鉴定 */
        public static final String APPRAISING = "APPRAISING";
        /** 初审通过 */
        public static final String FIRST_APPROVED = "FIRST_APPROVED";
        /** 销毁已批准 */
        public static final String DESTRUCTION_APPROVED = "DESTRUCTION_APPROVED";
        /** 已销毁 */
        public static final String DESTROYED = "DESTROYED";
        /** 已过期（待处置） */
        public static final String EXPIRED = "EXPIRED";
        /** 冻结/留置 */
        public static final String HOLD = "HOLD";
    }
}
