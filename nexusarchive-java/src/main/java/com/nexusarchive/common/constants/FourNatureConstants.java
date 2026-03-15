// Input: Java 标准库
// Output: FourNatureConstants 类
// Pos: 常量定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.common.constants;

/**
 * 四性检测常量
 * <p>
 * 用于四性检测（真实性、完整性、可用性、安全性）的统一常量定义
 * </p>
 *
 * @see com.nexusarchive.service.FourNatureCoreService
 */
public final class FourNatureConstants {

    private FourNatureConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 检测类型名称
     */
    public static final class CheckType {
        /** 去重检测 */
        public static final String DEDUPLICATION = "De-duplication Check";

        /** 真实性检测 */
        public static final String AUTHENTICITY = "Authenticity Check";

        /** 完整性检测 */
        public static final String INTEGRITY = "Integrity Check";

        /** 可用性检测 */
        public static final String USABILITY = "Usability Check";

        /** 安全性检测 */
        public static final String SAFETY = "Safety Check";

        private CheckType() {
        }
    }

    /**
     * 检测通过消息
     */
    public static final class SuccessMessage {
        /** 真实性检测通过 - 所有文件已验证 */
        public static final String AUTHENTICITY_PASSED = "All files verified";

        /** 完整性检测通过 */
        public static final String INTEGRITY_PASSED = "Metadata and structure valid";

        /** 可用性检测通过 - 所有文件可用 */
        public static final String USABILITY_PASSED = "All files usable";

        /** 安全性检测通过 */
        public static final String SAFETY_PASSED = "No threats";

        /** 文件可访问 */
        public static final String FILES_ACCESSIBLE = "Files accessible";

        /** 文件格式有效 */
        public static final String FORMAT_VALID = "File format valid";

        /** 哈希验证通过 */
        public static final String HASH_VERIFIED = "Hash verification passed";
        
        /** 去重通过 */
        public static final String DEDUPLICATION_PASSED = "No duplicates found";

        private SuccessMessage() {
        }
    }
    
    /**
     * 常用提示语
     */
    public static final class Prompt {
        public static final String MISSING_FONDS_CODE = "Missing Fonds Code";
        public static final String MISSING_VOUCHER_NUMBER = "Missing Voucher Number";
        public static final String MISSING_ACCOUNT_PERIOD = "Missing Account Period";
        public static final String MISSING_CONTENT = "Missing content: ";
        public static final String DUPLICATE_FILE = "Duplicate file detected: %s (Hash: %s already exists)";
    }
}
