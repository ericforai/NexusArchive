// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/CodeStorageService.java
// Input: GeneratedCode
// Output: Saved Java files in source directory
// Pos: AI 模块 - 代码存储服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.deploy;

import com.nexusarchive.integration.erp.ai.generator.GeneratedCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 代码存储服务
 * <p>
 * 将生成的代码保存到源码目录
 * </p>
 */
@Slf4j
@Service
public class CodeStorageService {

    @Value("${project.basedir:#{null}}")
    private String projectBaseDir;

    /**
     * 保存生成的代码到源码目录
     *
     * @param code 生成的代码
     */
    public void saveCode(GeneratedCode code) throws IOException {
        // 确定基础路径
        Path basePath = determineBasePath();
        Path sourceRoot = basePath.resolve("src/main/java");

        // 保存适配器主类
        saveAdapterClass(sourceRoot, code);

        // 保存 DTO 类
        saveDtoClasses(sourceRoot, code);

        // 保存测试类
        saveTestClass(basePath, code);

        log.info("所有代码文件已保存到源码目录");
    }

    /**
     * 获取适配器文件路径
     */
    public Path getAdapterPath(GeneratedCode code) {
        Path basePath = determineBasePath();
        Path sourceRoot = basePath.resolve("src/main/java");
        return sourceRoot.resolve(code.getPackageName().replace('.', '/'))
            .resolve(code.getClassName() + ".java");
    }

    /**
     * 确定项目基础路径
     */
    private Path determineBasePath() {
        if (projectBaseDir != null) {
            return Paths.get(projectBaseDir);
        }

        // 回退到当前工作目录
        Path cwd = Paths.get("").toAbsolutePath();
        log.debug("使用当前工作目录作为基础路径: {}", cwd);
        return cwd;
    }

    /**
     * 保存适配器主类
     */
    private void saveAdapterClass(Path sourceRoot, GeneratedCode code) throws IOException {
        String packagePath = code.getPackageName().replace('.', '/');
        Path packageDir = sourceRoot.resolve(packagePath);

        // 创建目录
        Files.createDirectories(packageDir);

        // 写入文件
        Path classFile = packageDir.resolve(code.getClassName() + ".java");
        Files.writeString(classFile,
            code.getAdapterClass(),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);

        log.info("保存适配器类: {}", classFile);
    }

    /**
     * 保存 DTO 类
     */
    private void saveDtoClasses(Path sourceRoot, GeneratedCode code) throws IOException {
        for (GeneratedCode.DtoClass dtoClass : code.getDtoClasses()) {
            String packagePath = dtoClass.getPackageName().replace('.', '/');
            Path packageDir = sourceRoot.resolve(packagePath);

            Files.createDirectories(packageDir);

            Path classFile = packageDir.resolve(dtoClass.getClassName() + ".java");
            Files.writeString(classFile,
                dtoClass.getCode(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

            log.info("保存 DTO 类: {}", classFile);
        }
    }

    /**
     * 保存测试类
     */
    private void saveTestClass(Path basePath, GeneratedCode code) throws IOException {
        String packagePath = code.getPackageName().replace('.', '/');
        Path testRoot = basePath.resolve("src/test/java");
        Path packageDir = testRoot.resolve(packagePath);

        Files.createDirectories(packageDir);

        Path classFile = packageDir.resolve(code.getClassName() + "Test.java");
        Files.writeString(classFile,
            code.getTestClass(),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);

        log.info("保存测试类: {}", classFile);
    }
}
