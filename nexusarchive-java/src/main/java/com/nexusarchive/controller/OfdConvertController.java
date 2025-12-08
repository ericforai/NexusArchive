package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.OfdConvertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
@Tag(name = "电子档案格式转换")
public class OfdConvertController {

    private final OfdConvertService ofdConvertService;

    @PostMapping("/{id}/convert-to-ofd")
    @Operation(summary = "单档案转换为OFD")
    @PreAuthorize("hasAuthority('archive:edit')")
    public Result<Map<String, Object>> convertToOfd(@PathVariable String id) {
        return Result.success(ofdConvertService.convertToOfd(id));
    }

    @PostMapping("/batch-convert-to-ofd")
    @Operation(summary = "批量转换为OFD")
    @PreAuthorize("hasAuthority('archive:edit')")
    public Result<Integer> batchConvertToOfd(@RequestBody List<String> ids) {
        return Result.success(ofdConvertService.batchConvert(ids));
    }
}
