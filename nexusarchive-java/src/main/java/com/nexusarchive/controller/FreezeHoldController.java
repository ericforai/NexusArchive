// Input: Spring Framework、Spring Security、ArchiveFreezeService、CustomUserDetails、Result
// Output: FreezeHoldController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.FreezeArchiveRequest;
import com.nexusarchive.dto.request.UnfreezeArchiveRequest;
import com.nexusarchive.dto.response.FreezeHoldResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.service.ArchiveFreezePartialSuccessException;
import com.nexusarchive.service.ArchiveFreezeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案冻结/保全控制器
 *
 * 路径: /archive/freeze
 *
 * 功能：
 * 1. 冻结档案（审计/诉讼冻结）
 * 2. 解除冻结
 * 3. 查询冻结状态
 *
 * 权限：
 * - archive:freeze - 冻结/解除冻结权限
 * - archive:read - 查询权限
 */
@Slf4j
@RestController
@RequestMapping("/archive/freeze")
@RequiredArgsConstructor
@Tag(name = "Archive Freeze", description = "档案冻结/保全接口")
public class FreezeHoldController {

    private final ArchiveFreezeService archiveFreezeService;

    /**
     * 申请冻结档案
     *
     * POST /archive/freeze/apply
     *
     * @param request 冻结请求
     * @param user 当前用户
     * @return 操作结果
     */
    @PostMapping("/apply")
    @PreAuthorize("hasAnyAuthority('archive:freeze','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "FREEZE_ARCHIVE", resourceType = "ARCHIVE", description = "冻结档案")
    @Operation(summary = "冻结档案", description = "对指定档案进行冻结/保全操作")
    public Result<FreezeHoldBatchResponse> freezeArchive(
            @Valid @RequestBody FreezeArchiveRequest request,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String operatorId = user != null ? user.getId() : "system";
        String operatorName = user != null ? user.getFullName() : "系统";

        try {
            // 批量冻结
            archiveFreezeService.freezeArchives(
                    request.getArchiveIds(),
                    request.getReason(),
                    operatorId,
                    request.getExpireDate()
            );

            // 构建响应
            FreezeHoldBatchResponse response = FreezeHoldBatchResponse.builder()
                    .totalCount(request.getArchiveIds().size())
                    .successCount(request.getArchiveIds().size())
                    .failedCount(0)
                    .operatorName(operatorName)
                    .operatedAt(LocalDateTime.now())
                    .build();

            log.info("档案冻结成功: count={}, operatorId={}, reason={}",
                    request.getArchiveIds().size(), operatorId, request.getReason());

            return Result.success("档案冻结成功", response);

        } catch (ArchiveFreezePartialSuccessException e) {
            // 部分成功情况
            FreezeHoldBatchResponse response = FreezeHoldBatchResponse.builder()
                    .totalCount(request.getArchiveIds().size())
                    .successCount(e.getSuccessCount())
                    .failedCount(e.getFailedIds().size())
                    .failedIds(e.getFailedIds())
                    .operatorName(operatorName)
                    .operatedAt(LocalDateTime.now())
                    .build();

            log.warn("档案冻结部分成功: total={}, success={}, failed={}",
                    request.getArchiveIds().size(), e.getSuccessCount(), e.getFailedIds().size());

            return Result.success("档案冻结部分完成", response);

        } catch (Exception e) {
            log.error("档案冻结失败: error={}", e.getMessage(), e);
            return Result.error("档案冻结失败: " + e.getMessage());
        }
    }

    /**
     * 解除冻结
     *
     * POST /archive/freeze/{id}/release
     *
     * @param id 档案ID
     * @param request 解除请求
     * @param user 当前用户
     * @return 操作结果
     */
    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyAuthority('archive:freeze','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "UNFREEZE_ARCHIVE", resourceType = "ARCHIVE", description = "解除冻结")
    @Operation(summary = "解除冻结", description = "解除指定档案的冻结/保全状态")
    public Result<Void> unfreezeArchive(
            @PathVariable String id,
            @Valid @RequestBody UnfreezeArchiveRequest request,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String operatorId = user != null ? user.getId() : "system";

        try {
            archiveFreezeService.unfreezeArchive(id, request.getReason(), operatorId);

            log.info("档案解除冻结成功: archiveId={}, operatorId={}, reason={}",
                    id, operatorId, request.getReason());

            return Result.success("解除冻结成功", null);

        } catch (IllegalArgumentException e) {
            log.warn("解除冻结失败: archiveId={}, error={}", id, e.getMessage());
            return Result.error(400, e.getMessage());

        } catch (Exception e) {
            log.error("解除冻结失败: archiveId={}, error={}", id, e.getMessage(), e);
            return Result.error("解除冻结失败: " + e.getMessage());
        }
    }

    /**
     * 检查档案是否被冻结
     *
     * GET /archive/freeze/check/{id}
     *
     * @param id 档案ID
     * @return 冻结状态
     */
    @GetMapping("/check/{id}")
    @PreAuthorize("hasAnyAuthority('archive:read','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "检查冻结状态", description = "查询指定档案是否处于冻结状态")
    public Result<Boolean> checkFrozen(@PathVariable String id) {
        boolean frozen = archiveFreezeService.isFrozen(id);
        return Result.success(frozen);
    }

    /**
     * 批量冻结响应
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FreezeHoldBatchResponse {
        /**
         * 总数
         */
        private Integer totalCount;

        /**
         * 成功数
         */
        private Integer successCount;

        /**
         * 失败数
         */
        private Integer failedCount;

        /**
         * 失败的档案ID列表
         */
        private List<String> failedIds;

        /**
         * 操作人姓名
         */
        private String operatorName;

        /**
         * 操作时间
         */
        private LocalDateTime operatedAt;
    }
}
