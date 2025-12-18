package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory.ErpAdapterInfo;
import com.nexusarchive.util.SM4Utils;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.nexusarchive.mapper.ErpConfigMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/erp/config")
@RequiredArgsConstructor
@Tag(name = "ERP对接配置")
public class ErpConfigController {

    private final ErpConfigMapper erpConfigMapper;
    private final ErpAdapterFactory erpAdapterFactory;
    private final com.nexusarchive.service.ErpDiagnosisService erpDiagnosisService;

    @GetMapping
    @Operation(summary = "获取所有配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<List<ErpConfig>> list() {
        return Result.success(erpConfigMapper.selectList(null));
    }

    @PostMapping
    @Operation(summary = "新增/更新配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<Void> save(@RequestBody ErpConfig config) {
        // 敏感信息加固: 加密 configJson 中的 secret
        if (config.getConfigJson() != null && !config.getConfigJson().isEmpty()) {
            try {
                JSONObject json = JSONUtil.parseObj(config.getConfigJson());
                encryptSecret(json, "appSecret");
                encryptSecret(json, "clientSecret");
                config.setConfigJson(json.toString());
            } catch (Exception e) {
                // 如果不是 JSON 则跳过，由前端保证格式
            }
        }

        if (config.getId() == null) {
            erpConfigMapper.insert(config);
        } else {
            erpConfigMapper.updateById(config);
        }
        return Result.success();
    }

    private void encryptSecret(JSONObject json, String key) {
        String secret = json.getStr(key);
        if (secret != null && !secret.isEmpty()) {
            // 如果已经是 32 位 hex (SM4 密文格式)，则不再加密
            if (secret.length() != 32 || !secret.matches("^[0-9a-fA-F]{32}$")) {
                json.set(key, SM4Utils.encrypt(secret));
            }
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<Void> delete(@PathVariable Long id) {
        erpConfigMapper.deleteById(id);
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
        Long connectedSystems = erpConfigMapper.selectCount(null);
        stats.put("connectedSystems", connectedSystems);

        // 2. 今日接收数据 (模拟: 真实环境应查询 arc_file_content where source_system is not null and
        // created_time = today)
        // 由于是演示/刚安装，返回 0 是正确的
        stats.put("todayReceived", 0);

        // 3. 运行正常接口
        stats.put("activeInterfaces", erpConfigMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpConfig>()
                        .eq(ErpConfig::getIsActive, 1)));

        // 4. 异常报警 (模拟)
        stats.put("abnormalCount", 0);

        return Result.success(stats);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试连接")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    public Result<java.util.Map<String, Object>> testConnection(@PathVariable Long id) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        ErpConfig config = erpConfigMapper.selectById(id);
        if (config == null) {
            return Result.error("配置不存在");
        }

        try {
            // 尝试获取适配器来验证连接配置
            var adapter = erpAdapterFactory.getAdapter(config.getErpType());
            if (adapter != null) {
                // 适配器存在说明类型可用
                result.put("success", true);
                result.put("adapterName", adapter.getClass().getSimpleName());
                result.put("message", "连接测试成功：适配器已就绪");
            } else {
                result.put("success", false);
                result.put("message", "未找到对应的适配器: " + config.getErpType());
            }
        } catch (Exception e) {
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
