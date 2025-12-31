// Input: Java Reflection, Properties
// Output: Masking implementation
// Pos: NexusCore masking
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.masking;

import com.nexusarchive.core.masking.DataMaskingProperties.MaskingRule;
import com.nexusarchive.core.masking.DataMaskingProperties.MaskPattern;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDataMaskingService implements DataMaskingService {

    private final DataMaskingProperties properties;

    @Override
    public String mask(String fieldName, String value) {
        if (!properties.isEnabled() || !StringUtils.hasText(value)) {
            return value;
        }

        for (MaskingRule rule : properties.getRules()) {
            // 简单包含匹配或正则匹配 (这里简化为包含/相等匹配，或者正则)
            // 假设 fieldMatch 是字段名精确匹配或正则
            if (fieldName.matches(rule.getFieldMatch())) {
                return applyPattern(value, rule.getPattern(), rule.getMaskChar());
            }
        }
        return value;
    }

    @Override
    public <T> T maskObject(T object) {
        if (object == null || !properties.isEnabled()) {
            return object;
        }

        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getType().equals(String.class)) {
                try {
                    String original = (String) field.get(object);
                    if (StringUtils.hasText(original)) {
                        String masked = mask(field.getName(), original);
                        if (!original.equals(masked)) {
                            field.set(object, masked); // 原地修改
                        }
                    }
                } catch (IllegalAccessException e) {
                    log.error("Failed to mask field: {}", field.getName(), e);
                }
            }
        }
        return object;
    }

    @Override
    public <T> List<T> maskList(List<T> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(this::maskObject).collect(Collectors.toList());
    }

    private String applyPattern(String value, MaskPattern pattern, String maskChar) {
        if (value == null) {
            return null;
        }
        int len = value.length();

        switch (pattern) {
            case MIDDLE_4:
                // 保留前3后4 (如果长度不够，则全掩码或保留首尾)
                // 常见手机号 11位: 138 **** 1234 (保留前3后4) => 实际上 pattern 命名可能混淆
                // 假设 MIDDLE_4 是隐藏中间4位? 还是保留中间4位? 通常是 "MASK MIDDLE 4".
                // 这里实现：中间4位隐藏。
                if (len <= 4) {
                    return repeat(maskChar, len);
                }
                int start = (len - 4) / 2;
                return value.substring(0, start) + repeat(maskChar, 4) + value.substring(start + 4);
                
            case MIDDLE_8:
                if (len <= 8) {
                    return repeat(maskChar, len);
                }
                start = (len - 8) / 2;
                return value.substring(0, start) + repeat(maskChar, 8) + value.substring(start + 8);

            case KEEP_3_4:
                // 保留前3后4，中间全部隐藏
                if (len <= 7) {
                    return repeat(maskChar, len);
                }
                return value.substring(0, 3) + repeat(maskChar, len - 7) + value.substring(len - 4);
            
            case KEEP_FIRST_LAST:
                if (len <= 2) {
                    return repeat(maskChar, len);
                }
                 return value.charAt(0) + repeat(maskChar, len - 2) + value.charAt(len - 1);
                 
            case FULL:
            default:
                return repeat(maskChar, len);
        }
    }

    private String repeat(String s, int count) {
        return s.repeat(Math.max(0, count));
    }
}
