// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: SmartParserServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.parser.ParsedInvoice;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.service.SmartParserService;
import com.nexusarchive.service.parser.InvoiceParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartParserServiceImpl implements SmartParserService {

    private final List<InvoiceParser> parsers;
    private final ArcFileMetadataIndexMapper metadataIndexMapper;

    @Async
    @Override
    public void parseAndIndex(List<ArcFileContent> files) {
        log.info("Starting Async Smart Parsing for {} files", files.size());
        
        for (ArcFileContent fileContent : files) {
            try {
                processFile(fileContent);
            } catch (Exception e) {
                log.error("Error processing file: {}", fileContent.getFileName(), e);
            }
        }
        
        log.info("Smart Parsing completed");
    }

    private void processFile(ArcFileContent fileContent) {
        File file = new File(fileContent.getStoragePath());
        if (!file.exists()) {
            log.warn("File not found for parsing: {}", file.getAbsolutePath());
            return;
        }

        for (InvoiceParser parser : parsers) {
            if (parser.supports(fileContent.getFileType())) {
                log.info("Parsing file {} using {}", fileContent.getFileName(), parser.getClass().getSimpleName());
                
                ParsedInvoice result = parser.parse(file);
                
                if (result.isSuccess()) {
                    saveMetadata(fileContent, result, parser.getClass().getSimpleName());
                } else {
                    log.warn("Parsing failed for {}: {}", fileContent.getFileName(), result.getErrorMessage());
                }
                return; // Use the first matching parser
            }
        }
        
        log.debug("No suitable parser found for file type: {}", fileContent.getFileType());
    }

    private void saveMetadata(ArcFileContent fileContent, ParsedInvoice result, String parserType) {
        // Check if metadata already exists? (Optional, but good for idempotency)
        // For now, just insert.
        
        ArcFileMetadataIndex index = ArcFileMetadataIndex.builder()
                .fileId(fileContent.getId())
                .invoiceCode(result.getInvoiceCode())
                .invoiceNumber(result.getInvoiceNumber())
                .totalAmount(result.getTotalAmount())
                .sellerName(result.getSellerName())
                .issueDate(result.getIssueDate())
                .parsedTime(LocalDateTime.now())
                .parserType(parserType)
                .build();
        
        metadataIndexMapper.insert(index);
        log.info("Metadata indexed for file: {}", fileContent.getFileName());
    }
}
