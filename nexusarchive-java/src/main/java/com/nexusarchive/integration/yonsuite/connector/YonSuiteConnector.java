// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: YonSuiteConnector 类
// Pos: 集成模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.connector;

import com.nexusarchive.integration.core.connector.SourceConnector;
import com.nexusarchive.integration.core.context.SyncContext;
import com.nexusarchive.integration.core.model.FileAttachmentDTO;
import com.nexusarchive.integration.core.model.UnifiedDocumentDTO;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.YonAttachmentListResponse;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListRequest;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;
import com.nexusarchive.integration.yonsuite.mapper.YonVoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * YonSuite 连接器实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class YonSuiteConnector implements SourceConnector {

    private final YonSuiteClient yonSuiteClient;
    private final YonVoucherMapper yonVoucherMapper;

    @Override
    public String getConnectorType() {
        return "YONSUITE";
    }

    @Override
    public String getDisplayName() {
        return "用友 YonSuite";
    }

    @Override
    public List<UnifiedDocumentDTO> fetchDocuments(SyncContext context) {
        log.info("YonSuiteConnector fetchDocuments: accbook={}, period={}-{}",
                context.getAccountBookCode(), context.getStartDate(), context.getEndDate());

        // 转换日期为 YYYY-MM 格式用于查询 (YonSuite API 限制)
        // 假设 SyncContext 的 startDate 和 endDate 在同一个期间，或者我们只用 syncByPeriod
        // 这里简化逻辑，只取 StartDate 的年月
        String periodStart = context.getStartDate().toString().substring(0, 7);
        String periodEnd = context.getEndDate().toString().substring(0, 7);

        List<UnifiedDocumentDTO> results = new ArrayList<>();
        int pageIndex = 1;
        int pageSize = 100;
        boolean hasMore = true;

        while (hasMore) {
            YonVoucherListRequest request = new YonVoucherListRequest();
            YonVoucherListRequest.Pager pager = new YonVoucherListRequest.Pager();
            pager.setPageIndex(pageIndex);
            pager.setPageSize(pageSize);
            request.setPager(pager);
            request.setAccbookCode(context.getAccountBookCode());
            request.setPeriodStart(periodStart);
            request.setPeriodEnd(periodEnd);

            YonVoucherListResponse response = yonSuiteClient.queryVouchers(context.getAccessToken(), request);

            if (response == null || response.getData() == null || response.getData().getRecordList() == null) {
                break;
            }

            for (YonVoucherListResponse.VoucherRecord record : response.getData().getRecordList()) {
                UnifiedDocumentDTO doc = yonVoucherMapper.toUnifiedDocument(record);
                if (doc != null) {
                    results.add(doc);
                }
            }

            int totalPages = (int) Math.ceil((double) response.getData().getRecordCount() / pageSize);
            hasMore = pageIndex < totalPages;
            pageIndex++;
        }

        return results;
    }

    @Override
    public UnifiedDocumentDTO fetchDocumentDetail(SyncContext context, String docId) {
        // YonSuite Detail API
        // 注意：docId 是 sourceId
        YonVoucherDetailResponse response = yonSuiteClient.queryVoucherById(context.getAccessToken(), docId);
        if (response != null && response.getData() != null) {
            return yonVoucherMapper.toUnifiedDocument(response.getData());
        }
        return null;
    }

    @Override
    public List<FileAttachmentDTO> fetchAttachments(SyncContext context, String docId) {
        YonAttachmentListResponse response = yonSuiteClient.queryVoucherAttachments(context.getAccessToken(), docId);
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }

        return response.getData().stream().map(att -> FileAttachmentDTO.builder()
                .id(att.getId())
                .fileName(att.getFileName())
                .fileType(att.getFileExtension())
                .fileSize(att.getFileSize())
                .downloadUrl(att.getUrl())
                .build()).collect(Collectors.toList());
    }

    @Override
    public InputStream downloadFile(SyncContext context, String url) {
        return yonSuiteClient.downloadStream(url);
    }
}
