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
}
