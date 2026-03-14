// Input: Spring Web、OfdPreviewResourceService
// Output: OfdPreviewController 类
// Pos: Web 控制器层

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.OfdPreviewResourceResponse;
import com.nexusarchive.service.ofd.OfdPreviewResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OFD 预览资源决策控制器。
 */
@Slf4j
@RestController
@RequestMapping("/ofd/preview-resource")
@RequiredArgsConstructor
@Tag(name = "OFD预览资源", description = "为前端返回 OFD 的优先预览资源与下载回退地址")
public class OfdPreviewController {

    private final OfdPreviewResourceService ofdPreviewResourceService;

    @GetMapping("/{fileId}")
    @Operation(summary = "获取 OFD 预览资源决策")
    @PreAuthorize("hasAnyAuthority('archive:read', 'archive:view', 'archive:preview', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OfdPreviewResourceResponse> getPreviewResource(@PathVariable String fileId) {
        try {
            return Result.success(ofdPreviewResourceService.resolve(fileId));
        } catch (IllegalArgumentException e) {
            return Result.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        } catch (AccessDeniedException e) {
            return Result.fail(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            log.error("获取 OFD 预览资源失败: fileId={}", fileId, e);
            return Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取 OFD 预览资源失败");
        }
    }
}
