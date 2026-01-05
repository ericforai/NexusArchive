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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * YonSuite 通用适配器控制器
 * 提供销售出库单等同步接口
 */
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
    public Result<List<String>> syncSalesOutList(
            @RequestParam Long configId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

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
    public Result<String> syncSalesOutDetail(
            @RequestParam Long configId,
            @RequestParam String salesOutId) {

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
    public Result<List<String>> syncRecentSalesOut(@RequestParam Long configId) {
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
    public Result<Map<String, List<VoucherAttachmentResponse.VoucherAttachment>>> queryVoucherAttachments(
            @RequestParam Long configId,
            @RequestBody List<String> businessIds) {

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
