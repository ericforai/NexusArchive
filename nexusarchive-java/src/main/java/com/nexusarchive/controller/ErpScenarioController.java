package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.service.ErpScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/erp/scenario")
@RequiredArgsConstructor
@Tag(name = "ERP业务场景管理")
public class ErpScenarioController {

    private final ErpScenarioService erpScenarioService;

    @GetMapping("/list/{configId}")
    @Operation(summary = "获取指定ERP配置的场景列表")
    public Result<List<ErpScenario>> listByConfig(@PathVariable Long configId) {
        return Result.success(erpScenarioService.listScenariosByConfigId(configId));
    }

    @PutMapping
    @Operation(summary = "更新场景配置")
    public Result<Void> update(@RequestBody ErpScenario scenario) {
        erpScenarioService.updateScenario(scenario);
        return Result.success();
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "手动触发同步")
    public Result<Void> triggerSync(@PathVariable Long id) {
        erpScenarioService.syncScenario(id);
        return Result.success();
    }

    @GetMapping("/channels")
    @Operation(summary = "获取所有集成通道（聚合视图）")
    public Result<List<com.nexusarchive.dto.IntegrationChannelDTO>> listAllChannels() {
        return Result.success(erpScenarioService.listAllChannels());
    }
}
