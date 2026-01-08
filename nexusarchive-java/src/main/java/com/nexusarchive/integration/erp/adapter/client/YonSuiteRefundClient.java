// Input: Spring Framework、Lombok、YonSuiteClient、ERP DTO
// Output: YonSuiteRefundClient 类
// Pos: 集成模块 - ERP 适配器客户端

package com.nexusarchive.integration.erp.adapter.client;

import com.nexusarchive.integration.erp.dto.AttachmentDTO;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.YonRefundFileRequest;
import com.nexusarchive.integration.yonsuite.dto.YonRefundFileResponse;
import com.nexusarchive.integration.yonsuite.service.YonRefundListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * YonSuite 退款单客户端
 * 负责处理付款退款单相关的操作：查询退款单列表、同步退款单文件
 *
 * @author Agent D (基础设施工程师)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YonSuiteRefundClient {

    private final YonSuiteClient yonSuiteClient;
    private final YonRefundListService yonRefundListService;
    private static final int MAX_BATCH_SIZE = 20;
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 同步退款单文件（按日期范围查询）
     *
     * @param config    ERP 配置
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 退款单 DTO 列表
     */
    public List<VoucherDTO> syncRefundFiles(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("执行付款退款单文件同步: startDate={}, endDate={}", startDate, endDate);

        // 1. 查询退款单 ID 列表
        List<String> refundIds = yonRefundListService.queryRefundIds(config, startDate, endDate);

        if (refundIds.isEmpty()) {
            log.info("未查询到任何退款单 ID，跳过文件同步");
            return Collections.emptyList();
        }

        log.info("查询到 {} 个退款单 ID，开始同步文件", refundIds.size());

        // 2. 分批同步文件
        return syncRefundFilesBatched(config, refundIds);
    }

    /**
     * 同步退款单文件（按 ID 列表）
     *
     * @param config  ERP 配置
     * @param fileIds 退款单文件 ID 列表
     * @return 退款单 DTO 列表
     */
    public List<VoucherDTO> syncRefundFilesByIds(ErpConfig config, List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            log.info("没有提供文件 ID，跳过退款单文件同步");
            return Collections.emptyList();
        }

        if (fileIds.size() > MAX_BATCH_SIZE) {
            log.warn("文件 ID 数量超过限制({})，将分批处理", MAX_BATCH_SIZE);
            return syncRefundFilesBatched(config, fileIds);
        }

        return syncRefundFilesSingleBatch(config, fileIds);
    }

    /**
     * 分批处理退款单文件同步
     */
    private List<VoucherDTO> syncRefundFilesBatched(ErpConfig config, List<String> fileIds) {
        List<VoucherDTO> allResults = new ArrayList<>();

        for (int i = 0; i < fileIds.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, fileIds.size());
            List<String> batch = fileIds.subList(i, end);
            allResults.addAll(syncRefundFilesSingleBatch(config, batch));
        }

        return allResults;
    }

    /**
     * 单批处理退款单文件同步
     */
    private List<VoucherDTO> syncRefundFilesSingleBatch(ErpConfig config, List<String> fileIds) {
        String accessToken = getAccessToken(config);

        YonRefundFileRequest request = new YonRefundFileRequest();
        request.setFileId(fileIds);

        YonRefundFileResponse response = yonSuiteClient.queryRefundFileUrls(accessToken, request);

        if (!hasValidData(response)) {
            log.warn("查询退款单文件失败: {}", response != null ? response.getMessage() : "null response");
            return Collections.emptyList();
        }

        return convertToVoucherDTOs(response.getData());
    }

    /**
     * 获取访问令牌
     */
    private String getAccessToken(ErpConfig config) {
        String appKey = config.getAppKey();
        String appSecret = config.getAppSecret();

        if (appKey == null || appKey.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            return null;
        }

        try {
            return yonSuiteClient.getTokenWithCredentials(appKey, appSecret);
        } catch (Exception e) {
            log.warn("使用配置中的 appKey 获取 token 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查响应是否有效
     */
    private boolean hasValidData(YonRefundFileResponse response) {
        return response != null
                && "200".equals(response.getCode())
                && response.getData() != null;
    }

    /**
     * 转换为 VoucherDTO 列表
     */
    private List<VoucherDTO> convertToVoucherDTOs(List<YonRefundFileResponse.RefundFileInfo> fileList) {
        List<VoucherDTO> results = new ArrayList<>();

        for (YonRefundFileResponse.RefundFileInfo fileInfo : fileList) {
            VoucherDTO dto = VoucherDTO.builder()
                    .voucherId(fileInfo.getId())
                    .voucherNo(fileInfo.getFileName())
                    .summary("付款退款单文件: " + fileInfo.getFileName())
                    .accountPeriod(LocalDate.now().format(PERIOD_FORMATTER))
                    .status("REFUND_FILE")
                    .debitTotal(BigDecimal.ZERO)
                    .creditTotal(BigDecimal.ZERO)
                    .build();

            // 添加附件信息
            List<AttachmentDTO> attachments = new ArrayList<>();
            AttachmentDTO attachment = AttachmentDTO.builder()
                    .attachmentId(fileInfo.getId())
                    .fileName(fileInfo.getFileName())
                    .downloadUrl(fileInfo.getDownLoadUrl())
                    .fileType(getFileExtension(fileInfo.getFileName()))
                    .build();
            attachments.add(attachment);
            dto.setAttachments(attachments);

            results.add(dto);

            log.info("成功获取退款单文件: fileName={}, downloadUrl={}",
                    fileInfo.getFileName(), fileInfo.getDownLoadUrl());
        }

        log.info("付款退款单文件同步完成: 共 {} 个文件", results.size());
        return results;
    }

    /**
     * 从文件名提取扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
}
