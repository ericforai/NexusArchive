// Input: 预览 API
// Output: 流式数据
// Pos: NexusCore controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.controller;

import com.nexusarchive.core.compliance.AuditLogEntry;
import com.nexusarchive.core.compliance.AuditLogService;
import com.nexusarchive.core.service.StreamingPreviewService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/archives/preview")
@RequiredArgsConstructor
public class ArchivePreviewController {

    private final StreamingPreviewService previewService;
    private final AuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<Resource> preview(
            @RequestBody ArchivePreviewRequest request,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) throws IOException {
        
        // Parse Range Header
        Long rangeStart = null;
        Long rangeEnd = null;
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            try {
                if (ranges.length > 0 && !ranges[0].isEmpty()) {
                    rangeStart = Long.parseLong(ranges[0]);
                }
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    rangeEnd = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid Range header: {}", rangeHeader);
            }
        }

        // Audit Log
        AuditLogEntry entry = new AuditLogEntry();
        entry.setTraceId(UUID.randomUUID().toString()); // Simple trace ID
        entry.setOperator("current-user"); // TODO: Get from security context
        // entry.setFondsNo("current-fonds"); // Not in AuditLogEntry yet
        entry.setAction("PREVIEW");
        entry.setTarget(request.getArchiveId());
        entry.setDataSnapshot("mode=" + request.getMode() + ", range=" + rangeHeader);
        auditLogService.log(entry);

        return previewService.preview(
                request.getArchiveId(),
                request.getFileId(),
                request.getMode(),
                rangeStart,
                rangeEnd,
                entry.getTraceId(),
                entry.getOperator()
        );
    }
}
