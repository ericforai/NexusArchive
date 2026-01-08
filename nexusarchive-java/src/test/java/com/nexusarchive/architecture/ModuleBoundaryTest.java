package com.nexusarchive.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.nexusarchive", importOptions = ImportOption.DoNotIncludeTests.class)
public class ModuleBoundaryTest {

    /**
     * 规则 1：分层架构约束 (模块内部)
     * 采用标准的依赖倒置原则 (DIP)：
     * - API 层只能被外部访问 (通过 Controller)
     * - Application 层可被 API 和 Infrastructure (实现接口) 访问
     * - Domain 层只能被 Application 和 Infrastructure 访问
     * - Infrastructure 层只能被 Application 访问 (依赖倒置)
     */
    @ArchTest
    static final ArchRule layers_should_be_respected = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("com.nexusarchive.modules..")
            .layer("API").definedBy("com.nexusarchive.modules.*.api..")
            .layer("Application").definedBy("com.nexusarchive.modules.*.app..")
            .layer("Domain").definedBy("com.nexusarchive.modules.*.domain..")
            .layer("Infrastructure").definedBy("com.nexusarchive.modules.*.infra..")
            .whereLayer("API").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("API", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Application")
            // API DTO 是对外契约，允许应用层引用
            .ignoreDependency(
                    resideInAnyPackage("com.nexusarchive.modules.*.app.."),
                    resideInAnyPackage("com.nexusarchive.modules.*.api.dto..")
            );

    /**
     * 规则 2：领域实体封箱 (合规保护)
     * 核心数据库实体 (Archive/Entity) 不得出现在业务模块的 API 定义中。
     * 同时也防止 DTO 误用 Entity。
     */
    @ArchTest
    static final ArchRule domain_entities_should_not_leak_to_api = noClasses()
            .that().resideInAPackage("com.nexusarchive.modules.*.api..")
            .should().dependOnClassesThat().resideInAPackage("com.nexusarchive.entity..");
    
    /**
     * 规则 3：Borrowing 模块公开契约 (显式白名单)
     * 外部只能依赖 Borrowing 的 App (Facade) 与 API DTO。
     * 禁止访问其 Domain, Infra, 或 API (Controller 不应被其他服务依赖) 层。
     */
    @ArchTest
    static final ArchRule borrowing_public_contract_only = noClasses()
            .that().resideOutsideOfPackage("com.nexusarchive.modules.borrowing..")
            .should().dependOnClassesThat(
                    resideInAnyPackage(
                            "com.nexusarchive.modules.borrowing.domain..",
                            "com.nexusarchive.modules.borrowing.infra..")
                            .or(resideInAPackage("com.nexusarchive.modules.borrowing.api..")
                                    .and(not(resideInAPackage("com.nexusarchive.modules.borrowing.api.dto.."))))
            );

    /**
     * 规则 4：Borrowing 内部不得依赖 Workflow/Web 实现包
     */
    @ArchTest
    static final ArchRule borrowing_should_not_depend_on_workflow_or_web_impl = noClasses()
            .that().resideInAPackage("com.nexusarchive.modules.borrowing..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexusarchive.service.impl..",
                    "com.nexusarchive.controller.."
            );

    /**
     * 规则 5：所有 DDD 模块不得依赖传统分层架构
     * 新模块不应依赖旧架构（service.impl/controller 根包），
     * 确保模块独立性，防止新旧架构混杂。
     */
    @ArchTest
    static final ArchRule modules_should_not_depend_on_legacy_layers = noClasses()
            .that().resideInAPackage("com.nexusarchive.modules..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexusarchive.service.impl..",
                    "com.nexusarchive.controller.."
            )
            .because("DDD 模块应通过 Facade 对外暴露，不应依赖旧架构");

    /**
     * 规则 6：传统分层只能通过 Facade 访问 DDD 模块
     * 只能通过 app 层（Facade）和 api.dto 层（数据契约）访问模块，
     * 禁止直接访问 domain 或 infra 层。
     */
    @ArchTest
    static final ArchRule legacy_should_only_access_module_via_facade = noClasses()
            .that().resideOutsideOfPackages("com.nexusarchive.modules..")
            .and().resideInAPackage("com.nexusarchive..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexusarchive.modules.*.domain..",
                    "com.nexusarchive.modules.*.infra..",
                    "com.nexusarchive.modules.*.api.controller.."
            )
            .because("外部只能通过 Facade (app层) 和 DTO (api.dto层) 访问模块");

    /**
     * 规则 7：禁止循环依赖（模块间）
     * 模块 A 依赖模块 B 时，模块 B 不应反向依赖 A。
     * 此规则防止模块间循环依赖。
     */
    @ArchTest
    static final ArchRule no_circular_dependencies_between_modules = noClasses()
            .that().resideInAPackage("com.nexusarchive.modules..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "com.nexusarchive.modules..",
                    "com.nexusarchive.common..",
                    "com.nexusarchive.annotation..",
                    "com.nexusarchive.security..",
                    "com.nexusarchive.config..",
                    "com.nexusarchive.exception..",
                    "com.nexusarchive.dto..",
                    "lombok..",
                    "org.springframework..",
                    "jakarta..",
                    "java..",
                    "com.baomidou.."
            )
            .because("模块间应保持单向依赖，避免循环");

    /**
     * 规则 8：禁止 Service 实现类之间直接依赖
     * Service.impl 包中的类不应依赖其他 Service.impl 类，
     * 应通过接口（Service 包）进行依赖。
     * 这防止了紧耦合和循环依赖。
     */
    @ArchTest
    static final ArchRule service_impls_should_not_depend_on_each_other = noClasses()
            .that().resideInAPackage("com.nexusarchive.service.impl..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.nexusarchive.service.impl..")
            .because("Service 实现类之间不应直接依赖，应通过接口解耦");

    /**
     * 规则 9：Controller 不应直接依赖 Service 实现类
     * Controller 应依赖 Service 接口，而非实现类。
     * 这符合依赖倒置原则（DIP）。
     */
    @ArchTest
    static final ArchRule controllers_should_not_depend_on_service_impls = noClasses()
            .that().resideInAPackage("com.nexusarchive.controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.nexusarchive.service.impl..")
            .because("Controller 应依赖 Service 接口，而非实现类");

    /**
     * 规则 10：禁止跨层直接访问 Mapper
     * Controller 不应直接依赖 MyBatis Mapper，
     * 应通过 Service 层访问数据。
     */
    @ArchTest
    static final ArchRule controllers_should_not_depend_on_mappers = noClasses()
            .that().resideInAPackage("com.nexusarchive.controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.nexusarchive.mapper..")
            .because("Controller 应通过 Service 层访问数据，而非直接使用 Mapper");
}
