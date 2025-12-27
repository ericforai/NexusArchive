// Input: Spring Framework、MyBatis-Plus、Java 标准库
// Output: RuleTemplateManager 模板管理器
// Pos: 匹配引擎/核心
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.engine.matching.dto.RuleTemplate;
import com.nexusarchive.engine.matching.enums.BusinessScene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则模板管理器
 * 
 * 负责加载、缓存和管理匹配规则模板。
 * 采用简单模式：启动时加载，通过"应用规则"按钮手动刷新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleTemplateManager {
    
    private final JdbcTemplate jdbcTemplate;
    
    // 模板缓存：templateId -> RuleTemplate
    private final Map<String, RuleTemplate> templateCache = new ConcurrentHashMap<>();
    
    // 场景到模板的映射：scene -> templateId
    private final Map<BusinessScene, String> sceneTemplateMap = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        loadTemplates();
    }
    
    /**
     * 加载所有启用的模板
     */
    public void loadTemplates() {
        try {
            String sql = "SELECT id, name, version, scene, config FROM match_rule_template WHERE status = 'ACTIVE'";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            
            // 使用临时 Map，避免并发问题
            Map<String, RuleTemplate> newTemplateCache = new HashMap<>();
            Map<BusinessScene, String> newSceneTemplateMap = new HashMap<>();
            
            for (Map<String, Object> row : rows) {
                String id = (String) row.get("id");
                String name = (String) row.get("name");
                String version = (String) row.get("version");
                String sceneStr = (String) row.get("scene");
                Object configObj = row.get("config");
                
                RuleTemplate template = RuleTemplate.builder()
                    .id(id)
                    .name(name)
                    .version(version)
                    .scene(sceneStr)
                    .config(configObj != null ? configObj.toString() : "{}")
                    .build();
                
                newTemplateCache.put(id, template);
                
                try {
                    BusinessScene scene = BusinessScene.valueOf(sceneStr);
                    newSceneTemplateMap.put(scene, id);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown scene: {}", sceneStr);
                }
            }
            
            // 原子替换
            templateCache.clear();
            templateCache.putAll(newTemplateCache);
            sceneTemplateMap.clear();
            sceneTemplateMap.putAll(newSceneTemplateMap);
            
            log.info("Loaded {} matching templates", templateCache.size());
        } catch (Exception e) {
            log.warn("Failed to load templates (table may not exist yet): {}", e.getMessage());
        }
    }
    
    /**
     * 手动刷新模板（管理员操作）
     */
    public void reloadTemplates() {
        loadTemplates();
        log.info("Templates reloaded by admin");
    }
    
    /**
     * 获取模板
     */
    public RuleTemplate getTemplate(String templateId) {
        return templateCache.get(templateId);
    }
    
    /**
     * 根据业务场景获取模板
     */
    public RuleTemplate getTemplateByScene(BusinessScene scene) {
        String templateId = sceneTemplateMap.get(scene);
        RuleTemplate template = null;
        
        if (templateId != null) {
            template = templateCache.get(templateId);
        }
        
        // 兜底：返回手工模板或创建一个空模板
        if (template == null) {
            template = templateCache.get("T00_MANUAL");
        }
        if (template == null) {
            template = RuleTemplate.builder()
                .id("T00_MANUAL")
                .name("手工关联")
                .version("1.0.0")
                .scene("UNKNOWN")
                .config("{}")
                .build();
        }
        
        return template;
    }
    
    /**
     * 获取所有模板
     */
    public List<RuleTemplate> getAllTemplates() {
        return List.copyOf(templateCache.values());
    }
}
