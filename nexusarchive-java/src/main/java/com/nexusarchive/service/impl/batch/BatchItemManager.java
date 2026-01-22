// Input: MyBatis-Plus、Lombok、Spring Framework
// Output: BatchItemManager 类
// Pos: 归档批次服务 - 条目管理层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.batch;

import com.nexusarchive.entity.ArchiveBatchItem;
import com.nexusarchive.entity.ArchiveSubmitBatch;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.mapper.ArchiveBatchItemMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批次条目管理器
 * <p>
 * 负责批次条目的添加、移除、查询等操作
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchItemManager {

    private final ArchiveBatchItemMapper itemMapper;
    private final ArcFileContentMapper voucherMapper;
    private final OriginalVoucherMapper originalVoucherMapper;

    /**
     * 向批次添加凭证
     */
    @Transactional
    public int addVouchersToBatch(ArchiveSubmitBatch batch, List<String> voucherIds) {
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }
        if (!batch.isPending()) {
            throw new IllegalStateException("只能向待提交状态的批次添加条目");
        }

        int added = 0;
        for (String voucherId : voucherIds) {
            // 检查凭证是否已在其他批次中
            int existCount = itemMapper.countVoucherInOtherBatches(voucherId);
            if (existCount > 0) {
                log.warn("凭证 {} 已在其他批次中，跳过", voucherId);
                continue;
            }

            // 获取凭证信息
            ArcFileContent voucher = voucherMapper.selectById(voucherId);
            if (voucher == null) {
                log.warn("凭证不存在: {}", voucherId);
                continue;
            }

            ArchiveBatchItem item = ArchiveBatchItem.builder()
                    .batchId(batch.getId())
                    .itemType(ArchiveBatchItem.TYPE_VOUCHER)
                    .refId(voucherId)
                    .refNo(voucher.getErpVoucherNo())
                    .status(ArchiveBatchItem.STATUS_PENDING)
                    .createdTime(LocalDateTime.now())
                    .build();

            itemMapper.insert(item);
            added++;
        }

        // 更新批次统计
        if (added > 0) {
            batch.setVoucherCount(batch.getVoucherCount() + added);
            batch.setLastModifiedTime(LocalDateTime.now());
            batch.getBatchNo(); // trigger lazy load if needed
        }

        log.info("向批次 {} 添加了 {} 张凭证", batch.getBatchNo(), added);
        return added;
    }

    /**
     * 向批次添加单据
     */
    @Transactional
    public int addDocsToBatch(ArchiveSubmitBatch batch, List<String> docIds) {
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }
        if (!batch.isPending()) {
            throw new IllegalStateException("只能向待提交状态的批次添加条目");
        }

        int added = 0;
        for (String docId : docIds) {
            // 获取单据信息
            OriginalVoucher doc = originalVoucherMapper.selectById(docId);
            if (doc == null) {
                log.warn("单据不存在: {}", docId);
                continue;
            }

            ArchiveBatchItem item = ArchiveBatchItem.builder()
                    .batchId(batch.getId())
                    .itemType(ArchiveBatchItem.TYPE_SOURCE_DOC)
                    .refId(docId)
                    .refNo(doc.getVoucherNo())
                    .status(ArchiveBatchItem.STATUS_PENDING)
                    .createdTime(LocalDateTime.now())
                    .build();

            itemMapper.insert(item);
            added++;
        }

        // 更新批次统计
        if (added > 0) {
            batch.setDocCount(batch.getDocCount() + added);
            batch.setLastModifiedTime(LocalDateTime.now());
        }

        log.info("向批次 {} 添加了 {} 张单据", batch.getBatchNo(), added);
        return added;
    }

    /**
     * 从批次移除条目
     */
    @Transactional
    public void removeItemFromBatch(ArchiveSubmitBatch batch, Long itemId) {
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在");
        }
        if (!batch.isPending()) {
            throw new IllegalStateException("只能从待提交状态的批次移除条目");
        }

        ArchiveBatchItem item = itemMapper.selectById(itemId);
        if (item == null || !item.getBatchId().equals(batch.getId())) {
            throw new IllegalArgumentException("条目不存在或不属于此批次");
        }

        itemMapper.deleteById(itemId);

        // 更新统计
        if (ArchiveBatchItem.TYPE_VOUCHER.equals(item.getItemType())) {
            batch.setVoucherCount(Math.max(0, batch.getVoucherCount() - 1));
        } else {
            batch.setDocCount(Math.max(0, batch.getDocCount() - 1));
        }
        batch.setLastModifiedTime(LocalDateTime.now());
    }

    /**
     * 获取批次的所有条目
     */
    public List<ArchiveBatchItem> getBatchItems(Long batchId) {
        return itemMapper.findByBatchId(batchId);
    }

    /**
     * 按类型获取批次条目
     */
    public List<ArchiveBatchItem> getBatchItemsByType(Long batchId, String itemType) {
        return itemMapper.findByBatchIdAndType(batchId, itemType);
    }
}
