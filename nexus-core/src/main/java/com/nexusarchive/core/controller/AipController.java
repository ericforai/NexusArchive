// Input: Archive ID
// Output: File Stream (ZIP)
// Pos: NexusCore controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.controller;

import com.nexusarchive.core.preservation.aip.AipExportService;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/archives")
@RequiredArgsConstructor
public class AipController {

    private final AipExportService aipExportService;

    @GetMapping("/{archiveId}/aip/download")
    public void downloadAip(@PathVariable String archiveId, HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"AIP-" + archiveId + ".zip\"");
        
        aipExportService.exportAip(archiveId, response.getOutputStream());
    }
}
