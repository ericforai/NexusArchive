// Input: ObjectMapper, ModuleCatalog DTOs
// Output: ModuleCatalogExporter
// Pos: Service Layer - Governance
// 负责导出模块清单

package com.nexusarchive.service.governance.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.service.governance.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 模块目录导出器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>导出模块目录为 JSON</li>
 *   <li>生成后端模块清单</li>
 *   <li>生成前端模块清单</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModuleCatalogExporter {

    private final ObjectMapper objectMapper;

    /**
     * 导出模块目录为 JSON
     *
     * @return JSON 字符串
     * @throws IOException 如果序列化失败
     */
    public String exportCatalog() throws IOException {
        ModuleCatalog catalog = ModuleCatalog.builder()
            .version("2.1.0")
            .generatedAt(new Date())
            .backendModules(exportBackendModules())
            .frontendModules(exportFrontendModules())
            .build();

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(catalog);
    }

    /**
     * 导出后端模块列表
     */
    public List<BackendModule> exportBackendModules() {
        List<BackendModule> modules = new ArrayList<>();

        // Core layers
        modules.add(BackendModule.builder()
            .id("BE.CONTROLLER")
            .name("Controller Layer")
            .packageName("com.nexusarchive.controller")
            .description("REST API 端点")
            .status("ACTIVE")
            .build());

        modules.add(BackendModule.builder()
            .id("BE.SERVICE")
            .name("Service Layer")
            .packageName("com.nexusarchive.service")
            .description("业务逻辑实现")
            .status("ACTIVE")
            .build());

        modules.add(BackendModule.builder()
            .id("BE.MAPPER")
            .name("Mapper Layer")
            .packageName("com.nexusarchive.mapper")
            .description("数据访问层")
            .status("ACTIVE")
            .build());

        modules.add(BackendModule.builder()
            .id("BE.ENTITY")
            .name("Entity Layer")
            .packageName("com.nexusarchive.entity")
            .description("数据模型定义")
            .status("ACTIVE")
            .build());

        // Modularized components
        modules.add(BackendModule.builder()
            .id("BE.BORROWING")
            .name("Borrowing Module")
            .packageName("com.nexusarchive.modules.borrowing")
            .description("借阅全生命周期管理")
            .status("ACTIVE")
            .sinceVersion("2.0.0")
            .build());

        // Integration layer
        modules.add(BackendModule.builder()
            .id("BE.INTEGRATION")
            .name("Integration Layer")
            .packageName("com.nexusarchive.integration")
            .description("外部系统集成 (ERP)")
            .status("ACTIVE")
            .build());

        return modules;
    }

    /**
     * 导出前端模块列表
     */
    public List<FrontendModule> exportFrontendModules() {
        List<FrontendModule> modules = new ArrayList<>();

        modules.add(FrontendModule.builder()
            .id("FE.SYS")
            .name("Settings Module")
            .scope("src/features/settings + src/pages/settings + src/components/settings")
            .description("系统基础配置/字典/日志")
            .allowedDependencies("src/api, src/store, src/utils, src/hooks, src/types.ts")
            .status("LOCKED")
            .build());

        modules.add(FrontendModule.builder()
            .id("FE.ADMIN")
            .name("Admin Module")
            .scope("src/pages/admin + 相关组件")
            .description("用户/角色/全宗管理")
            .allowedDependencies("src/api, src/store, src/utils, src/types.ts")
            .status("IN_PROGRESS")
            .build());

        modules.add(FrontendModule.builder()
            .id("FE.SHARED")
            .name("Shared Infrastructure")
            .scope("src/api, src/store, src/utils, src/hooks")
            .description("跨模块通用能力与基础设施")
            .allowedDependencies("无跨模块依赖")
            .status("BASE")
            .build());

        return modules;
    }
}
