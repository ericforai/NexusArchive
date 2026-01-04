// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/TestExecutionService.java
// Input: GeneratedCode
// Output: TestResult
// Pos: AI 模块 - 测试执行服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.deploy;

import com.nexusarchive.integration.erp.ai.generator.GeneratedCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试执行服务
 * <p>
 * 运行生成的测试类，验证代码正确性
 * </p>
 */
@Slf4j
@Service
public class TestExecutionService {

    @Value("${project.basedir:#{null}}")
    private String projectBaseDir;

    private static final int TEST_TIMEOUT_SECONDS = 60;

    /**
     * 运行测试
     *
     * @param code 生成的代码
     * @return 测试结果
     */
    public ErpAdapterAutoDeployService.TestResult runTests(GeneratedCode code) {
        log.info("开始运行测试: className={}", code.getClassName());

        Path basePath = determineBasePath();
        Path pomPath = basePath.resolve("pom.xml");

        if (!Files.exists(pomPath)) {
            return ErpAdapterAutoDeployService.TestResult.failure("找不到 pom.xml: " + pomPath);
        }

        try {
            // 构建测试类名称
            String testClassName = code.getPackageName() + "." + code.getClassName() + "Test";

            // 执行 Maven 测试
            ProcessBuilder pb = new ProcessBuilder(
                "mvn",
                "test",
                "-Dtest=" + testClassName,
                "-q",
                "-f",
                pomPath.toString()
            );

            pb.directory(basePath.toFile());
            pb.redirectErrorStream(true);

            log.debug("执行测试命令: mvn test -Dtest={}", testClassName);
            Process process = pb.start();

            // 读取输出
            List<String> outputLines = new ArrayList<>();
            List<String> errorLines = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLines.add(line);
                    log.trace("测试输出: {}", line);
                }
            }

            // 等待进程完成
            boolean finished = process.waitFor(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ErpAdapterAutoDeployService.TestResult.failure("测试超时（超过 " + TEST_TIMEOUT_SECONDS + " 秒）");
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                // 解析测试数量
                int testCount = parseTestCount(outputLines);
                log.info("测试通过: {} 个测试", testCount);
                return ErpAdapterAutoDeployService.TestResult.success(testCount);
            } else {
                String errorMsg = parseTestErrors(outputLines);
                log.warn("测试失败: {}", errorMsg);
                return ErpAdapterAutoDeployService.TestResult.failure(errorMsg);
            }

        } catch (IOException e) {
            log.error("测试过程发生 IO 异常", e);
            return ErpAdapterAutoDeployService.TestResult.failure("测试异常: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("测试过程被中断", e);
            return ErpAdapterAutoDeployService.TestResult.failure("测试被中断: " + e.getMessage());
        }
    }

    /**
     * 解析测试数量
     */
    private int parseTestCount(List<String> outputLines) {
        // 查找类似 "Tests run: 2, Failures: 0, Errors: 0" 的行
        Pattern pattern = Pattern.compile("Tests run: (\\d+)");
        for (String line : outputLines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return 0;
    }

    /**
     * 解析测试错误
     */
    private String parseTestErrors(List<String> outputLines) {
        List<String> errors = new ArrayList<>();
        for (String line : outputLines) {
            if (line.toLowerCase().contains("error") ||
                line.toLowerCase().contains("failure") ||
                line.toLowerCase().contains("failed")) {
                errors.add(line);
            }
        }
        return errors.isEmpty() ? "测试失败" : String.join("; ", errors);
    }

    /**
     * 确定项目基础路径
     */
    private Path determineBasePath() {
        if (projectBaseDir != null) {
            return Paths.get(projectBaseDir);
        }
        return Paths.get("").toAbsolutePath();
    }
}
