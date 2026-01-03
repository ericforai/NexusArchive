// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/controller/ErpAdaptationController.java
// Input: HTTP requests
// Output: JSON responses
// Pos: AI 模块 - REST API 控制器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.controller;

import com.nexusarchive.integration.erp.ai.agent.ErpAdaptationOrchestrator;
import com.nexusarchive.integration.erp.ai.agent.ErpAdaptationOrchestrator.AdaptationRequest;
import com.nexusarchive.integration.erp.ai.agent.ErpAdaptationOrchestrator.AdaptationResult;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * ERP 适配 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/erp-ai")
public class ErpAdaptationController {

    private final ErpAdaptationOrchestrator orchestrator;

    public ErpAdaptationController(ErpAdaptationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * 上传接口文件并生成适配器
     */
    @PostMapping("/adapt")
    public ResponseEntity<ApiResponse> adaptErp(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("erpType") String erpType,
            @RequestParam("erpName") String erpName
    ) {
        try {
            log.info("收到 ERP 适配请求: erpType={}, erpName={}, fileCount={}",
                erpType, erpName, files.size());

            // 构建请求
            AdaptationRequest request = AdaptationRequest.builder()
                .erpType(erpType)
                .erpName(erpName)
                .apiFiles(files)
                .build();

            // 执行适配
            AdaptationResult result = orchestrator.adapt(request);

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(result.getMessage()));
            }

        } catch (IOException e) {
            log.error("文件处理失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("文件处理失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("ERP 适配失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 上传接口文件、生成适配器并自动部署
     *
     * 功能包括：
     * 1. 解析 OpenAPI 文档
     * 2. 生成适配器代码
     * 3. 保存代码到源码目录
     * 4. 自动编译验证
     * 5. 自动运行测试
     * 6. 数据库自动注册
     * 7. 热加载适配器（MVP 版本需手动重启）
     */
    @PostMapping("/deploy")
    public ResponseEntity<ApiResponse> adaptAndDeployErp(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("erpType") String erpType,
            @RequestParam("erpName") String erpName
    ) {
        try {
            log.info("收到 ERP 适配+自动部署请求: erpType={}, erpName={}, fileCount={}",
                erpType, erpName, files.size());

            // 构建请求
            AdaptationRequest request = AdaptationRequest.builder()
                .erpType(erpType)
                .erpName(erpName)
                .apiFiles(files)
                .build();

            // 执行适配+自动部署
            AdaptationResult result = orchestrator.adaptAndDeploy(request);

            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(result.getMessage()));
            }

        } catch (IOException e) {
            log.error("文件处理失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("文件处理失败: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("部署过程被中断", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("部署被中断: " + e.getMessage()));
        } catch (Exception e) {
            log.error("ERP 适配或部署失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 预览生成的代码
     * MVP: 简化实现，返回基本信息
     */
    @GetMapping("/preview/{sessionId}")
    public ResponseEntity<ApiResponse> previewCode(@PathVariable String sessionId) {
        // MVP: 简化实现， sessionId 在当前版本中未使用
        // 完整版本会从会话存储中获取生成的代码
        return ResponseEntity.ok(ApiResponse.success("预览功能（MVP 简化版本）"));
    }

    /**
     * API 响应封装
     */
    @Data
    @lombok.Builder
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public static ApiResponse success(Object data) {
            return ApiResponse.builder()
                .success(true)
                .message("操作成功")
                .data(data)
                .build();
        }

        public static ApiResponse error(String message) {
            return ApiResponse.builder()
                .success(false)
                .message(message)
                .build();
        }
    }
}
