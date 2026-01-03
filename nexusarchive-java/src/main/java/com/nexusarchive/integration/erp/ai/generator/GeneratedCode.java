// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/generator/GeneratedCode.java
// Input: Scenario mappings
// Output: Generated Java code
// Pos: AI 模块 - 代码生成器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.generator;

import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 生成的代码
 * <p>
 * 包含生成的适配器类、DTO类、测试类等
 * </p>
 */
@Data
@Builder
public class GeneratedCode {

    /**
     * 适配器主类
     */
    private String adapterClass;

    /**
     * 类名
     */
    private String className;

    /**
     * 包名
     */
    private String packageName;

    /**
     * ERP 类型标识
     */
    private String erpType;

    /**
     * ERP 名称
     */
    private String erpName;

    /**
     * DTO 类列表
     */
    private List<DtoClass> dtoClasses;

    /**
     * 单元测试类
     */
    private String testClass;

    /**
     * 集成配置 SQL
     */
    private String configSql;

    /**
     * 场景映射列表
     */
    private List<BusinessSemanticMapper.ScenarioMapping> mappings;

    /**
     * DTO 类
     */
    @Data
    @Builder
    public static class DtoClass {
        private String className;
        private String packageName;
        private String code;
    }
}
