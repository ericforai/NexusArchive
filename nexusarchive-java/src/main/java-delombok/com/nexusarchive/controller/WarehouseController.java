// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: WarehouseController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.Location;
import com.nexusarchive.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能仓库管理控制器
 *
 * <p>提供物理档案存储位置管理和智能设备控制功能</p>
 */
@Tag(name = "智能仓库管理", description = """
    智能仓库管理接口。

    **功能说明:**
    - 查询货架位置信息
    - 档案上架/下架管理
    - 环境数据监控
    - 智能设备控制

    **仓库结构:**
    - Zone: 区域（如 A 区、B 区）
    - Rack: 货架（如 A-01）
    - Shelf: 层架（如 A-01-01）
    - Location: 存储位（如 A-01-01-001）

    **环境监控:**
    - 温度: °C
    - 湿度: %RH
    - 光照: Lux
    - 门禁状态: 开/关

    **智能设备:**
    - 自动导引车 (AGV)
    - 智能货架
    - 环境传感器
    - 门禁控制器

    **使用场景:**
    - 物理档案入库
    - 档案位置查询
    - 仓库环境监控
    - 设备远程控制

    **权限要求:**
    - WAREHOUSE_MANAGE: 仓库管理权限
    - SYSTEM_ADMIN: 系统管理员
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    /**
     * 查询所有货架
     */
    @GetMapping("/shelves")
    @Operation(
        summary = "查询所有货架",
        description = """
            获取仓库中所有货架的位置信息。

            **返回数据包括:**
            - id: 货架 ID
            - code: 货架编码（如 A-01）
            - zone: 所属区域
            - row: 行号
            - column: 列号
            - capacity: 容量
            - usedCount: 已用数量
            - status: 状态（ACTIVE/MAINTENANCE/FULL）

            **使用场景:**
            - 货架列表展示
            - 存储位置选择
            - 容量规划
            """,
        operationId = "getShelves",
        tags = {"智能仓库管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<List<Location>> getShelves() {
        return Result.success(warehouseService.getShelves());
    }

    /**
     * 档案上架
     */
    @PostMapping("/items")
    @Operation(
        summary = "档案上架",
        description = """
            将档案放置到指定货架位置。

            **请求参数:**
            - shelfId: 货架 ID（必填）
            - archiveId: 档案 ID（必填）

            **业务规则:**
            - 货架必须有剩余容量
            - 档案必须已登记
            - 自动记录上架时间
            - 生成上架操作日志

            **使用场景:**
            - 归档案卷入库
            - 档案位置变更
            - 移库操作
            """,
        operationId = "addItemToShelf",
        tags = {"智能仓库管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "上架成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或货架已满"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "货架或档案不存在")
    })
    public Result<Void> addItemToShelf(
            @Parameter(description = "上架请求参数", required = true,
                    schema = @Schema(example = "{\"shelfId\": \"SHELLF-001\", \"archiveId\": \"ARCHIVE-123\"}"))
            @RequestBody Map<String, String> payload) {
        String shelfId = payload.get("shelfId");
        String archiveId = payload.get("archiveId");
        warehouseService.addItemToShelf(shelfId, archiveId);
        return Result.success();
    }

    /**
     * 档案下架
     */
    @DeleteMapping("/items/{id}")
    @Operation(
        summary = "档案下架",
        description = """
            从指定货架移除档案。

            **路径参数:**
            - id: 档案 ID

            **查询参数:**
            - shelfId: 货架 ID

            **业务规则:**
            - 自动释放货架容量
            - 记录下架时间
            - 生成交接清单
            - 需要权限验证

            **使用场景:**
            - 档案借出
            - 档案销毁提取
            - 档案移库
            """,
        operationId = "removeItemFromShelf",
        tags = {"智能仓库管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "下架成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    public Result<Void> removeItemFromShelf(
            @Parameter(description = "档案ID", required = true, example = "ARCHIVE-123") @PathVariable String id,
            @Parameter(description = "货架ID", required = true, example = "SHELF-001") @RequestParam String shelfId) {
        warehouseService.removeItemFromShelf(shelfId, id);
        return Result.success();
    }

    /**
     * 获取环境数据
     */
    @GetMapping("/environment")
    @Operation(
        summary = "获取仓库环境数据",
        description = """
            获取仓库当前的环境监控数据。

            **返回数据包括:**
            - temperature: 温度（°C）
            - humidity: 湿度（%RH）
            - light: 光照（Lux）
            - doorStatus: 门禁状态（OPEN/CLOSED）
            - lastUpdate: 最后更新时间

            **监控标准:**
            - 温度: 14-24°C
            - 湿度: 45-60%RH
            - 光照: ≤150 Lux（避光保存）

            **告警规则:**
            - 温度超出范围: 告警
            - 湿度超出范围: 告警
            - 门禁异常开启: 告警

            **使用场景:**
            - 环境监控仪表盘
            - 告警触发判定
            - 环境历史记录
            """,
        operationId = "getEnvironmentData",
        tags = {"智能仓库管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<Map<String, Object>> getEnvironmentData() {
        return Result.success(warehouseService.getEnvironmentData());
    }

    /**
     * 发送设备控制指令
     */
    @PostMapping("/racks/{id}/command")
    @Operation(
        summary = "发送设备控制指令",
        description = """
            向智能设备发送控制指令。

            **路径参数:**
            - id: 设备 ID（货架/AGV 等）

            **请求参数:**
            - action: 指令类型
              - OPEN: 开启
              - CLOSE: 关闭
              - MOVE_TO: 移动到
              - LOCK: 锁定
              - UNLOCK: 解锁

            **业务规则:**
            - 指令异步执行
            - 返回执行状态
            - 记录操作日志
            - 失败自动回滚

            **使用场景:**
            - 智能货架开闭
            - AGV 调度
            - 设备远程控制
            """,
        operationId = "sendDeviceCommand",
        tags = {"智能仓库管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "指令已发送"),
        @ApiResponse(responseCode = "400", description = "参数错误或不支持的指令"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "设备不存在"),
        @ApiResponse(responseCode = "503", description = "设备离线")
    })
    public Result<Map<String, Object>> sendCommand(
            @Parameter(description = "设备ID", required = true, example = "RACK-001") @PathVariable("id") String rackId,
            @Parameter(description = "控制指令", required = true,
                    schema = @Schema(example = "{\"action\": \"OPEN\"}"))
            @RequestBody Map<String, String> payload) {
        String action = payload.get("action");
        return Result.success(warehouseService.applyCommand(rackId, action));
    }
}
