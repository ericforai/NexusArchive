// Input: Lombok、Java 标准库、匹配引擎枚举
// Output: MatchingContext 类
// Pos: 匹配引擎/核心
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import lombok.Data;

import java.util.Map;

/**
 * 匹配上下文
 * 
 * 保证单次匹配生命周期内规则版本绝对一致
 */
@Data
public class MatchingContext {
    
    private String batchId;
    private String templateId;
    private String templateVersion;
    private Map<String, String> accountRoleMap;
    private Map<String, String> docTypeMap;
    private String configHash;
    
    /**
     * 创建匹配上下文
     */
    public static MatchingContext create(
            String templateId,
            String templateVersion,
            Map<String, String> accountRoleMap,
            Map<String, String> docTypeMap) {
        
        MatchingContext ctx = new MatchingContext();
        ctx.batchId = IdUtil.fastSimpleUUID();
        ctx.templateId = templateId;
        ctx.templateVersion = templateVersion;
        ctx.accountRoleMap = accountRoleMap;
        ctx.docTypeMap = docTypeMap;
        
        // 生成配置指纹
        String configStr = templateVersion + 
            JSONUtil.toJsonStr(accountRoleMap) + 
            JSONUtil.toJsonStr(docTypeMap);
        ctx.configHash = DigestUtil.sha256Hex(configStr);
        
        return ctx;
    }
}
