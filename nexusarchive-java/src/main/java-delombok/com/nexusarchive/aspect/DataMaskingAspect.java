// Input: Spring AOP、Jackson、DataMasking 注解
// Output: DataMaskingAspect 切面类
// Pos: AOP 切面
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexusarchive.annotation.DataMasking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 数据脱敏切面
 * 
 * 功能：
 * 1. 拦截 Controller 方法返回值
 * 2. 检查用户权限（是否有 FULL_ACCESS）
 * 3. 对敏感字段进行脱敏处理
 * 
 * PRD 来源: Section 2.1 - 高级检索与脱敏
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataMaskingAspect {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 拦截标记了 @DataMasking 的方法
     */
    @Around("@annotation(dataMasking)")
    public Object maskData(ProceedingJoinPoint joinPoint, DataMasking dataMasking) throws Throwable {
        // 1. 执行原方法
        Object result = joinPoint.proceed();
        
        // 2. 检查用户权限
        if (hasFullAccess()) {
            // 有 FULL_ACCESS 权限，不脱敏
            return result;
        }
        
        // 3. 对返回值进行脱敏处理
        return applyMasking(result, dataMasking);
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
     * 应用脱敏规则
     */
    private Object applyMasking(Object result, DataMasking dataMasking) {
        if (result == null) {
            return null;
        }
        
        try {
            // 将结果转换为 JSON 节点
            String json = objectMapper.writeValueAsString(result);
            ObjectNode rootNode = (ObjectNode) objectMapper.readTree(json);
            
            // 递归处理所有节点
            maskNode(rootNode, dataMasking);
            
            // 转换回对象
            return objectMapper.treeToValue(rootNode, result.getClass());
        } catch (Exception e) {
            log.warn("数据脱敏处理失败，返回原始数据", e);
            return result;
        }
    }
    
    /**
     * 递归处理 JSON 节点
     */
    private void maskNode(com.fasterxml.jackson.databind.JsonNode node, DataMasking dataMasking) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                com.fasterxml.jackson.databind.JsonNode fieldValue = entry.getValue();
                
                // 检查字段名是否包含敏感关键词
                if (isSensitiveField(fieldName)) {
                    String maskedValue = maskValue(fieldValue.asText(), dataMasking);
                    objectNode.put(fieldName, maskedValue);
                } else if (fieldValue.isObject() || fieldValue.isArray()) {
                    // 递归处理嵌套对象和数组
                    maskNode(fieldValue, dataMasking);
                }
            });
        } else if (node.isArray()) {
            node.forEach(child -> maskNode(child, dataMasking));
        }
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
     * 对值进行脱敏处理
     */
    private String maskValue(String value, DataMasking dataMasking) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        int length = value.length();
        DataMasking.MaskingType type = dataMasking.type();
        
        switch (type) {
            case ALL:
                return dataMasking.maskChar();
            case PREFIX_ONLY:
                if (length <= dataMasking.prefixLength()) {
                    return value;
                }
                return value.substring(0, dataMasking.prefixLength()) + dataMasking.maskChar();
            case SUFFIX_ONLY:
                if (length <= dataMasking.suffixLength()) {
                    return value;
                }
                return dataMasking.maskChar() + value.substring(length - dataMasking.suffixLength());
            case MIDDLE:
            default:
                if (length <= dataMasking.prefixLength() + dataMasking.suffixLength()) {
                    return dataMasking.maskChar();
                }
                return value.substring(0, dataMasking.prefixLength()) + 
                       dataMasking.maskChar() + 
                       value.substring(length - dataMasking.suffixLength());
        }
    }
}





