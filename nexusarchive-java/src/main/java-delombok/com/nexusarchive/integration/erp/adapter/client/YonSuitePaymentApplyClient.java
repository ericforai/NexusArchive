// Input: Spring Framework、Lombok、YonSuiteClient、ERP DTO
// Output: YonSuitePaymentApplyClient 类
// Pos: 集成模块 - ERP 适配器客户端
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter.client;

import com.nexusarchive.integration.erp.dto.AttachmentDTO;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.YonPaymentApplyFileRequest;
import com.nexusarchive.integration.yonsuite.dto.YonPaymentApplyFileResponse;
import com.nexusarchive.integration.yonsuite.service.YonPaymentApplySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * YonSuite 付款申请单客户端
 * 负责处理付款申请单相关的操作：查询申请单列表、同步申请单文件
 *
 * @author AI Integration Agent
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YonSuitePaymentApplyClient {

    private final YonSuiteClient yonSuiteClient;
    private final YonPaymentApplySyncService yonPaymentApplySyncService;
    private static final int MAX_BATCH_SIZE = 20;
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 同步付款申请单文件（按日期范围查询）
     *
     * @param config    ERP 配置
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 付款申请单 DTO 列表
     */
    public List<VoucherDTO> syncPaymentApplyFiles(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("执行付款申请单文件同步: startDate={}, endDate={}", startDate, endDate);

        // 1. 查询付款申请单 ID 列表
        List<String> paymentApplyIds = yonPaymentApplySyncService.queryPaymentApplyIds(config, startDate, endDate);

        if (paymentApplyIds.isEmpty()) {
            log.info("未查询到任何付款申请单 ID，跳过文件同步");
            return Collections.emptyList();
        }

        log.info("查询到 {} 个付款申请单 ID，开始同步文件", paymentApplyIds.size());

        // 2. 分批同步文件
        return syncPaymentApplyFilesBatched(config, paymentApplyIds);
    }

    /**
     * 同步付款申请单文件（按 ID 列表）
     *
     * @param config  ERP 配置
     * @param fileIds 付款申请单文件 ID 列表
     * @return 付款申请单 DTO 列表
     */
    public List<VoucherDTO> syncPaymentApplyFilesByIds(ErpConfig config, List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            log.info("没有提供文件 ID，跳过付款申请单文件同步");
            return Collections.emptyList();
        }

        if (fileIds.size() > MAX_BATCH_SIZE) {
            log.warn("文件 ID 数量超过限制({})，将分批处理", MAX_BATCH_SIZE);
            return syncPaymentApplyFilesBatched(config, fileIds);
        }

        return syncPaymentApplyFilesSingleBatch(config, fileIds);
    }

    /**
     * 分批处理付款申请单文件同步
     */
    private List<VoucherDTO> syncPaymentApplyFilesBatched(ErpConfig config, List<String> fileIds) {
        List<VoucherDTO> allResults = new ArrayList<>();

        for (int i = 0; i < fileIds.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, fileIds.size());
            List<String> batch = fileIds.subList(i, end);
            allResults.addAll(syncPaymentApplyFilesSingleBatch(config, batch));
        }

        return allResults;
    }

    /**
     * 单批处理付款申请单文件同步
     */
    private List<VoucherDTO> syncPaymentApplyFilesSingleBatch(ErpConfig config, List<String> fileIds) {
        String accessToken = getAccessToken(config);

        YonPaymentApplyFileRequest request = new YonPaymentApplyFileRequest();
        request.setFileId(fileIds);

        YonPaymentApplyFileResponse response = yonSuiteClient.queryPaymentApplyFileUrls(accessToken, request);

        if (!hasValidData(response)) {
            log.warn("查询付款申请单文件失败: {}", response != null ? response.getMessage() : "null response");
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
    private boolean hasValidData(YonPaymentApplyFileResponse response) {
        return response != null
                && "200".equals(response.getCode())
                && response.getData() != null;
    }

    /**
     * 转换为 VoucherDTO 列表
     */
    private List<VoucherDTO> convertToVoucherDTOs(List<YonPaymentApplyFileResponse.FileData> fileList) {
        List<VoucherDTO> results = new ArrayList<>();

        for (YonPaymentApplyFileResponse.FileData fileInfo : fileList) {
            VoucherDTO dto = VoucherDTO.builder()
                    .voucherId(fileInfo.getId())
                    .voucherNo(fileInfo.getFileName())
                    .summary("付款申请单文件: " + fileInfo.getFileName())
                    .accountPeriod(LocalDate.now().format(PERIOD_FORMATTER))
                    .status("PAYMENT_APPLY_FILE")
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

            log.info("成功获取付款申请单文件: fileName={}, downloadUrl={}",
                    fileInfo.getFileName(), fileInfo.getDownLoadUrl());
        }

        log.info("付款申请单文件同步完成: 共 {} 个文件", results.size());
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
