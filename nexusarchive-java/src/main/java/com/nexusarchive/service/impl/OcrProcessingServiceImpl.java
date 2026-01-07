// Input: MyBatis-Plus、Lombok、Spring Framework、Jackson、Java 标准库
// Output: OcrProcessingServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.OcrResult;
import com.nexusarchive.entity.ScanWorkspace;
import com.nexusarchive.mapper.ScanWorkspaceMapper;
import com.nexusarchive.service.OcrProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * OCR 处理服务实现（Mock 版本）
 *
 * <p>当前实现为 Mock 版本，模拟 OCR 识别过程：
 * <ul>
 *   <li>更新状态为 processing</li>
 *   <li>模拟 2 秒处理延迟</li>
 *   <li>生成模拟的发票识别结果</li>
 *   <li>更新状态为 review 或 failed</li>
 * </ul>
 *
 * <p>TODO: 集成真实的 OCR 引擎（Tesseract、PaddleOCR、百度 OCR 等）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrProcessingServiceImpl implements OcrProcessingService {

    private final ScanWorkspaceMapper scanWorkspaceMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void processAsync(Long id, String filePath) {
        log.info("Starting async OCR processing for workspace id: {}, file: {}", id, filePath);

        // 1. 更新状态为 processing
        ScanWorkspace workspace = scanWorkspaceMapper.selectById(id);
        if (workspace == null) {
            log.error("Workspace not found for id: {}", id);
            return;
        }

        workspace.setOcrStatus("processing");
        scanWorkspaceMapper.updateById(workspace);

        try {
            // 2. 模拟处理延迟（2秒）
            Thread.sleep(2000);

            // 3. 生成 Mock OCR 结果
            OcrResult mockResult = generateMockOcrResult(filePath);

            // 4. 更新状态为 review，保存结果
            workspace.setOcrStatus("review");
            workspace.setOverallScore(mockResult.getConfidence());
            workspace.setDocType(mockResult.getDocType());

            String resultJson = objectMapper.writeValueAsString(mockResult);
            workspace.setOcrResult(resultJson);

            scanWorkspaceMapper.updateById(workspace);

            log.info("OCR processing completed successfully for workspace id: {}", id);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("OCR processing interrupted for workspace id: {}", id, e);
            markAsFailed(id, "Processing interrupted: " + e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OCR result for workspace id: {}", id, e);
            markAsFailed(id, "Failed to serialize result: " + e.getMessage());
        } catch (Exception e) {
            log.error("OCR processing failed for workspace id: {}", id, e);
            markAsFailed(id, "Processing failed: " + e.getMessage());
        }
    }

    @Override
    public OcrResult processSync(String filePath, String engine) {
        log.info("Starting sync OCR processing for file: {}, engine: {}", filePath, engine);

        try {
            // 模拟处理延迟
            Thread.sleep(2000);

            return generateMockOcrResult(filePath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Sync OCR processing interrupted for file: {}", filePath, e);
            return OcrResult.failure("Processing interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("Sync OCR processing failed for file: {}", filePath, e);
            return OcrResult.failure("Processing failed: " + e.getMessage());
        }
    }

    /**
     * 生成模拟的 OCR 识别结果
     *
     * @param filePath 文件路径
     * @return Mock OCR 结果
     */
    private OcrResult generateMockOcrResult(String filePath) {
        // 从文件名提取一些信息作为 mock 数据
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);

        return OcrResult.builder()
                .success(true)
                .docType("invoice")
                .invoiceNumber("MOCK-" + System.currentTimeMillis())
                .invoiceDate(LocalDate.now())
                .amount(new BigDecimal("1000.00"))
                .taxAmount(new BigDecimal("130.00"))
                .totalAmount(new BigDecimal("1130.00"))
                .sellerName("模拟销货方公司")
                .sellerTaxId("91110000MA000001XX")
                .buyerName("模拟购货方公司")
                .buyerTaxId("91110000MA000002XX")
                .engine("mock")
                .confidence(85)
                .rawText("这是模拟的 OCR 识别文本内容")
                .build();
    }

    /**
     * 标记处理失败
     *
     * @param id 工作区 ID
     * @param errorMessage 错误信息
     */
    private void markAsFailed(Long id, String errorMessage) {
        try {
            ScanWorkspace workspace = scanWorkspaceMapper.selectById(id);
            if (workspace != null) {
                workspace.setOcrStatus("failed");
                workspace.setOcrResult(errorMessage);
                scanWorkspaceMapper.updateById(workspace);
            }
        } catch (Exception e) {
            log.error("Failed to mark workspace {} as failed: {}", id, errorMessage, e);
        }
    }
}
