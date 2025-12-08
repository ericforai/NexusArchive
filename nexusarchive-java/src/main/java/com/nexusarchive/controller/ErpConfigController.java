package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory.ErpAdapterInfo;
import com.nexusarchive.mapper.ErpConfigMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/erp/config")
@RequiredArgsConstructor
@Tag(name = "ERP对接配置")
public class ErpConfigController {

    private final ErpConfigMapper erpConfigMapper;
    private final ErpAdapterFactory erpAdapterFactory;

    @GetMapping
    @Operation(summary = "获取所有配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<ErpConfig>> list() {
        return Result.success(erpConfigMapper.selectList(null));
    }

    @PostMapping
    @Operation(summary = "新增/更新配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> save(@RequestBody ErpConfig config) {
        if (config.getId() == null) {
            erpConfigMapper.insert(config);
        } else {
            erpConfigMapper.updateById(config);
        }
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        erpConfigMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/types")
    @Operation(summary = "获取支持的ERP类型")
    public Result<List<ErpAdapterInfo>> getSupportedTypes() {
        return Result.success(erpAdapterFactory.listAvailableAdapters());
    }
}
