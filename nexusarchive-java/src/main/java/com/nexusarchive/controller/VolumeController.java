// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: VolumeController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.service.VolumeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组卷与归档审核 Controller
 * 符合 DA/T 104-2024 组卷规范
 */
@RestController
@RequestMapping("/volumes")
@RequiredArgsConstructor
@Slf4j
public class VolumeController {

    private final VolumeService volumeService;

    /**
     * 按月自动组卷
     * POST /api/volumes/assemble
     */
    @PostMapping("/assemble")
    public ResponseEntity<Map<String, Object>> assembleByMonth(@RequestBody AssembleRequest request) {
        log.info("请求组卷: fiscalPeriod={}", request.getFiscalPeriod());
        
        Volume volume = volumeService.assembleByMonth(request.getFiscalPeriod());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "组卷成功");
        result.put("data", volume);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取案卷列表
     * GET /api/volumes
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getVolumeList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status) {
        
        Page<Volume> pageResult = volumeService.getVolumeList(page, limit, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", Map.of(
                "records", pageResult.getRecords(),
                "total", pageResult.getTotal(),
                "page", pageResult.getCurrent(),
                "limit", pageResult.getSize()
        ));
        return ResponseEntity.ok(result);
    }

    /**
     * 获取案卷详情
     * GET /api/volumes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVolumeDetail(@PathVariable String id) {
        Volume volume = volumeService.getVolumeById(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", volume);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取卷内文件列表
     * GET /api/volumes/{id}/files
     */
    @GetMapping("/{id}/files")
    public ResponseEntity<Map<String, Object>> getVolumeFiles(@PathVariable String id) {
        List<Archive> files = volumeService.getVolumeFiles(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", files);
        return ResponseEntity.ok(result);
    }

    /**
     * 提交案卷审核
     * POST /api/volumes/{id}/submit-review
     */
    @PostMapping("/{id}/submit-review")
    public ResponseEntity<Map<String, Object>> submitForReview(@PathVariable String id) {
        volumeService.submitForReview(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已提交审核");
        return ResponseEntity.ok(result);
    }

    /**
     * 审核通过并归档
     * POST /api/volumes/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveArchival(
            @PathVariable String id,
            @RequestParam(defaultValue = "system") String reviewerId) {
        
        volumeService.approveArchival(id, reviewerId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "归档成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 审核驳回
     * POST /api/volumes/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectArchival(
            @PathVariable String id,
            @RequestBody RejectRequest request) {
        
        volumeService.rejectArchival(id, request.getReviewerId(), request.getReason());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已驳回");
        return ResponseEntity.ok(result);
    }

    /**
     * 移交档案管理部门
     * POST /api/volumes/{id}/handover
     */
    @PostMapping("/{id}/handover")
    public ResponseEntity<Map<String, Object>> handoverToArchives(@PathVariable String id) {
        
        volumeService.handoverToArchives(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "移交成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 获取归档登记表
     * GET /api/volumes/{id}/registration-form
     */
    @GetMapping("/{id}/registration-form")
    public ResponseEntity<Map<String, Object>> getRegistrationForm(@PathVariable String id) {
        Map<String, Object> form = volumeService.generateRegistrationForm(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", form);
        return ResponseEntity.ok(result);
    }

    /**
     * 导出案卷 AIP 包
     * GET /api/volumes/{id}/export-aip
     */
    @GetMapping("/{id}/export-aip")
    public ResponseEntity<org.springframework.core.io.Resource> exportAipPackage(@PathVariable String id) throws java.io.IOException {
        java.io.File zipFile = volumeService.exportAipPackage(id);
        
        org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(new java.io.FileInputStream(zipFile));
        
        String filename = zipFile.getName();
        
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipFile.length())
                .body(resource);
    }

    @Data
    public static class AssembleRequest {
        private String fiscalPeriod; // YYYY-MM
    }

    @Data
    public static class RejectRequest {
        private String reviewerId;
        private String reason;
    }
}
