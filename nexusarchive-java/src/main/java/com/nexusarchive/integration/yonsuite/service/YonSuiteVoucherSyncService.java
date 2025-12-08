package com.nexusarchive.integration.yonsuite.service;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.*;
import com.nexusarchive.integration.yonsuite.mapper.YonVoucherMapper;
import com.nexusarchive.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * YonSuite 凭证同步服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YonSuiteVoucherSyncService {

    private static final String SOURCE_SYSTEM = "YonSuite";

    private final YonSuiteClient yonSuiteClient;
    private final YonVoucherMapper yonVoucherMapper;
    private final ArchiveService archiveService;

    /**
     * 按期间同步凭证列表 (非事务，每条记录单独处理)
     */
    public List<String> syncVouchersByPeriod(String accessToken, String accbookCode, 
                                              String periodStart, String periodEnd) {
        log.info("开始同步凭证: accbookCode={}, period={}-{}", accbookCode, periodStart, periodEnd);
        
        List<String> syncedIds = new ArrayList<>();
        int pageIndex = 1;
        int pageSize = 100;
        boolean hasMore = true;
        
        while (hasMore) {
            // 构建请求
            YonVoucherListRequest request = new YonVoucherListRequest();
            YonVoucherListRequest.Pager pager = new YonVoucherListRequest.Pager();
            pager.setPageIndex(pageIndex);
            pager.setPageSize(pageSize);
            request.setPager(pager);
            request.setAccbookCode(accbookCode);
            request.setPeriodStart(periodStart);
            request.setPeriodEnd(periodEnd);
            // 同步所有状态的凭证 (不设状态过滤)
            
            // 调用 API
            YonVoucherListResponse response = yonSuiteClient.queryVouchers(accessToken, request);
            
            if (response.getData() == null || response.getData().getRecordList() == null) {
                break;
            }
            
            List<YonVoucherListResponse.VoucherRecord> records = response.getData().getRecordList();
            
            for (YonVoucherListResponse.VoucherRecord record : records) {
                try {
                    String archiveId = processVoucherRecord(record);
                    if (archiveId != null) {
                        syncedIds.add(archiveId);
                    }
                } catch (Exception e) {
                    log.error("Failed to process voucher: {}", record.getHeader().getId(), e);
                }
            }
            
            // 检查是否还有更多
            int totalPages = (int) Math.ceil((double) response.getData().getRecordCount() / pageSize);
            hasMore = pageIndex < totalPages;
            pageIndex++;
        }
        
        log.info("凭证同步完成: 共同步 {} 条", syncedIds.size());
        return syncedIds;
    }

    /**
     * 按凭证ID同步单个凭证
     */
    @Transactional
    public String syncVoucherById(String accessToken, String voucherId) {
        log.info("同步单个凭证: voucherId={}", voucherId);
        
        YonVoucherDetailResponse response = yonSuiteClient.queryVoucherById(accessToken, voucherId);
        
        if (response.getData() == null) {
            log.warn("凭证不存在: {}", voucherId);
            return null;
        }
        
        Archive archive = yonVoucherMapper.fromDetail(response.getData(), SOURCE_SYSTEM);
        
        if (archive == null) {
            return null;
        }
        
        // 幂等性检查
        Archive existing = archiveService.getByUniqueBizId(archive.getUniqueBizId());
        
        if (existing != null) {
            if ("archived".equalsIgnoreCase(existing.getStatus())) {
                log.info("凭证已归档，跳过更新: {}", voucherId);
                return existing.getId();
            }
            // 更新草稿
            archive.setId(existing.getId());
            archiveService.updateArchive(existing.getId(), archive);
            log.info("更新凭证: {}", voucherId);
            return existing.getId();
        }
        
        // 创建新记录
        Archive saved = archiveService.createArchive(archive, "system");
        log.info("创建凭证: voucherId={}, archiveId={}", voucherId, saved.getId());
        return saved.getId();
    }

    /**
     * 处理单条凭证记录
     */
    private String processVoucherRecord(YonVoucherListResponse.VoucherRecord record) {
        Archive archive = yonVoucherMapper.fromListRecord(record, SOURCE_SYSTEM);
        
        if (archive == null) {
            return null;
        }
        
        // 幂等性检查
        Archive existing = archiveService.getByUniqueBizId(archive.getUniqueBizId());
        
        if (existing != null) {
            if ("archived".equalsIgnoreCase(existing.getStatus())) {
                return existing.getId();
            }
            archive.setId(existing.getId());
            archiveService.updateArchive(existing.getId(), archive);
            return existing.getId();
        }
        
        Archive saved = archiveService.createArchive(archive, "system");
        return saved.getId();
    }
}
