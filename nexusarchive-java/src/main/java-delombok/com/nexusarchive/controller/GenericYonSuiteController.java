// Input: Spring Web、Lombok
// Output: GenericYonSuiteController 类
// Pos: YonSuite 集成 - REST 控制器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.integration.yonsuite.dto.VoucherAttachmentResponse;
import com.nexusarchive.integration.yonsuite.service.GenericYonSuiteAdapter;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.service.ErpConfigService;
import com.nexusarchive.util.SM4Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * YonSuite 通用适配器控制器
 *
 * <p>提供销售出库单等 YonSuite 数据同步接口</p>
 */
@Tag(name = "YonSuite 通用适配器", description = """
    YonSuite ERP 通用数据同步接口。

    **功能说明:**
    - 同步销售出库单列表
    - 同步单个销售出库单详情
    - 快速同步（最近 7 天）
    - 查询凭证附件

    **同步类型:**
    - 销售出库单列表: 按时间范围批量同步
    - 销售出库单详情: 单据详细信息同步
    - 凭证附件: 关联附件查询

    **数据格式:**
    - 请求格式: JSON
    - 返回格式: JSON
    - 日期格式: yyyy-MM-dd

    **认证方式:**
    - 使用 ERP 配置中的 appKey/appSecret
    - appSecret 采用 SM4 加密存储

    **使用场景:**
    - ERP 数据自动同步
    - 定时增量同步
    - 手动触发同步

    **注意事项:**
    - configId 对应 sys_erp_config 表的 ID
    - 同步操作会创建对应的档案记录
    - 大批量同步建议使用定时任务

    **权限要求:**
    - 需登录认证
    - 需有 ERP 配置管理权限
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/yonsuite/generic")
@RequiredArgsConstructor
@Slf4j
public class GenericYonSuiteController {

    private final GenericYonSuiteAdapter genericYonSuiteAdapter;
    private final ErpConfigService erpConfigService;
    private final ErpConfigMapper erpConfigMapper; // 直接查询 Mapper 以获取完整配置（包含 appSecret）

    /**
     * 同步销售出库单列表
     *
     * @param configId  ERP配置ID
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate   结束日期 (yyyy-MM-dd)
     * @return 同步结果
     */
    @PostMapping("/salesout/sync")
    @Operation(
        summary = "同步销售出库单列表",
        description = """
            按时间范围同步销售出库单列表。

            **查询参数:**
            - configId: ERP 配置 ID（对应 sys_erp_config 表）
            - startDate: 开始日期（yyyy-MM-dd）
            - endDate: 结束日期（yyyy-MM-dd）

            **返回数据包括:**
            - 同步成功数量
            - 销售出库单 ID 列表

            **使用场景:**
            - 按时间范围同步数据
            - 增量数据同步
            - 初始化历史数据
            """,
        operationId = "syncSalesOutList",
        tags = {"YonSuite 通用适配器"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "同步成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或 ERP 配置不存在"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "500", description = "同步失败")
    })
    public Result<List<String>> syncSalesOutList(
            @Parameter(description = "ERP配置ID", required = true, example = "1") @RequestParam Long configId,
            @Parameter(description = "开始日期 (yyyy-MM-dd)", required = true, example = "2024-01-01") @RequestParam String startDate,
            @Parameter(description = "结束日期 (yyyy-MM-dd)", required = true, example = "2024-01-31") @RequestParam String endDate) {

        log.info("收到销售出库单同步请求: configId={}, {} - {}", configId, startDate, endDate);

        try {
            // 获取 ERP 配置
            ErpConfig config = erpConfigService.findById(configId);
            if (config == null) {
                return Result.error("ERP 配置不存在");
            }

            JSONObject configJson = JSONUtil.parseObj(config.getConfigJson());
            String appKey = configJson.getStr("appKey");
            String appSecret = configJson.getStr("appSecret");

            // 执行同步
            List<String> syncedIds = genericYonSuiteAdapter.syncSalesOutList(appKey, appSecret, startDate, endDate);

            return Result.success("同步成功，共 " + syncedIds.size() + " 条", syncedIds);

        } catch (Exception e) {
            log.error("同步销售出库单失败", e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    /**
     * 同步单个销售出库单详情
     *
     * @param configId   ERP配置ID
     * @param salesOutId 销售出库单ID
     * @return 同步结果
     */
    @PostMapping("/salesout/detail")
    @Operation(
        summary = "同步销售出库单详情",
        description = """
            同步指定销售出库单的详细信息。

            **查询参数:**
            - configId: ERP 配置 ID
            - salesOutId: 销售出库单 ID

            **返回数据包括:**
            - 同步状态
            - 文件 ID

            **使用场景:**
            - 单据详情同步
            - 补充遗漏数据
            - 手动触发同步
            """,
        operationId = "syncSalesOutDetail",
        tags = {"YonSuite 通用适配器"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "同步成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或 ERP 配置不存在"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "销售出库单不存在"),
        @ApiResponse(responseCode = "500", description = "同步失败")
    })
    public Result<String> syncSalesOutDetail(
            @Parameter(description = "ERP配置ID", required = true, example = "1") @RequestParam Long configId,
            @Parameter(description = "销售出库单ID", required = true, example = "your_salesout_id") @RequestParam String salesOutId) {

        log.info("收到销售出库单详情同步请求: configId={}, salesOutId={}", configId, salesOutId);

        try {
            // 获取 ERP 配置
            ErpConfig config = erpConfigService.findById(configId);
            if (config == null) {
                return Result.error("ERP 配置不存在");
            }

            JSONObject configJson = JSONUtil.parseObj(config.getConfigJson());
            String appKey = configJson.getStr("appKey");
            String appSecret = configJson.getStr("appSecret");

            // 执行同步
            String fileId = genericYonSuiteAdapter.syncSalesOutDetail(appKey, appSecret, salesOutId);

            if (fileId == null) {
                return Result.error("同步失败");
            }

            return Result.success("同步成功", fileId);

        } catch (Exception e) {
            log.error("同步销售出库单详情失败", e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    /**
     * 快速同步（最近7天）
     */
    @PostMapping("/salesout/sync/recent")
    @Operation(
        summary = "快速同步最近7天销售出库单",
        description = """
            同步最近 7 天的销售出库单数据。

            **查询参数:**
            - configId: ERP 配置 ID

            **时间范围:**
            - 自动计算当前日期前 7 天

            **使用场景:**
            - 快速增量同步
            - 定时任务调用
            - 日常数据同步
            """,
        operationId = "syncRecentSalesOut",
        tags = {"YonSuite 通用适配器"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "同步成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或 ERP 配置不存在"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "500", description = "同步失败")
    })
    public Result<List<String>> syncRecentSalesOut(
            @Parameter(description = "ERP配置ID", required = true, example = "1") @RequestParam Long configId) {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String startDate = sevenDaysAgo.format(formatter);
        String endDate = today.format(formatter);

        return syncSalesOutList(configId, startDate, endDate);
    }

    /**
     * 查询凭证附件
     *
     * @param configId    ERP配置ID
     * @param businessIds 凭证ID列表
     * @return 凭证ID -> 附件列表的映射
     */
    @PostMapping("/voucher/attachments")
    @Operation(
        summary = "查询凭证附件",
        description = """
            查询指定凭证列表的附件信息。

            **查询参数:**
            - configId: ERP 配置 ID

            **请求体:**
            - 凭证 ID 列表（businessIds）

            **返回数据包括:**
            - 凭证 ID -> 附件列表的映射
            - 每个附件包含文件名、URL、大小等信息

            **附件信息包括:**
            - fileName: 文件名
            - fileUrl: 文件 URL
            - fileSize: 文件大小
            - uploadTime: 上传时间

            **使用场景:**
            - 凭证附件展示
            - 附件下载
            - 归档时获取附件
            """,
        operationId = "queryVoucherAttachments",
        tags = {"YonSuite 通用适配器"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或 ERP 配置不存在"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "500", description = "查询失败")
    })
    public Result<Map<String, List<VoucherAttachmentResponse.VoucherAttachment>>> queryVoucherAttachments(
            @Parameter(description = "ERP配置ID", required = true, example = "1") @RequestParam Long configId,
            @Parameter(description = "凭证ID列表", required = true) @RequestBody List<String> businessIds) {

        log.info("收到凭证附件查询请求: configId={}, businessIds数量={}", configId, businessIds.size());

        try {
            // 直接从 Mapper 获取 ERP 配置（包含 appSecret，不经过 sanitizeSensitiveFields）
            ErpConfig config = erpConfigMapper.selectById(configId);
            if (config == null) {
                return Result.error("ERP 配置不存在");
            }

            JSONObject configJson = JSONUtil.parseObj(config.getConfigJson());
            String appKey = configJson.getStr("appKey");
            String appSecret = configJson.getStr("appSecret");

            if (appSecret == null || appSecret.isEmpty()) {
                log.error("appSecret 为空: configId={}, configJson={}", configId, config.getConfigJson());
                return Result.error("ERP 配置中缺少 appSecret");
            }

            // 解密 appSecret (SM4 加密)
            String decryptedAppSecret = SM4Utils.decrypt(appSecret);

            // 执行查询
            Map<String, List<VoucherAttachmentResponse.VoucherAttachment>> attachments =
                    genericYonSuiteAdapter.queryVoucherAttachments(appKey, decryptedAppSecret, businessIds);

            return Result.success("查询成功，共 " + attachments.size() + " 个凭证有附件", attachments);

        } catch (Exception e) {
            log.error("查询凭证附件失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
