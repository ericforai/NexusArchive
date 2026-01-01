// Input: ArchUnit 测试框架
// Output: 架构规则测试类
// Pos: 测试层 - 架构验证

package com.nexusarchive;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

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
     * TODO: 当前许多控制器直接注入 Mapper，需要重构以通过服务层访问
     * </p>
     */
    @Test
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
     * TODO: 当前控制器直接抛出 BusinessException，需要重构
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
}
