package com.nexusarchive.service;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.parser.InvoiceParserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 启动时自动处理器：为现有附件批量填充 highlight_meta
 * 
 * 该组件在应用启动时自动执行一次，扫描所有 PDF/OFD 类型且 highlight_meta 为空的文件，
 * 调用解析器提取坐标信息并更新数据库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HighlightMetaBatchProcessor implements ApplicationRunner {

    private final ArcFileContentMapper fileContentMapper;
    private final List<InvoiceParserService> invoiceParsers;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("========== 开始批量填充 highlight_meta ==========");

        // 查询所有 PDF/OFD 类型且 highlight_meta 为空的文件
        LambdaQueryWrapper<ArcFileContent> query = new LambdaQueryWrapper<>();
        query.and(w -> w.eq(ArcFileContent::getFileType, "PDF")
                       .or()
                       .eq(ArcFileContent::getFileType, "OFD")
                       .or()
                       .eq(ArcFileContent::getFileType, "pdf")
                       .or()
                       .eq(ArcFileContent::getFileType, "ofd"));
        query.isNull(ArcFileContent::getHighlightMeta);

        List<ArcFileContent> files = fileContentMapper.selectList(query);
        log.info("发现 {} 个待处理文件", files.size());

        int successCount = 0;
        int failCount = 0;

        for (ArcFileContent fileContent : files) {
            try {
                String fileType = fileContent.getFileType() != null ? fileContent.getFileType().toLowerCase() : "";
                String storagePath = fileContent.getStoragePath();

                if (storagePath == null || storagePath.isBlank()) {
                    log.warn("文件 {} 没有存储路径，跳过", fileContent.getId());
                    failCount++;
                    continue;
                }

                // 查找支持该类型的解析器
                InvoiceParserService parser = null;
                for (InvoiceParserService p : invoiceParsers) {
                    if (p.supports(fileType)) {
                        parser = p;
                        break;
                    }
                }

                if (parser == null) {
                    log.debug("文件 {} 类型 {} 无对应解析器，跳过", fileContent.getId(), fileType);
                    continue;
                }

                // 获取文件对象
                // storagePath 可能是绝对路径 (如 /data/archives/...) 或相对路径
                File file = null;
                
                if (storagePath.startsWith("/")) {
                    // 绝对路径，直接使用
                    file = new File(storagePath);
                } else {
                    // 相对路径，通过 FileStorageService 解析
                    file = fileStorageService.getFile(storagePath);
                }
                
                if (file == null || !file.exists()) {
                    log.warn("文件 {} 物理路径不存在: {}", fileContent.getId(), storagePath);
                    failCount++;
                    continue;
                }

                // 解析
                Map<String, Object> meta = parser.parse(file);
                if (meta != null && !meta.isEmpty()) {
                    String metaJson = objectMapper.writeValueAsString(meta);
                    fileContent.setHighlightMeta(metaJson);
                    fileContentMapper.updateById(fileContent);
                    log.info("成功处理文件: {} ({})", fileContent.getFileName(), fileContent.getId());
                    successCount++;
                }

            } catch (Exception e) {
                log.warn("处理文件 {} 失败: {}", fileContent.getId(), e.getMessage());
                failCount++;
            }
        }

        log.info("========== highlight_meta 批量处理完成 ==========");
        log.info("成功: {}, 失败: {}, 跳过: {}", successCount, failCount, files.size() - successCount - failCount);
    }
}
