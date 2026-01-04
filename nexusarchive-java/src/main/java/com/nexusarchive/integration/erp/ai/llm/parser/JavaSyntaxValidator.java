// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/parser/JavaSyntaxValidator.java
package com.nexusarchive.integration.erp.ai.llm.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JavaSyntaxValidator {

    /**
     * 验证 Java 代码语法
     */
    public void validate(String javaCode) throws CodeValidationException {
        Path tempDir = null;
        Path tempFile = null;

        try {
            // 创建临时文件
            tempDir = Files.createTempDirectory("ai-code-");
            tempFile = tempDir.resolve("GeneratedAdapter.java");
            Files.writeString(tempFile, javaCode);

            // 使用 javac 验证语法
            ProcessBuilder pb = new ProcessBuilder(
                "javac",
                "-encoding", "UTF-8",
                tempFile.toString()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取错误输出
            List<String> outputLines = new ArrayList<>();
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLines.add(line);
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new CodeValidationException("Syntax validation timeout", outputLines);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new CodeValidationException("Generated code has syntax errors", outputLines);
            }

            log.info("Java syntax validation passed");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeValidationException("Syntax validation interrupted",
                List.of(e.getMessage()));
        } catch (IOException e) {
            throw new CodeValidationException("Syntax validation failed: " + e.getMessage(),
                List.of(e.getMessage()));
        } finally {
            // 清理临时文件
            try {
                if (tempFile != null) Files.deleteIfExists(tempFile);
                if (tempDir != null) Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                log.warn("Failed to cleanup temp files", e);
            }
        }
    }
}
