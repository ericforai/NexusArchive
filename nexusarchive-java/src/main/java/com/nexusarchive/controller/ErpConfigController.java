// Input: cn.hutool、io.swagger、Lombok、Spring Security、等
// Output: ErpConfigController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.ErpConfigDto;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory.ErpAdapterInfo;
import com.nexusarchive.integration.erp.dto.ConnectionTestResult;
import com.nexusarchive.service.ErpConfigService;
import com.nexusarchive.service.ErpDiagnosisService;
import com.nexusarchive.service.erp.ErpConfigDtoBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

/**
 * ERP 配置 Controller
 *
 * <p><strong>安全规则：</strong></p>
 * <ul>
 *   <li>所有 API 响应使用 ErpConfigDto（已清理敏感信息）</li>
 *   <li>testConnection 使用 getByIdForInternalUse() 获取完整配置</li>
 *   <li>save 保存配置时自动加密敏感字段</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/erp/config")
@RequiredArgsConstructor
@Tag(name = "ERP对接配置")
public class ErpConfigController {

    private final ErpConfigService erpConfigService;
    private final ErpAdapterFactory erpAdapterFactory;
    private final ErpDiagnosisService erpDiagnosisService;
    private final ErpConfigDtoBuilder erpConfigDtoBuilder;

    /**
     * 获取所有配置（API 响应，已清理敏感信息）
     */
    @GetMapping
    @Operation(summary = "获取所有配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<List<ErpConfigDto>> list() {
        return Result.success(erpConfigService.getConfigs());
    }

    @PostMapping
    @Operation(summary = "新增/更新配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<Void> save(@Valid @RequestBody ErpConfig config) {
        try {
            erpConfigService.saveConfig(config);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.warn("保存ERP配置失败 - 参数错误: {}", e.getMessage());
            return Result.error("参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("保存ERP配置失败 - 系统错误: {}", e.getMessage(), e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<Void> delete(@PathVariable Long id) {
        erpConfigService.deleteConfig(id);
        return Result.success();
    }

    @GetMapping("/types")
    @Operation(summary = "获取支持的ERP类型")
    public Result<List<ErpAdapterInfo>> getSupportedTypes() {
        return Result.success(erpAdapterFactory.listAvailableAdapters());
    }

    @GetMapping("/stats")
    @Operation(summary = "获取集成监控统计")
    public Result<java.util.Map<String, Object>> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        // 1. 接入系统总数
        stats.put("connectedSystems", erpConfigService.countConfigs());

        // 2. 今日接收数据 (模拟: 真实环境应查询 arc_file_content where source_system is not null and
        // created_time = today)
        // 由于是演示/刚安装，返回 0 是正确的
        stats.put("todayReceived", 0);

        // 3. 运行正常接口
        stats.put("activeInterfaces", erpConfigService.countActiveConfigs());

        // 4. 异常报警 (模拟)
        stats.put("abnormalCount", 0);

        return Result.success(stats);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试连接")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<java.util.Map<String, Object>> testConnection(@PathVariable Long id) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        // 使用内部方法获取完整配置（包含敏感信息）
        ErpConfig config = erpConfigService.getByIdForInternalUse(id);
        if (config == null) {
            return Result.error("配置不存在");
        }

        try {
            // 获取适配器并真正测试连接
            ErpAdapter adapter = erpAdapterFactory.getAdapter(config.getErpType());
            if (adapter != null) {
                // 将 Entity 转换为适配器 DTO（包含解密后的敏感信息）
                com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig =
                    erpConfigDtoBuilder.buildDtoConfig(config);

                // 调用适配器的 testConnection 方法
                ConnectionTestResult testResult = adapter.testConnection(dtoConfig);

                result.put("id", config.getId());
                result.put("name", config.getName());
                result.put("erpType", config.getErpType());
                result.put("success", testResult.isSuccess());
                result.put("adapterName", adapter.getName());
                result.put("message", testResult.getMessage());
                result.put("responseTimeMs", testResult.getResponseTimeMs());
            } else {
                result.put("success", false);
                result.put("message", "未找到对应的适配器: " + config.getErpType());
            }
        } catch (Exception e) {
            log.error("测试连接失败: configId={}", id, e);
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
        }

        return Result.success(result);
    }

    @GetMapping("/{id}/diagnose")
    @Operation(summary = "一键诊断")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<java.util.Map<String, Object>> diagnose(@PathVariable Long id) {
        return Result.success(erpDiagnosisService.diagnose(id));
    }
}
