// Input: Java Standard Library, MyBatis, Spring Transaction
// Output: BatchNumberGenerator
// Pos: Service Layer

package com.nexusarchive.service.collection;

import com.nexusarchive.mapper.CollectionBatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * 批次编号生成器
 *
 * 负责批次编号、上传令牌生成
 * <p>
 * 使用 PostgreSQL 函数 next_batch_no() + pg_advisory_xact_lock 保证并发安全
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BatchNumberGenerator {

    private static final DateTimeFormatter BATCH_NO_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd");

    // 上限检查已移至 PostgreSQL 函数 next_batch_no() 内部

    private final CollectionBatchMapper batchMapper;

    /**
     * 生成批次编号
     * <p>格式: COL-YYYYMMDD-NNNNN (NNNNN为当日递增序号)</p>
     * <p>使用 PostgreSQL advisory lock 函数 next_batch_no() 保证并发安全</p>
     *
     * @return 批次编号
     */
    @Transactional(transactionManager = "transactionManager", propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public String generateBatchNo() {
        String datePart = LocalDateTime.now().format(BATCH_NO_FORMATTER);

        // 委托给 PostgreSQL 函数，内部使用 pg_advisory_xact_lock 保证唯一性
        String batchNo = batchMapper.nextBatchNo(datePart);

        log.debug("生成批次号: {}", batchNo);
        return batchNo;
    }

    /**
     * 生成上传令牌
     * <p>注意: 生产环境应使用JWT</p>
     *
     * @param batchId 批次ID
     * @param userId 用户ID
     * @return 上传令牌
     */
    public String generateUploadToken(Long batchId, String userId) {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
