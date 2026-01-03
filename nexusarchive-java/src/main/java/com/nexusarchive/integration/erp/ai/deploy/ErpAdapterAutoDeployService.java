// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/ErpAdapterAutoDeployService.java
// Input: GeneratedCode, adapter metadata
// Output: DeploymentResult with compilation/test results
// Pos: AI 模块 - 自动部署服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.deploy;

import com.nexusarchive.integration.erp.ai.generator.GeneratedCode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * ERP 适配器自动部署服务
 * <p>
 * 功能：
 * 1. 保存生成的代码到源码目录
 * 2. 自动编译验证
 * 3. 自动运行测试
 * 4. 数据库自动注册
 * 5. 热加载适配器
 * </p>
 */
@Slf4j
@Service
public class ErpAdapterAutoDeployService {

    private final CodeStorageService codeStorageService;
    private final CompilationService compilationService;
    private final TestExecutionService testExecutionService;
    private final DatabaseRegistrationService databaseRegistrationService;
    private final HotLoadService hotLoadService;

    @Autowired
    public ErpAdapterAutoDeployService(CodeStorageService codeStorageService,
                                       CompilationService compilationService,
                                       TestExecutionService testExecutionService,
                                       DatabaseRegistrationService databaseRegistrationService,
                                       HotLoadService hotLoadService) {
        this.codeStorageService = codeStorageService;
        this.compilationService = compilationService;
        this.testExecutionService = testExecutionService;
        this.databaseRegistrationService = databaseRegistrationService;
        this.hotLoadService = hotLoadService;
    }

    /**
     * 执行完整的自动部署流程
     *
     * @param code 生成的代码
     * @return 部署结果
     */
    public DeploymentResult deploy(GeneratedCode code) throws IOException, InterruptedException {
        log.info("开始自动部署流程: className={}", code.getClassName());

        List<String> steps = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        boolean success = true;

        // Step 1: 保存代码到源码
        log.info("Step 1: 保存代码到源码");
        try {
            codeStorageService.saveCode(code);
            steps.add("✅ 代码已保存到: " + codeStorageService.getAdapterPath(code));
            log.info("代码保存成功: {}", codeStorageService.getAdapterPath(code));
        } catch (Exception e) {
            success = false;
            errors.add("❌ 代码保存失败: " + e.getMessage());
            log.error("代码保存失败", e);
            return DeploymentResult.builder()
                .success(false)
                .stepsCompleted(steps)
                .errors(errors)
                .build();
        }

        // Step 2: 编译验证
        log.info("Step 2: 编译验证");
        CompilationResult compilationResult = compilationService.compile(code);
        if (compilationResult.isSuccess()) {
            steps.add("✅ 编译成功: " + compilationResult.getOutputMessage());
            log.info("编译成功");
        } else {
            success = false;
            errors.add("❌ 编译失败: " + compilationResult.getErrorMessage());
            log.error("编译失败: {}", compilationResult.getErrorMessage());
            // 编译失败则终止后续步骤
            return DeploymentResult.builder()
                .success(false)
                .stepsCompleted(steps)
                .errors(errors)
                .build();
        }

        // Step 3: 运行测试
        log.info("Step 3: 运行测试");
        TestResult testResult = testExecutionService.runTests(code);
        if (testResult.isSuccess()) {
            steps.add("✅ 测试通过: " + testResult.getTestCount() + " 个测试");
            log.info("测试通过: {} 个测试", testResult.getTestCount());
        } else {
            steps.add("⚠️ 测试失败: " + testResult.getErrorMessage());
            log.warn("测试失败: {}", testResult.getErrorMessage());
            // 测试失败不阻止部署，只警告
        }

        // Step 4: 数据库注册
        log.info("Step 4: 数据库注册");
        try {
            databaseRegistrationService.register(code);
            steps.add("✅ 数据库注册成功");
            log.info("数据库注册成功");
        } catch (Exception e) {
            success = false;
            errors.add("❌ 数据库注册失败: " + e.getMessage());
            log.error("数据库注册失败", e);
            return DeploymentResult.builder()
                .success(false)
                .stepsCompleted(steps)
                .errors(errors)
                .build();
        }

        // Step 5: 热加载
        log.info("Step 5: 热加载适配器");
        try {
            hotLoadService.reloadAdapter(code.getClassName());
            steps.add("✅ 热加载成功");
            log.info("热加载成功");
        } catch (Exception e) {
            steps.add("⚠️ 热加载失败（需手动重启）: " + e.getMessage());
            log.warn("热加载失败（需手动重启）", e);
            // 热加载失败不阻止部署，代码已保存
        }

        log.info("自动部署完成: success={}, steps={}", success, steps.size());
        return DeploymentResult.builder()
            .success(success)
            .stepsCompleted(steps)
            .errors(errors)
            .adapterPath(codeStorageService.getAdapterPath(code).toString())
            .className(code.getClassName())
            .packageName(code.getPackageName())
            .build();
    }

    /**
     * 部署结果
     */
    @Data
    @Builder
    public static class DeploymentResult {
        private boolean success;
        private List<String> stepsCompleted;
        private List<String> errors;
        private String adapterPath;
        private String className;
        private String packageName;

        public String getMessage() {
            if (success) {
                return "自动部署成功";
            } else {
                return "自动部署失败: " + String.join("; ", errors);
            }
        }
    }

    /**
     * 编译结果
     */
    @Data
    @Builder
    public static class CompilationResult {
        private boolean success;
        private String outputMessage;
        private String errorMessage;
    }

    /**
     * 测试结果
     */
    @Data
    @Builder
    public static class TestResult {
        private boolean success;
        private int testCount;
        private String errorMessage;
    }
}
