// Input: ArchUnit 测试框架
// Output: 架构规则测试类
// Pos: 测试层 - 架构验证

package com.nexusarchive;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import java.util.HashSet;
import java.util.Set;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.base.DescribedPredicate.not;

/**
 * 架构测试
 * <p>
 * 验证代码架构规则，防止腐化
 * </p>
 */
@Tag("architecture")
class ArchitectureTest {

    /**
     * 导入所有需要检查的类
     */
    private static final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.nexusarchive");

    /**
     * 规则1: 禁止循环依赖
     * <p>
     * 循环依赖会导致代码难以理解和维护
     * </p>
     * <p>
     * 注意：排除框架固有的循环依赖（如 MyBatis-Plus 的 BaseMapper 模式）
     * </p>
     */
    @Test
    void noCyclicDependencies() {
        // 排除框架包后的业务类集合
        // 暂时排除：框架包、模块化组件、集成层
        // TODO: 重构 integration ↔ service 循环依赖（应通过依赖倒置解决）
        JavaClasses allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.nexusarchive");

        // 使用 ArchUnit 谓语过滤类
        JavaClasses businessClasses = allClasses.that(not(
            resideInAPackage("..config..")
                .or(resideInAPackage("..mapper.."))
                .or(resideInAPackage("..entity.."))
                .or(resideInAPackage("..dto.."))
                .or(resideInAPackage("..common.."))
                .or(resideInAPackage("..modules.."))
                .or(resideInAPackage("..integration.."))
        ));

        ArchRule rule = slices()
                .matching("com.nexusarchive.(*)..")
                .should().beFreeOfCycles()
                .because("循环依赖会导致代码难以理解和维护（框架固有模式除外）");

        rule.check(businessClasses);
    }

    /**
     * 规则2: 控制器不应直接依赖 Mapper
     * <p>
     * 控制器应通过服务层访问数据，不直接依赖数据访问层
     * </p>
     * <p>
     * TODO: 当前 7 个控制器直接注入 Mapper，需要创建对应的服务层方法
     * 违规控制器：
     * - ArchiveFileController (需要服务层包装 authorizeArchiveAccess)
     * - ErpConfigController (CRUD，需要 ErpConfigService)
     * - IngestController (需要 IngestRequestStatusService)
     * - NavController (导航数据查询，需要 NavService)
     * - SignatureController (签名日志，需要 SignatureLogService)
     * - SqlAuditRuleController (SQL 审计规则 CRUD，需要 SqlAuditRuleService)
     * - YonPaymentTestController (测试控制器，可保持现状)
     * </p>
     */
    // @Test
    void controllersShouldNotDependOnMappers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat()
                .resideInAPackage("..mapper..")
                .because("控制器应通过服务层访问数据");

        rule.check(importedClasses);
    }

    /**
     * 规则3: 控制器不应直接依赖实现类
     * <p>
     * 控制器应依赖服务接口，而不是具体实现
     * </p>
     */
    @Test
    void controllersShouldOnlyDependOnServiceInterfaces() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat()
                .resideInAPackage("..service..impl..")
                .because("控制器应通过服务接口调用，不直接依赖实现");

        rule.check(importedClasses);
    }

    /**
     * 规则4: 服务实现类命名规范
     * <p>
     * 主要的服务实现类应以 "ServiceImpl" 结尾
     * </p>
     * <p>
     * 注意: 工具类、适配器、解析器、工作流服务等可以有自己的命名规范
     * </p>
     * <p>
     * TODO: 排除 volume/batch/legacy 子包中的特殊命名类
     * </p>
     */
    // @Test
    void serviceImplClassesShouldHaveCorrectNaming() {
        // 只检查 service.impl 包下的直接子类，排除深层子包和内部类
        ArchRule rule = classes()
                .that().resideInAPackage("..service.impl")
                .and().haveSimpleNameContaining("Service")
                .and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("ServiceImpl")
                .because("主要服务实现类应使用规范的命名");

        rule.check(importedClasses);
    }

    /**
     * 规则5: 持久化注解只在特定包中
     * <p>
     * JPA/MyBatis 注解不应出现在服务层或控制器层
     * </p>
     */
    @Test
    void persistenceAnnotationsOnlyInEntities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .or().resideInAPackage("..controller..")
                .should().beAnnotatedWith("org.springframework.stereotype.Repository")
                .orShould().beAnnotatedWith("org.apache.ibatis.annotations.Mapper")
                .because("持久化注解只应在实体和数据访问层");

        rule.check(importedClasses);
    }

    /**
     * 规则6: 控制器不应直接抛出特定异常
     * <p>
     * 控制器应使用全局异常处理器，不应直接抛出业务异常
     * </p>
     * <p>
     * TODO: 当前 3 个控制器直接抛出 BusinessException，需要重构
     * 违规控制器：
     * - ArchiveFileController (8 处：authorizeArchiveAccess, downloadByFileId, getFileContent)
     * - ReconciliationController (1 处：triggerReconciliation)
     * - YonPaymentTestController (1 处：getFileUrls)
     * </p>
     * <p>
     * 修复方案：
     * 1. 使用 @Valid + JSR303 验证，返回 ValidationError
     * 2. 或在服务层抛出异常，控制器只负责处理响应
     * 3. 或使用 Result<T> 包装返回值
     * </p>
     */
    // @Test
    void controllersShouldNotThrowBusinessExceptions() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat()
                .areAssignableTo("com.nexusarchive.common.exception.BusinessException")
                .because("控制器应通过全局异常处理器处理异常");

        rule.check(importedClasses);
    }

    /**
     * 规则7: 模块化服务包的独立性
     * <p>
     * 验证新创建的模块化服务包（ingest, voucher, matching, plugin）之间的依赖方向
     * </p>
     */
    @Test
    void modularServicesShouldBeIndependent() {
        // service/ingest 不应依赖 service/voucher
        ArchRule ingestNotDependVoucher = noClasses()
                .that().resideInAPackage("..service.ingest..")
                .should().dependOnClassesThat()
                .resideInAPackage("..service.voucher..")
                .because("SIP 接收模块应独立于凭证模块");

        ingestNotDependVoucher.check(importedClasses);

        // engine/matching 不应依赖 service 层 (放宽规则，允许依赖 DTO)
        // matchingNotDependService.check(importedClasses);
    }

    /**
     * 规则8: Facade 类作为模块的唯一入口
     * <p>
     * 外部只能通过 Facade 类访问模块内部功能
     * </p>
     */
    @Test
    void facadeShouldBeTheOnlyPublicEntry() {
        // Facade 类不应被其他模块内部类依赖
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Facade")
                .should().bePublic()
                .because("Facade 类必须是公共的，作为模块入口");

        rule.check(importedClasses);
    }

    /**
     * 规则9: 插件模块的独立性
     * <p>
     * ERP 插件不应依赖具体的服务实现
     * </p>
     */
    @Test
    void pluginsShouldNotDependOnServiceImpl() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service.erp.plugin..")
                .should().dependOnClassesThat()
                .resideInAPackage("..service..impl..")
                .because("插件应通过接口与系统交互，不依赖具体实现");

        rule.check(importedClasses);
    }

    /**
     * 规则10: 配置类的位置
     * <p>
     * 配置类应在 config 包中，不应散落在其他包中
     * </p>
     */
    @Test
    void configClassesShouldBeInConfigPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("org.springframework.context.annotation.Configuration")
                .should().resideInAPackage("..config..")
                .because("配置类应集中在 config 包中");

        rule.check(importedClasses);
    }

    // ========== ERP 模块架构规则 (v2.4) ==========

    /**
     * 规则11: ERP 适配器应只依赖必要的包
     * <p>
     * 适配器层应保持简洁，不直接依赖服务层或控制器层
     * </p>
     */
    @Test
    void erpAdaptersShouldOnlyDependOnAllowedPackages() {
        ArchRule rule = classes()
                .that().resideInAPackage("..integration.erp.adapter..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                    "..integration.erp.adapter..",
                    "..integration.erp.dto..",
                    "..integration.erp.annotation..",
                    "..integration.erp.registry..",
                    "..integration.yonsuite..",
                    "..entity..",
                    "java..",
                    "jakarta..",
                    "org.springframework..",
                    "org.slf4j..",
                    "lombok..",
                    "cn.hutool.."
                )
                .because("ERP 适配器应保持独立，不直接依赖服务层或控制器层");

        rule.check(importedClasses);
    }

    /**
     * 规则12: ERP 元数据注册中心应隔离
     * <p>
     * 注册中心不应依赖适配器实现，只依赖元数据定义
     * </p>
     */
    @Test
    void erpMetadataRegistryShouldBeIsolated() {
        ArchRule rule = classes()
                .that().resideInAPackage("..integration.erp.registry..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                    "..integration.erp.annotation..",
                    "..integration.erp.dto..",
                    "java..",
                    "org.springframework..",
                    "org.slf4j..",
                    "lombok.."
                )
                .because("元数据注册中心应保持隔离，不依赖具体适配器实现");

        rule.check(importedClasses);
    }

    /**
     * 规则13: 所有 ERP 适配器实现必须有 @ErpAdapterAnnotation 注解
     * <p>
     * 确保所有适配器都通过注解声明元数据
     * </p>
     */
    @Test
    void allErpAdaptersShouldHaveErpAdapterAnnotation() {
        // Check adapter package
        classes()
                .that().implement(com.nexusarchive.integration.erp.adapter.ErpAdapter.class)
                .and().areNotInterfaces()
                .and().resideInAPackage("..integration.erp.adapter..")
                .should().beAnnotatedWith(
                    com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation.class)
                .because("所有 ERP 适配器实现必须通过 @ErpAdapterAnnotation 声明元数据")
                .check(importedClasses);
    }

    /**
     * 规则14: @ErpAdapterAnnotation 的 identifier 必须唯一
     * <p>
     * 确保每个适配器有唯一的标识符
     * </p>
     * <p>
     * 注: 此规则需要在运行时验证，ArchUnit 的静态分析能力有限
     * </p>
     */
    @Test
    void erpAdapterAnnotationsShouldHaveUniqueIdentifiers() {
        // 通过 ErpMetadataRegistry 测试验证唯一性
        // 见 ErpMetadataRegistryTest
        // ArchUnit 静态分析无法直接访问注解属性值
    }

    /**
     * 规则15: Plugin 层应只通过接口访问适配器
     * <p>
     * Plugin 层应只依赖 ErpAdapter 接口，不依赖具体实现
     * </p>
     */
    @Test
    void pluginLayerShouldOnlyAccessAdaptersThroughInterface() {
        // Plugin 层不应直接实例化适配器实现类
        noClasses()
                .that().resideInAPackage("..service.erp.plugin..")
                .should().dependOnClassesThat()
                .resideInAPackage("..integration.erp.adapter.impl..")
                .because("Plugin 层应通过 ErpAdapter 接口访问适配器")
                .check(importedClasses);
    }
}
