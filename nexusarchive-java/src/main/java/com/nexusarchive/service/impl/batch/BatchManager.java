// Input: MyBatis-Plus、Lombok、Spring Framework
// Output: BatchManager 类
// Pos: 归档批次服务 - 批次管理层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.batch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.ArchiveBatchItem;
import com.nexusarchive.entity.ArchiveSubmitBatch;
import com.nexusarchive.entity.PeriodLock;
import com.nexusarchive.mapper.ArchiveBatchItemMapper;
import com.nexusarchive.mapper.ArchiveSubmitBatchMapper;
import com.nexusarchive.mapper.PeriodLockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批次管理器
 * <p>
 * 负责归档批次的创建、查询、删除等基础操作
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchManager {

    private final ArchiveSubmitBatchMapper batchMapper;
    private final ArchiveBatchItemMapper itemMapper;
    private final PeriodLockMapper periodLockMapper;

    /**
     * 创建归档批次
     */
    @Transactional
    public ArchiveSubmitBatch createBatch(String fondsId, LocalDate periodStart, LocalDate periodEnd, String createdBy) {
        // 检查期间是否已锁定
        String startPeriod = periodStart.toString().substring(0, 7);
        String endPeriod = periodEnd.toString().substring(0, 7);

        PeriodLock lock = periodLockMapper.findActiveLock(fondsId, startPeriod);
        if (lock != null && PeriodLock.TYPE_ARCHIVED.equals(lock.getLockType())) {
            throw new IllegalStateException("期间 " + startPeriod + " 已归档，不能重复创建批次");
        }

        // 检查是否有未完成的批次
        int pendingCount = batchMapper.countPendingBatchesInPeriod(fondsId, startPeriod, endPeriod);
        if (pendingCount > 0) {
            throw new IllegalStateException("该期间范围已有进行中的归档批次");
        }

        // 生成批次编号
        String batchNo = batchMapper.generateBatchNo();

        ArchiveSubmitBatch batch = ArchiveSubmitBatch.builder()
                .batchNo(batchNo)
                .fondsId(fondsId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .scopeType(ArchiveSubmitBatch.SCOPE_PERIOD)
                .status(ArchiveSubmitBatch.STATUS_PENDING)
                .voucherCount(0)
                .docCount(0)
                .fileCount(0)
                .totalSizeBytes(0L)
                .createdBy(createdBy)
                .createdTime(LocalDateTime.now())
                .lastModifiedTime(LocalDateTime.now())
                .build();

        batchMapper.insert(batch);
        log.info("创建归档批次: {} (fondsId={}, period={} ~ {})", batchNo, fondsId, periodStart, periodEnd);

        return batch;
    }

    /**
     * 获取批次详情
     */
    public ArchiveSubmitBatch getBatch(Long batchId) {
        return batchMapper.selectById(batchId);
    }

    /**
     * 分页查询批次列表
     */
    public IPage<ArchiveSubmitBatch> listBatches(Page<ArchiveSubmitBatch> page, String fondsId, String status) {
        return batchMapper.findPage(page, fondsId, status);
    }

    /**
     * 按全宗查询批次列表
     */
    public List<ArchiveSubmitBatch> listBatchesByFonds(String fondsId, String status) {
        return batchMapper.findByFondsAndStatus(fondsId, status);
    }

    /**
     * 删除批次
     */
    @Transactional
    public void deleteBatch(Long batchId) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!batch.isPending()) {
            throw new IllegalStateException("只能删除待提交状态的批次");
        }

        // 删除批次条目
        itemMapper.delete(new LambdaQueryWrapper<ArchiveBatchItem>()
                .eq(ArchiveBatchItem::getBatchId, batchId));

        // 删除批次
        batchMapper.deleteById(batchId);
        log.info("删除归档批次: {}", batch.getBatchNo());
    }
}
