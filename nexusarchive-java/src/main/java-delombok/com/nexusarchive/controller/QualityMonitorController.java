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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 质量监控控制器
 *
 * PRD 来源: 开发工具（非产品功能）
 * 提供代码复杂度监控相关的 REST API
 *
 * @author Agent D (基础设施工程师)
 *
 * ⚠️ 开发工具功能 - 不属于产品功能
 * 此控制器用于代码复杂度监控，仅供开发团队内部使用。
 * 部署离线包时需排除此控制器（通过 Maven Profile 或手动删除）。
 */
@Tag(name = "质量监控", description = """
    代码复杂度监控接口（开发工具）。

    **功能说明:**
    - 获取复杂度历史数据
    - 生成新的复杂度快照
    - 查询最新快照

    **复杂度指标:**
    - 圈复杂度 (Cyclomatic Complexity): 方法分支数量
    - 认知复杂度 (Cognitive Complexity): 代码理解难度
    - 方法行数: 单方法代码行数
    - 类行数: 单类代码行数

    **快照数据:**
    - timestamp: 快照时间戳
    - totalClasses: 总类数
    - totalMethods: 总方法数
    - avgComplexity: 平均复杂度
    - highComplexityMethods: 高复杂度方法列表
    - violations: 违规项统计

    **数据存储:**
    - 文件路径: docs/metrics/complexity-history.json
    - 保留快照数: 默认最近 100 个

    **使用场景:**
    - 代码质量趋势分析
    - 技术债识别
    - 重构优先级决策

    **权限要求:**
    - 仅限开发环境
    - 生产环境应禁用
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/quality")
@RequiredArgsConstructor
@Slf4j
public class QualityMonitorController {

    private final QualityMonitorService qualityMonitorService;

    /**
     * 获取复杂度历史数据
     */
    @GetMapping("/history")
    @Operation(
        summary = "获取复杂度历史数据",
        description = """
            获取完整的代码复杂度历史记录。

            **返回数据包括:**
            - snapshots: 快照列表
              - timestamp: 快照时间戳
              - totalClasses: 总类数
              - totalMethods: 总方法数
              - avgCyclomaticComplexity: 平均圈复杂度
              - avgCognitiveComplexity: 平均认知复杂度
              - highComplexityMethods: 高复杂度方法列表
            - summary: 统计摘要
              - totalSnapshots: 总快照数
              - firstSnapshotTime: 首个快照时间
              - lastSnapshotTime: 最近快照时间

            **业务规则:**
            - 按时间戳升序排列
            - 最多返回最近 100 个快照

            **使用场景:**
            - 质量趋势分析
            - 历史数据对比
            """,
        operationId = "getComplexityHistory",
        tags = {"质量监控"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public ResponseEntity<ComplexityHistoryDto> getHistory() {
        log.info("[QualityMonitor] API: getHistory");
        ComplexityHistoryDto history = qualityMonitorService.getComplexityHistory();
        return ResponseEntity.ok(history);
    }

    /**
     * 生成新的复杂度快照
     */
    @PostMapping("/snapshot")
    @Operation(
        summary = "生成新的复杂度快照",
        description = """
            执行代码复杂度分析并生成新快照。

            **分析流程:**
            1. 扫描 src/main/java 目录
            2. 解析所有 Java 类和方法
            3. 计算圈复杂度和认知复杂度
            4. 识别高复杂度方法（阈值 10）
            5. 追加到历史记录文件

            **返回数据包括:**
            - 更新后的完整历史数据
            - 新生成的快照位于列表末尾

            **高复杂度方法判定标准:**
            - 圈复杂度 > 10
            - 认知复杂度 > 15

            **业务规则:**
            - 自动保留最近 100 个快照
            - 超出限制时删除最旧的快照
            - 分析耗时约 5-30 秒（取决于代码规模）

            **使用场景:**
            - 定期质量检查
            - 发布前质量评估
            """,
        operationId = "generateComplexitySnapshot",
        tags = {"质量监控"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "快照生成成功"),
        @ApiResponse(responseCode = "500", description = "生成失败")
    })
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
     */
    @GetMapping("/latest")
    @Operation(
        summary = "获取最新快照",
        description = """
            获取最近一次的复杂度快照。

            **返回数据包括:**
            - timestamp: 快照时间戳
            - totalClasses: 总类数
            - totalMethods: 总方法数
            - avgCyclomaticComplexity: 平均圈复杂度
            - avgCognitiveComplexity: 平均认知复杂度
            - highComplexityMethods: 高复杂度方法列表
              - className: 类名
              - methodName: 方法名
              - cyclomaticComplexity: 圈复杂度
              - cognitiveComplexity: 认知复杂度

            **业务规则:**
            - 返回最新生成的快照
            - 无快照时返回 404

            **使用场景:**
            - 当前质量状态查看
            - 仪表盘数据展示
            """,
        operationId = "getLatestComplexitySnapshot",
        tags = {"质量监控"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "暂无快照数据"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
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
