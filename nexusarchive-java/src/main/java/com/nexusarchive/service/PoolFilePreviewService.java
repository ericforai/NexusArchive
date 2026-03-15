// Input: Spring Framework、Lombok、Java 标准库、等
// Output: PoolFilePreviewService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.ArcFileContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.nexusarchive.common.constants.HttpConstants;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 电子凭证池文件预览服务
 * 负责处理文件预览请求，包括多源查找和 PDF 实时生成回退
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PoolFilePreviewService {

    private final PoolService poolService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.nexusarchive.service.VoucherPdfGeneratorService pdfGeneratorService;

    /**
     * 预览文件
     *
     * @param id 文件ID
     * @return 文件流响应
     */
    public ResponseEntity<Resource> previewFile(String id) {
        log.info("请求预览文件: {}", id);

        String storagePath = null;
        String fileName = null;
        ArcFileContent fileContent = null;

        // 1. 先查 arc_file_content
        ArcFileContent file = poolService.getFileById(id);
        if (file != null) {
            storagePath = file.getStoragePath();
            fileName = file.getFileName();
            fileContent = file;
        } else {
            // 2. 如果找不到，查 arc_original_voucher_file (智能匹配关联的文件)
            log.debug("arc_file_content 未找到 {}, 尝试查询 arc_original_voucher_file", id);
            FileLookupResult lookupResult = lookupOriginalVoucherFile(id);
            storagePath = lookupResult.storagePath();
            fileName = lookupResult.fileName();

            if (storagePath == null) {
                log.error("文件不存在: {}", id);
                return ResponseEntity.notFound().build();
            }
        }

        try {
            Path filePath = Paths.get(storagePath);
            Resource resource = new UrlResource(filePath.toUri());

            // 如果文件不存在，但在 arc_file_content 表记录存在，且是 PDF，则尝试实时生成
            if (!resource.exists() && fileContent != null) {
                if (fileName.toLowerCase().endsWith(".pdf")) {
                    log.info("PDF 文件未找到，尝试实时生成: {}", filePath);
                    try {
                        // 优先使用数据库中保存的原始JSON数据
                        String sourceData = fileContent.getSourceData();
                        String voucherJson = (sourceData != null && !sourceData.isEmpty()) ? sourceData : "{}";

                        pdfGeneratorService.generatePdfForPreArchive(id, voucherJson);
                        log.debug("PDF 实时生成完成");
                        resource = new UrlResource(filePath.toUri()); // 重新加载
                    } catch (Exception e) {
                        log.error("实时生成 PDF 失败", e);
                    }
                }
            }

            if (resource.exists() || resource.isReadable()) {
                String contentType = determineContentType(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                log.error("文件无法读取: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("文件路径错误", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 从原始凭证文件表查找文件
     */
    private FileLookupResult lookupOriginalVoucherFile(String id) {
        try {
            String sql = "SELECT storage_path, file_name FROM arc_original_voucher_file WHERE id = ? AND deleted = 0";
            java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);

            if (!rows.isEmpty()) {
                String storagePath = (String) rows.get(0).get("storage_path");
                String fileName = (String) rows.get(0).get("file_name");
                log.info("从 arc_original_voucher_file 找到文件: {} -> {}", id, storagePath);
                return new FileLookupResult(storagePath, fileName);
            }
        } catch (Exception e) {
            log.debug("查询 arc_original_voucher_file 失败: {}", e.getMessage());
        }
        return new FileLookupResult(null, null);
    }

    /**
     * 根据文件扩展名确定 Content-Type
     */
    private String determineContentType(String fileName) {
        String fileNameLower = fileName.toLowerCase();
        if (fileNameLower.endsWith(".pdf")) {
            return HttpConstants.APPLICATION_PDF;
        } else if (fileNameLower.endsWith(".ofd")) {
            return HttpConstants.APPLICATION_OFD;
        } else if (fileNameLower.endsWith(".jpg") || fileNameLower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileNameLower.endsWith(".png")) {
            return "image/png";
        } else if (fileNameLower.endsWith(".xml")) {
            return "text/xml";
        }
        return "application/octet-stream";
    }

    /**
     * 文件查找结果
     */
    private record FileLookupResult(String storagePath, String fileName) {
    }
}
