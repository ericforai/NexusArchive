// Input: ArchUnit 测试框架、JUnit
// Output: FondsIsolationTest 架构测试类
// Pos: 架构测试 - 强制全宗隔离规则
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 全宗隔离架构测试
 *
 * <p>强制执行数据按全宗隔离的架构规则，防止跨全宗数据泄露</p>
 */
@Tag("architecture")
@AnalyzeClasses(packages = "com.nexusarchive", importOptions = ImportOption.DoNotIncludeTests.class)
public class FondsIsolationTest {

    /**
     * 规则 1：Controller 层必须导入 FondsContext
     *
     * <p>确保所有 REST Controller 都能访问全宗上下文，
     * 这是数据隔离的基础设施</p>
     */
    @ArchTest
    static final ArchRule controllers_should_import_fonds_context = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("com.nexusarchive.security.FondsContext")
            .because("Controller 必须使用 FondsContext 来获取当前全宗号进行数据隔离");

    /**
     * 规则 2：Service 层应使用 DataScopeService 或 FondsContext
     *
     * <p>确保 Service 层使用正确的数据范围控制机制</p>
     */
    @ArchTest
    static final ArchRule services_should_use_data_scope_or_fonds_context = classes()
            .that().resideInAPackage("com.nexusarchive.service..")
            .and().areNotAssignableTo("com.nexusarchive.security.FondsContext")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("com.nexusarchive.service.DataScopeService")
            .orShould().dependOnClassesThat()
            .haveFullyQualifiedName("com.nexusarchive.security.FondsContext")
            .because("Service 层应该使用 DataScopeService 或 FondsContext 进行数据范围控制");
}
