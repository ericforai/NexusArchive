// Input: QualityMonitorService
// Output: REST API 端点
// Pos: Controller 层 - 质量监控
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
//
// ⚠️ 开发工具功能 - 不属于产品功能
// 此控制器用于代码复杂度监控，仅供开发团队内部使用。
// 部署离线包时需排除此控制器（通过 Maven Profile 或手动删除）。

package com.nexusarchive.controller;

import com.nexusarchive.dto.quality.ComplexityHistoryDto;
import com.nexusarchive.service.QualityMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 质量监控控制器
 * 提供代码复杂度监控相关的 REST API
 *
 * @author Agent D (基础设施工程师)
 */
@RestController
@RequestMapping("/quality")
@RequiredArgsConstructor
@Slf4j
public class QualityMonitorController {

    private final QualityMonitorService qualityMonitorService;

    /**
     * 获取复杂度历史数据
     *
     * @return 完整的复杂度历史数据
     */
    @GetMapping("/history")
    public ResponseEntity<ComplexityHistoryDto> getHistory() {
        log.info("[QualityMonitor] API: getHistory");
        ComplexityHistoryDto history = qualityMonitorService.getComplexityHistory();
        return ResponseEntity.ok(history);
    }

    /**
     * 生成新的复杂度快照
     *
     * @return 更新后的历史数据
     */
    @PostMapping("/snapshot")
    public ResponseEntity<ComplexityHistoryDto> generateSnapshot() {
        log.info("[QualityMonitor] API: generateSnapshot");
        try {
            ComplexityHistoryDto history = qualityMonitorService.generateSnapshot();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("[QualityMonitor] Failed to generate snapshot", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取最新快照
     *
     * @return 最新的复杂度快照
     */
    @GetMapping("/latest")
    public ResponseEntity<?> getLatest() {
        log.info("[QualityMonitor] API: getLatest");
        ComplexityHistoryDto history = qualityMonitorService.getComplexityHistory();

        if (history.getSnapshots() == null || history.getSnapshots().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 返回最后一个快照
        return ResponseEntity.ok(history.getSnapshots().get(history.getSnapshots().size() - 1));
    }
}
