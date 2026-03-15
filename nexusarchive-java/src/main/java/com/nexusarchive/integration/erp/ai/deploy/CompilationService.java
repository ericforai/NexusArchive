// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/CompilationService.java
// Input: GeneratedCode
// Output: CompilationResult
// Pos: AI 模块 - 编译验证服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.deploy;

import com.nexusarchive.integration.erp.ai.generator.GeneratedCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 编译验证服务
 * <p>
 * 使用 Maven 编译生成的代码，验证语法正确性
 * </p>
 */
@Slf4j
@Service
public class CompilationService {

    @Value("${project.basedir:#{null}}")
    private String projectBaseDir;

    private static final int COMPILATION_TIMEOUT_SECONDS = 120;

    /**
     * 编译生成的代码
     *
     * @param code 生成的代码
     * @return 编译结果
     */
    public ErpAdapterAutoDeployService.CompilationResult compile(GeneratedCode code) {
        log.info("开始编译验证: className={}", code.getClassName());

        Path basePath = determineBasePath();
        Path pomPath = basePath.resolve("pom.xml");

        // 安全验证：确保路径在允许的项目目录内
        if (!validatePathWithinProject(basePath, pomPath)) {
            log.error("路径安全验证失败: basePath={}, pomPath={}", basePath, pomPath);
            return ErpAdapterAutoDeployService.CompilationResult.failure("路径安全验证失败：路径必须在项目目录内");
        }

        if (!Files.exists(pomPath)) {
            return ErpAdapterAutoDeployService.CompilationResult.failure("找不到 pom.xml: " + pomPath);
        }

        try {
            // 执行 Maven 编译（仅编译，不跳过测试）
            ProcessBuilder pb = new ProcessBuilder(
                "mvn",
                "compile",
                "-q",
                "-f",
                pomPath.toString()
            );

            pb.directory(basePath.toFile());
            pb.redirectErrorStream(true);

            log.debug("执行编译命令: mvn compile -f {}", pomPath);
            Process process = pb.start();

            // 读取输出
            List<String> outputLines = new ArrayList<>();
            List<String> errorLines = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLines.add(line);
                    if (line.toLowerCase().contains("error") ||
                        line.toLowerCase().contains("failure") ||
                        line.toLowerCase().contains("failed")) {
                        errorLines.add(line);
                    }
                }
            }

            // 等待进程完成
            boolean finished = process.waitFor(COMPILATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ErpAdapterAutoDeployService.CompilationResult.failure("编译超时（超过 " + COMPILATION_TIMEOUT_SECONDS + " 秒）");
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                String output = String.join("\n", outputLines);
                log.info("编译成功");
                return ErpAdapterAutoDeployService.CompilationResult.success(output);
            } else {
                String errorMsg = String.join("\n", errorLines);
                if (errorMsg.isEmpty()) {
                    errorMsg = "编译失败，退出码: " + exitCode;
                }
                log.error("编译失败: {}", errorMsg);
                return ErpAdapterAutoDeployService.CompilationResult.failure(errorMsg);
            }

        } catch (IOException e) {
            log.error("编译过程发生 IO 异常", e);
            return ErpAdapterAutoDeployService.CompilationResult.failure("编译异常: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("编译过程被中断", e);
            return ErpAdapterAutoDeployService.CompilationResult.failure("编译被中断: " + e.getMessage());
        }
    }

    /**
     * 确定项目基础路径
     */
    private Path determineBasePath() {
        if (projectBaseDir != null) {
            return Paths.get(projectBaseDir).toAbsolutePath();
        }
        return Paths.get("").toAbsolutePath();
    }

    /**
     * 验证路径是否在允许的项目目录内（防止路径遍历攻击）
     *
     * @param basePath 基础路径
     * @param targetPath 目标路径
     * @return 是否通过验证
     */
    private boolean validatePathWithinProject(Path basePath, Path targetPath) {
        try {
            Path absoluteBasePath = basePath.toAbsolutePath().normalize();
            Path absoluteTargetPath = targetPath.toAbsolutePath().normalize();

            // 获取当前工作目录作为项目根目录
            Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

            // 验证基础路径必须在项目根目录内
            if (!absoluteBasePath.startsWith(projectRoot)) {
                log.warn("基础路径不在项目根目录内: basePath={}, projectRoot={}", absoluteBasePath, projectRoot);
                return false;
            }

            // 验证目标路径必须在项目根目录内
            if (!absoluteTargetPath.startsWith(projectRoot)) {
                log.warn("目标路径不在项目根目录内: targetPath={}, projectRoot={}", absoluteTargetPath, projectRoot);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("路径验证异常", e);
            return false;
        }
    }
}
