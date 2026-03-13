// Input: Lombok、Spring Framework、Java 标准库、本地模块、DtoMapper、IngestRequestStatusResponse
// Output: IngestController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.response.IngestRequestStatusResponse;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.IngestResponse;
import com.nexusarchive.service.IngestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;

/**
 * SIP 接收网关控制器
 * Reference: DA/T 104-2024 接口规范
 * 所有返回值使用 DTO，避免直接暴露 Entity
 */
@RestController
@RequestMapping("/v1/archive/sip")
@PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
@RequiredArgsConstructor
public class IngestController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IngestController.class);

    private final IngestService ingestService;
    private final com.nexusarchive.mapper.IngestRequestStatusMapper statusMapper;
    private final DtoMapper dtoMapper;

    /**
     * 接收会计凭证 SIP 包
     *
     * @param sipDto SIP 数据包
     * @return 处理结果
     */
    @PostMapping("/ingest")
    public Result<IngestResponse> ingestSip(@RequestBody @Validated AccountingSipDto sipDto) {
        log.info("收到 SIP 接收请求: requestId={}, source={}", sipDto.getRequestId(), sipDto.getSourceSystem());

        IngestResponse response = ingestService.ingestSip(sipDto);

        return Result.success("接收成功", response);
    }

    /**
     * 查询接收处理状态
     *
     * @param requestId 请求ID
     * @return 处理状态
     */
    @GetMapping("/status/{requestId}")
    public Result<IngestRequestStatusResponse> getStatus(@PathVariable String requestId) {
        com.nexusarchive.entity.IngestRequestStatus status = statusMapper.selectById(requestId);
        if (status == null) {
            return Result.error(404, "Request ID not found");
        }
        return Result.success(dtoMapper.toIngestRequestStatusResponse(status));
    }

    /**
     * 正式归档
     * 将凭证池中的记录转换为正式的 AIP 档案包
     *
     * @param request 归档请求（包含凭证池 ID 列表）
     * @return 归档结果
     */
    @PostMapping("/archive")
    public Result<String> archivePoolItems(@RequestBody @Validated com.nexusarchive.dto.ArchiveRequest request) {
        log.info("收到正式归档请求: poolItemIds={}", request.getPoolItemIds());

        try {
            String userId = (String) ((jakarta.servlet.http.HttpServletRequest) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes().resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST)).getAttribute("userId");
            ingestService.archivePoolItems(request.getPoolItemIds(), userId);
            return Result.success("归档成功");
        } catch (Exception e) {
            log.error("归档失败", e);
            return Result.error(500, "归档失败: " + e.getMessage());
        }
    }
}
