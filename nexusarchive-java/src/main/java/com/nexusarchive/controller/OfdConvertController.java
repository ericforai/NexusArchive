// Input: io.swagger、Lombok、Spring Security、Spring Framework、等
// Output: OfdConvertController 类（OFD 转换已禁用）
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
    private static final int DISABLED_CODE = 410;
    private static final String DISABLED_MESSAGE = "OFD 转换已禁用，归档仅保留原始 PDF";

    @PostMapping("/{id}/convert-to-ofd")
    @Operation(summary = "单档案转换为OFD（已禁用）")
    @PreAuthorize("hasAuthority('archive:edit')")
    public Result<Map<String, Object>> convertToOfd(@PathVariable String id) {
        return Result.error(DISABLED_CODE, DISABLED_MESSAGE);
    }

    @PostMapping("/batch-convert-to-ofd")
    @Operation(summary = "批量转换为OFD（已禁用）")
    @PreAuthorize("hasAuthority('archive:edit')")
    public Result<Integer> batchConvertToOfd(@RequestBody List<String> ids) {
        return Result.error(DISABLED_CODE, DISABLED_MESSAGE);
    }
}
