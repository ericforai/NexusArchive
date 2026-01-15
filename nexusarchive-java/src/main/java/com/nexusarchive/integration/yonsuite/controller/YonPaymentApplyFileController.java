// Input: Spring Framework、Lombok、本地模块
// Output: YonPaymentApplyFileController 类
// Pos: YonSuite 集成 - 控制器层

package com.nexusarchive.integration.yonsuite.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.common.Result;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.integration.yonsuite.dto.YonPaymentApplyFileResponse;
import com.nexusarchive.integration.yonsuite.service.YonPaymentApplyFileService;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.service.ErpConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * YonSuite 付款申请单文件接口
 * <p>
 * 提供付款申请单文件下载地址查询功能
 * </p>
 */
@Tag(name = "YonSuite 付款申请单文件", description = "付款申请单附件下载地址查询接口")
@RestController
@RequestMapping("/integration/yonsuite/payment-apply/file")
@RequiredArgsConstructor
@Slf4j
public class YonPaymentApplyFileController {

    private final YonPaymentApplyFileService paymentApplyFileService;
    private final ErpConfigService erpConfigService;
    private final ErpConfigMapper erpConfigMapper;

    /**
     * 批量查询文件下载地址
     *
     * @param configId    ERP 配置 ID
     * @param fileIds     文件 ID 列表
     * @return 文件下载信息列表
     */
    @Operation(
        summary = "查询付款申请单文件下载地址",
        description = "通过文件 ID 批量获取付款申请单文件的下载地址。最多支持 20 个文件 ID。"
    )
    @PostMapping("/url")
    public Result<List<YonPaymentApplyFileResponse.FileData>> queryFileUrls(
            @Parameter(description = "ERP 配置 ID")
            @RequestParam Long configId,

            @Parameter(description = "文件 ID 列表")
            @RequestBody List<String> fileIds) {

        log.info("查询付款申请单文件下载地址: configId={}, fileIds={}", configId, fileIds);

        try {
            // 获取 ERP 配置（使用 Mapper 直接获取，包含 appSecret）
            ErpConfig configEntity = erpConfigMapper.selectById(configId);
            if (configEntity == null) {
                return Result.error("ERP 配置不存在");
            }

            // 转换为 DTO
            com.nexusarchive.integration.erp.dto.ErpConfig config = toDto(configEntity);

            // 查询文件下载地址
            List<YonPaymentApplyFileResponse.FileData> files =
                paymentApplyFileService.queryFileUrls(config, fileIds);

            return Result.success("查询成功，共 " + files.size() + " 个文件", files);

        } catch (Exception e) {
            log.error("查询付款申请单文件下载地址失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询单个文件下载地址
     *
     * @param configId ERP 配置 ID
     * @param fileId   文件 ID
     * @return 文件下载信息
     */
    @Operation(
        summary = "查询单个文件下载地址",
        description = "通过文件 ID 获取单个付款申请单文件的下载地址"
    )
    @GetMapping("/url/{fileId}")
    public Result<YonPaymentApplyFileResponse.FileData> queryFileUrl(
            @Parameter(description = "ERP 配置 ID")
            @RequestParam Long configId,

            @Parameter(description = "文件 ID")
            @PathVariable String fileId) {

        log.info("查询单个付款申请单文件下载地址: configId={}, fileId={}", configId, fileId);

        try {
            // 获取 ERP 配置
            ErpConfig configEntity = erpConfigMapper.selectById(configId);
            if (configEntity == null) {
                return Result.error("ERP 配置不存在");
            }

            // 转换为 DTO
            com.nexusarchive.integration.erp.dto.ErpConfig config = toDto(configEntity);

            // 查询文件下载地址
            YonPaymentApplyFileResponse.FileData file =
                paymentApplyFileService.queryFileUrl(config, fileId);

            if (file == null) {
                return Result.error("文件不存在或获取失败");
            }

            return Result.success(file);

        } catch (Exception e) {
            log.error("查询单个付款申请单文件下载地址失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     * <p>
     * 用于验证服务是否正常运行
     * </p>
     */
    @Operation(summary = "健康检查", description = "检查服务是否可用")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("付款申请单文件服务正常运行");
    }

    /**
     * 将 Entity 转换为 DTO
     */
    private com.nexusarchive.integration.erp.dto.ErpConfig toDto(ErpConfig entity) {
        JSONObject configJson = JSONUtil.parseObj(entity.getConfigJson());

        return com.nexusarchive.integration.erp.dto.ErpConfig.builder()
            .id(String.valueOf(entity.getId()))
            .name(entity.getName())
            .adapterType(entity.getErpType())
            .baseUrl(configJson.getStr("baseUrl", configJson.getStr("host")))
            .appKey(configJson.getStr("appKey"))
            .appSecret(configJson.getStr("appSecret"))
            .accbookCode(configJson.getStr("accbookCode"))
            .build();
    }
}
