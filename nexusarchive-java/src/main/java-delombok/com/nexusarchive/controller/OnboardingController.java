// Input: Spring Framework、MyBatis-Plus、Swagger/OpenAPI、Jakarta EE、本地模块
// Output: OnboardingController 类
// Pos: 匹配引擎/Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.engine.matching.OnboardingService;
import com.nexusarchive.engine.matching.dto.AutoMappingResult;
import com.nexusarchive.engine.matching.dto.MappingConfirmation;
import com.nexusarchive.engine.matching.dto.OnboardingSummary;
import com.nexusarchive.engine.matching.dto.UnmatchedItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 初始化向导控制器
 *
 * PRD 来源: 匹配引擎模块
 * 帮助客户快速完成科目角色和单据类型映射配置
 */
@Tag(name = "初始化向导", description = """
    匹配引擎初始化向导接口。

    **功能说明:**
    - 扫描客户现有数据结构
    - 应用预置规则自动映射
    - 识别需要人工确认的项
    - 确认并保存映射配置

    **向导流程:**
    1. **扫描**: 分析现有科目、单据类型
    2. **应用预置**: 使用预置规则自动匹配
    3. **待确认**: 获取无法自动匹配的项
    4. **确认**: 人工确认并保存映射

    **预置套件:**
    - KIT_GENERAL: 通用套件（适用于大多数企业）
    - KIT_MANUFACTURING: 制造业套件
    - KIT_TRADE: 贸易业套件
    - KIT_SERVICE: 服务业套件

    **映射类型:**
    - 科目角色映射: 科目 → 归档角色
    - 单据类型映射: ERP 单据 → 档案类型

    **业务规则:**
    - 每个公司独立配置
    - 映射关系全局唯一
    - 确认后生效

    **使用场景:**
    - 首次部署配置
    - 新公司接入
    - 映射规则调整

    **权限要求:**
    - manage_settings 或 SYSTEM_ADMIN
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/matching/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * Step 1: 扫描客户现有数据
     */
    @PostMapping("/scan/{companyId}")
    @Operation(
        summary = "扫描现有数据",
        description = """
            扫描并分析客户现有的数据结构。

            **路径参数:**
            - companyId: 公司/法人 ID

            **返回数据包括:**
            - subjectCount: 科目数量
            - documentTypeCount: 单据类型数量
            - identifiedSubjects: 已识别的科目
            - identifiedDocumentTypes: 已识别的单据类型
            - estimatedTime: 预计配置时间（分钟）

            **扫描内容:**
            - 科目表结构
            - 单据类型列表
            - 业务字段分布
            - 数据量统计

            **使用场景:**
            - 向导第一步
            - 数据结构分析
            """,
        operationId = "scanExistingData",
        tags = {"初始化向导"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "扫描完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "公司不存在")
    })
    public Result<OnboardingSummary> scanExistingData(
            @Parameter(description = "公司/法人 ID", example = "1", required = true)
            @PathVariable Long companyId) {
        OnboardingSummary summary = onboardingService.scanExistingData(companyId);
        return Result.success(summary);
    }

    /**
     * Step 2: 应用预置规则并返回自动猜测结果
     */
    @PostMapping("/apply-preset/{companyId}")
    @Operation(
        summary = "应用预置规则",
        description = """
            使用预置规则套件自动映射数据。

            **路径参数:**
            - companyId: 公司/法人 ID

            **查询参数:**
            - kitId: 预置套件 ID（默认 KIT_GENERAL）
              - KIT_GENERAL: 通用套件
              - KIT_MANUFACTURING: 制造业套件
              - KIT_TRADE: 贸易业套件
              - KIT_SERVICE: 服务业套件

            **返回数据包括:**
            - autoMapped: 自动映射成功数量
            - needConfirm: 需要人工确认数量
            - unmatched: 无法匹配数量
            - mappings: 映射详情列表

            **映射规则:**
            - 基于科目名称相似度
            - 基于单据类型关键词
            - 基于行业特征

            **使用场景:**
            - 向导第二步
            - 快速初始化
            """,
        operationId = "applyPresetRules",
        tags = {"初始化向导"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "预置规则应用成功"),
        @ApiResponse(responseCode = "400", description = "套件 ID 不存在"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<AutoMappingResult> applyPreset(
            @Parameter(description = "公司/法人 ID", example = "1", required = true)
            @PathVariable Long companyId,
            @Parameter(description = "预置套件 ID", example = "KIT_GENERAL",
                    schema = @Schema(defaultValue = "KIT_GENERAL",
                    allowableValues = {"KIT_GENERAL", "KIT_MANUFACTURING", "KIT_TRADE", "KIT_SERVICE"}))
            @RequestParam(defaultValue = "KIT_GENERAL") String kitId) {
        AutoMappingResult result = onboardingService.applyPreset(companyId, kitId);
        return Result.success(result);
    }

    /**
     * Step 3: 获取需要人工确认的项
     */
    @GetMapping("/pending/{companyId}")
    @Operation(
        summary = "获取待确认项",
        description = """
            获取无法自动匹配、需要人工确认的映射项。

            **路径参数:**
            - companyId: 公司/法人 ID

            **返回数据包括:**
            - id: 待确认项 ID
            - type: 类型（SUBJECT/DOCUMENT_TYPE）
            - sourceName: 原始名称
            - suggestedTarget: 建议的目标
            - confidence: 匹配置信度（0-100）
            - reason: 无法自动匹配的原因

            **待确认原因:**
            - AMBIGUOUS: 名称模糊，有多个候选
            - UNKNOWN: 未知类型，不在预置规则中
            - CONFLICT: 与现有映射冲突

            **使用场景:**
            - 向导第三步
            - 人工确认界面
            """,
        operationId = "getPendingItems",
        tags = {"初始化向导"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<List<UnmatchedItem>> getPendingItems(
            @Parameter(description = "公司/法人 ID", example = "1", required = true)
            @PathVariable Long companyId) {
        List<UnmatchedItem> items = onboardingService.getPendingItems(companyId);
        return Result.success(items);
    }

    /**
     * Step 4: 确认映射
     */
    @PostMapping("/confirm/{companyId}")
    @Operation(
        summary = "确认并保存映射",
        description = """
            确认并保存映射配置，使其生效。

            **路径参数:**
            - companyId: 公司/法人 ID

            **请求参数:**
            - mappings: 映射确认列表
              - sourceId: 源 ID
              - targetType: 目标类型
              - targetId: 目标 ID
              - confirmed: 是否确认

            **业务规则:**
            - 只保存已确认的映射
            - 覆盖之前的映射关系
            - 确认后立即生效

            **使用场景:**
            - 向导第四步
            - 完成配置
            """,
        operationId = "confirmMappings",
        tags = {"初始化向导"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "映射保存成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<Void> confirmMappings(
            @Parameter(description = "公司/法人 ID", example = "1", required = true)
            @PathVariable Long companyId,
            @Parameter(description = "映射确认列表", required = true)
            @Valid @RequestBody List<MappingConfirmation> mappings) {
        onboardingService.confirmMappings(companyId, mappings);
        return Result.success();
    }
}
