// Input: Spring Boot Configuration
// Output: 脱敏规则配置
// Pos: NexusCore masking
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.masking;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 动态数据脱敏配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "nexus.masking")
public class DataMaskingProperties {
    
    private boolean enabled = true;
    
    private List<MaskingRule> rules = new ArrayList<>();
    
    @Data
    public static class MaskingRule {
        /** 字段名正则匹配 (如 bank_account) */
        private String fieldMatch;
        
        /** 脱敏模式 */
        private MaskPattern pattern;
        
        /** 自定义遮罩符 (默认 *) */
        private String maskChar = "*";
    }
    
    public enum MaskPattern {
        MIDDLE_4,      // 隐藏中间4位 (手机号)
        MIDDLE_8,      // 隐藏中间8位 (银行卡)
        KEEP_3_4,      // 保留前3后4 (身份证)
        KEEP_FIRST_LAST, // 保留首尾 (姓名)
        FULL           // 全遮蔽
    }
}
