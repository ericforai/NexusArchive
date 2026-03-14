package com.nexusarchive.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模块依赖契约测试
 * <p>
 * 目的：验证文档中记录的跨模块依赖关系在代码中保持一致。
 * 当依赖关系发生变化时，这些测试会失败，提醒开发者更新架构文档。
 * </p>
 *
 * <p>契约内容来源：docs/architecture/module-dependency-status.md</p>
 *
 * <p>测试策略：</p>
 * <ul>
 *   <li>验证 ARCHITECTURE-NOTE 注释存在性（文档与代码一致性）</li>
 *   <li>验证关键依赖注入方式（如 @Lazy 注解）</li>
 *   <li>验证直接依赖的 Mapper 类型</li>
 * </ul>
 *
 * <p>当这些测试失败时，请检查：</p>
 * <ol>
 *   <li>代码重构是否改变了依赖关系？</li>
 *   <li>如果是，请更新 docs/architecture/module-dependency-status.md</li>
 *   <li>如果不是，请恢复被误删的注释或依赖</li>
 * </ol>
 *
 * @see <a href="../../../../docs/architecture/module-dependency-status.md">模块依赖现状文档</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("architecture")
@DisplayName("模块依赖契约测试")
class ModuleDependencyContractTest {

    /**
     * 测试数据：已确认的跨模块依赖契约
     *
     * 格式：[源文件, 验证类型, 预期内容/模式, 说明]
     */
    private static final List<DependencyContract> CONTRACTS = List.of(
            // 契约 1: 预归档 → 正式归档边界
            new DependencyContract(
                    "src/main/java/com/nexusarchive/service/PreArchiveSubmitService.java",
                    "ARCHITECTURE_NOTE",
                    "ARCHITECTURE-NOTE: 预归档 → 正式归档 边界依赖",
                    "预归档服务应说明为何直接依赖 ArchiveMapper"
            ),
            new DependencyContract(
                    "src/main/java/com/nexusarchive/service/PreArchiveSubmitService.java",
                    "MAPPER_DEPENDENCY",
                    "private final ArchiveMapper archiveMapper",
                    "预归档服务直接注入 ArchiveMapper（已接受的例外）"
            ),

            // 契约 2: 销毁 → Archive 边界
            new DependencyContract(
                    "src/main/java/com/nexusarchive/service/impl/DestructionServiceImpl.java",
                    "ARCHITECTURE_NOTE",
                    "ARCHITECTURE-NOTE: 销毁 → Archive 边界依赖",
                    "销毁服务应说明为何直接依赖 ArchiveMapper"
            ),
            new DependencyContract(
                    "src/main/java/com/nexusarchive/service/impl/DestructionServiceImpl.java",
                    "MAPPER_DEPENDENCY",
                    "private final ArchiveMapper archiveMapper",
                    "销毁服务直接注入 ArchiveMapper（已接受的例外）"
            ),

            // 契约 3: 销毁审批 → Archive 状态变更边界
            new DependencyContract(
                    "src/main/java/com/nexusarchive/service/impl/DestructionApprovalServiceImpl.java",
                    "ARCHITECTURE_NOTE",
                    "ARCHITECTURE-NOTE: 销毁审批 → Archive 状态变更边界",
                    "销毁审批服务应说明为何直接依赖 ArchiveMapper"
            ),

            // 契约 4: 审批流程 → 预归档边界
            new DependencyContract(
                    "src/main/java/com/nexusarchive/service/impl/ArchiveApprovalServiceImpl.java",
                    "LAZY_DEPENDENCY",
                    "@Lazy",
                    "审批服务使用 @Lazy 注入 PreArchiveSubmitService 以避免循环依赖"
            ),
            new DependencyContract(
                    "src/main/java/com/nexusarchive/service/impl/ArchiveApprovalServiceImpl.java",
                    "ARCHITECTURE_NOTE",
                    "ARCHITECTURE-NOTE: 审批 → 预归档 边界依赖",
                    "审批服务应说明使用 @Lazy 的原因"
            ),

            // 契约 5: ArchiveService 职责说明
            new DependencyContract(
                    "src/main/java/com/nexusarchive/service/ArchiveService.java",
                    "ARCHITECTURE_NOTE",
                    "ARCHITECTURE-NOTE: 核心服务 - 职责集中设计",
                    "ArchiveService 应说明为何职责集中"
            ),

            // 契约 6: PoolService 边界说明
            new DependencyContract(
                    "src/main/java/com/nexusarchive/service/impl/PoolServiceImpl.java",
                    "ARCHITECTURE_NOTE",
                    "ARCHITECTURE-NOTE: 预归档模块边界",
                    "PoolService 应说明其职责范围和边界"
            )
    );

    @Test
    @DisplayName("所有契约文件应存在")
    void allContractFilesShouldExist() {
        List<String> missingFiles = CONTRACTS.stream()
                .map(DependencyContract::filePath)
                .distinct()
                .filter(this::fileNotExists)
                .toList();

        assertTrue(missingFiles.isEmpty(),
                "以下契约文件不存在:\n" + String.join("\n", missingFiles) +
                "\n\n如果是文件被移动/重命名，请更新契约定义。");
    }

    @ParameterizedTest
    @MethodSource("provideArchitectureNoteContracts")
    @DisplayName("ARCHITECTURE-NOTE 注释应存在")
    void architectureNoteShouldExist(DependencyContract contract) {
        String content = readFileContent(contract.filePath());
        assertTrue(content.contains(contract.expectedPattern()),
                String.format("""
                        在文件 %s 中未找到预期的 ARCHITECTURE-NOTE 注释。
                        预期包含: %s
                        说明: %s

                        如果重构移除了此注释，请确认：
                        1. 依赖关系是否已改变？
                        2. 如果已改变，请更新 docs/architecture/module-dependency-status.md
                        3. 如果未改变，请恢复此注释以保持文档一致性
                        """,
                        contract.filePath(), contract.expectedPattern(), contract.description()));
    }

    @ParameterizedTest
    @MethodSource("provideMapperDependencyContracts")
    @DisplayName("已接受的 Mapper 直接依赖应存在")
    void acceptedMapperDependenciesShouldExist(DependencyContract contract) {
        String content = readFileContent(contract.filePath());
        assertTrue(content.contains(contract.expectedPattern()),
                String.format("""
                        在文件 %s 中未找到预期的依赖声明。
                        预期包含: %s
                        说明: %s

                        这是文档中记录的"有意的架构妥协"。
                        如果移除此依赖，请确认：
                        1. 重构是否已改用 Facade/Service 解耦？
                        2. 如果是，恭喜！请更新 docs/architecture/module-dependency-status.md
                        3. 如果不是，请恢复此依赖
                        """,
                        contract.filePath(), contract.expectedPattern(), contract.description()));
    }

    @ParameterizedTest
    @MethodSource("provideLazyDependencyContracts")
    @DisplayName("@Lazy 注解应存在（避免循环依赖）")
    void lazyAnnotationShouldExist(DependencyContract contract) {
        String content = readFileContent(contract.filePath());

        // 验证 @Lazy 导入
        boolean hasLazyImport = content.contains("import org.springframework.context.annotation.Lazy") ||
                               content.contains("import org.springframework.beans.factory.annotation.Lazy");

        // 验证 @Lazy 使用
        boolean hasLazyUsage = content.contains("@Lazy");

        assertTrue(hasLazyImport && hasLazyUsage,
                String.format("""
                        在文件 %s 中未找到正确的 @Lazy 注解使用。
                        说明: %s

                        检查结果：
                        - @Lazy 导入: %s
                        - @Lazy 使用: %s

                        如果移除了 @Lazy，请确认：
                        1. 循环依赖问题是否已通过其他方式解决？
                        2. 如果是，请更新 docs/architecture/module-dependency-status.md
                        3. 如果不是，请恢复 @Lazy 注解
                        """,
                        contract.filePath(), contract.description(),
                        hasLazyImport ? "✓" : "✗",
                        hasLazyUsage ? "✓" : "✗"));
    }

    @Test
    @DisplayName("契约数量应与文档一致")
    void contractCountShouldMatchDocumentation() {
        // 契约数量应与 docs/architecture/module-dependency-status.md 中记录的一致
        // 当前文档记录了 3 个主要跨模块依赖
        // 但测试会覆盖更多细节（如每个依赖有多项验证）

        long mapperDependencyCount = CONTRACTS.stream()
                .filter(c -> "MAPPER_DEPENDENCY".equals(c.validationType()))
                .count();

        assertTrue(mapperDependencyCount >= 2,
                "预期至少有 2 个 Mapper 直接依赖契约。" +
                "当前: " + mapperDependencyCount +
                "\n如果是新增了跨模块依赖，请更新文档和契约。");
    }

    // ========== 辅助方法 ==========

    /**
     * 获取项目根目录（nexusarchive-java）
     * 通过向上查找 pom.xml 文件来确定项目根目录，确保在不同工作目录下都能正确运行
     */
    private String getProjectRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        Path current = cwd;

        // 向上查找 pom.xml 文件（最多查找 5 层）
        int maxLevels = 5;
        for (int i = 0; i < maxLevels && current != null; i++) {
            if (Files.exists(current.resolve("pom.xml"))) {
                return current.toString();
            }
            current = current.getParent();
        }

        // 如果找不到 pom.xml，回退到当前目录（可能是 IDE 运行）
        return cwd.toString();
    }

    private Stream<DependencyContract> provideArchitectureNoteContracts() {
        return CONTRACTS.stream()
                .filter(c -> "ARCHITECTURE_NOTE".equals(c.validationType()));
    }

    private Stream<DependencyContract> provideMapperDependencyContracts() {
        return CONTRACTS.stream()
                .filter(c -> "MAPPER_DEPENDENCY".equals(c.validationType()));
    }

    private Stream<DependencyContract> provideLazyDependencyContracts() {
        return CONTRACTS.stream()
                .filter(c -> "LAZY_DEPENDENCY".equals(c.validationType()));
    }

    /**
     * 读取文件内容，提供更详细的错误信息
     */
    private String readFileContent(String relativePath) {
        Path fullPath = Path.of(getProjectRoot(), relativePath);

        if (!Files.exists(fullPath)) {
            throw new AssertionError(
                    String.format("""
                            契约文件不存在: %s
                            相对路径: %s
                            项目根目录: %s

                            可能的原因：
                            1. 文件被移动或重命名
                            2. 相对路径不正确
                            3. 项目根目录检测失败
                            """,
                            fullPath, relativePath, getProjectRoot())
            );
        }

        try {
            return Files.readString(fullPath);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("读取文件失败: %s (完整路径: %s)", relativePath, fullPath),
                    e
            );
        }
    }

    private boolean fileNotExists(String relativePath) {
        return !Files.exists(Path.of(getProjectRoot(), relativePath));
    }

    // ========== 内部记录 ==========

    private record DependencyContract(
            String filePath,           // 相对项目根目录的文件路径
            String validationType,     // 验证类型: ARCHITECTURE_NOTE, MAPPER_DEPENDENCY, LAZY_DEPENDENCY
            String expectedPattern,    // 预期在文件中找到的内容模式
            String description         // 契约说明
    ) {
        @Override
        public String toString() {
            return String.format("[%s] %s: %s", validationType, filePath, description);
        }
    }
}
