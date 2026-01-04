// Input: 模块清单注解定义
// Output: J1 Self-Description - 每个模块声明自己的边界和所有者
// Pos: 架构防御 - 自描述架构

package com.nexusarchive.annotation.architecture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模块清单注解
 * <p>
 * 用于声明模块的身份、所有权和依赖规则。
 * 实现 Architecture Defense 的 J1 (Self-Description) 原则。
 * </p>
 *
 * <h3>用法示例：</h3>
 * <pre>{@code
 * @ModuleManifest(
 *     id = "feature.archive-management",
 *     name = "Archive Management Module",
 *     owner = "team-backend",
 *     layer = Layer.SERVICE,
 *     description = "负责档案的核心业务逻辑"
 * )
 * package com.nexusarchive.service;
 * }</pre>
 *
 * @see Layer
 * @see DependencyRule
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleManifest {

    /**
     * 模块唯一标识符
     * <p>
     * 格式: [layer].[feature].[component]
     * 例如: service.archive, controller.user, integration.erp.yonsuite
     * </p>
     */
    String id();

    /**
     * 模块名称
     */
    String name() default "";

    /**
     * 模块所有者
     * <p>
     * 谁对这个模块的架构健康负责
     * </p>
     */
    String owner();

    /**
     * 模块所属层级
     */
    Layer layer();

    /**
     * 模块描述
     */
    String description() default "";

    /**
     * 公共 API 入口
     * <p>
     * 其他模块只能通过这些类访问此模块
     * </p>
     */
    String[] publicApi() default {};

    /**
     * 依赖规则
     * <p>
     * 声明此模块可以依赖哪些其他模块
     * </p>
     */
    DependencyRule[] dependencies() default {};

    /**
     * 模块标签
     * <p>
     * 用于分类和过滤，例如: "legacy", "experimental", "core"
     * </p>
     */
    String[] tags() default {};

    /**
     * 是否为遗留代码
     * <p>
     * 标记为遗留的模块会在架构报告中特别标注
     * </p>
     */
    boolean legacy() default false;

    /**
     * 合规目标日期
     * <p>
     * 对于遗留代码，设定重构完成日期
     * </p>
     */
    String complianceTarget() default "";

    /**
     * 例外说明
     * <p>
     * 仅用于紧急情况，需要说明原因和复审日期
     * </p>
     */
    String exceptionReason() default "";

    /**
     * 复审日期
     * <p>
     * 格式: yyyy-MM-dd
     * </p>
     */
    String reviewDate() default "";

    /**
     * 模块层级
     */
    enum Layer {
        /** 控制器层 - API 入口 */
        CONTROLLER("controller", 1),
        /** 服务层 - 业务逻辑 */
        SERVICE("service", 2),
        /** 数据访问层 - 持久化 */
        MAPPER("mapper", 3),
        /** 实体层 - 领域模型 */
        ENTITY("entity", 4),
        /** 集成层 - 外部系统 */
        INTEGRATION("integration", 2),
        /** 工具层 - 通用功能 */
        UTIL("util", 5),
        /** 配置层 - 框架配置 */
        CONFIG("config", 0);

        private final String packageName;
        private final int level;

        Layer(String packageName, int level) {
            this.packageName = packageName;
            this.level = level;
        }

        public String getPackageName() {
            return packageName;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * 依赖规则
     */
    @interface DependencyRule {
        /**
         * 可以依赖的包模式
         * <p>
         * 支持通配符，例如:
         * - "..service.." - 服务层
         * - "java..", "jakarta.." - JDK
         * - "org.springframework.." - Spring
         * </p>
         */
        String[] allowedPackages();

        /**
         * 禁止依赖的包模式
         */
        String[] forbiddenPackages() default {};

        /**
         * 规则说明
         */
        String description() default "";
    }
}
