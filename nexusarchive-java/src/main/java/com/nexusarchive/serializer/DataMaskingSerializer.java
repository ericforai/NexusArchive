// Input: Jackson、Spring Security
// Output: DataMaskingSerializer 序列化器类
// Pos: 序列化器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collection;

/**
 * 数据脱敏序列化器
 * 
 * 在 JSON 序列化时自动对敏感字段进行脱敏
 * 
 * PRD 来源: Section 2.1 - 高级检索与脱敏
 */
public class DataMaskingSerializer extends JsonSerializer<String> {
    
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        // 检查用户权限
        if (hasFullAccess()) {
            // 有 FULL_ACCESS 权限，不脱敏
            gen.writeString(value);
            return;
        }
        
        // 检查字段名（通过序列化上下文获取）
        String fieldName = gen.getOutputContext().getCurrentName();
        if (fieldName != null && isSensitiveField(fieldName)) {
            // 对敏感字段进行脱敏
            String maskedValue = maskValue(value);
            gen.writeString(maskedValue);
        } else {
            gen.writeString(value);
        }
    }
    
    /**
     * 检查用户是否有 FULL_ACCESS 权限
     */
    private boolean hasFullAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("FULL_ACCESS") || 
                           auth.getAuthority().equals("archive:full_access"));
    }
    
    /**
     * 判断字段是否为敏感字段
     */
    private boolean isSensitiveField(String fieldName) {
        String lowerFieldName = fieldName.toLowerCase();
        return lowerFieldName.contains("bank") && lowerFieldName.contains("account") ||
               lowerFieldName.contains("account") && lowerFieldName.contains("number") ||
               lowerFieldName.contains("card") && lowerFieldName.contains("number") ||
               lowerFieldName.contains("idcard") ||
               lowerFieldName.contains("phone") ||
               lowerFieldName.contains("email") ||
               lowerFieldName.contains("password") ||
               lowerFieldName.contains("secret");
    }
    
    /**
     * 对值进行脱敏处理（中间8位替换为星号）
     */
    private String maskValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        int length = value.length();
        if (length <= 8) {
            // 长度小于等于8，全部脱敏
            return "********";
        }
        
        // 保留前4位和后4位，中间替换为8个星号
        int prefixLength = 4;
        int suffixLength = 4;
        
        if (length <= prefixLength + suffixLength) {
            return "********";
        }
        
        return value.substring(0, prefixLength) + "********" + 
               value.substring(length - suffixLength);
    }
}


