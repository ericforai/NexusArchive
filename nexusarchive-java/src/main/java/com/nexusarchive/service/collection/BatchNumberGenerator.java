// Input: Java Standard Library
// Output: BatchNumberGenerator
// Pos: Service Layer

package com.nexusarchive.service.collection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 批次编号生成器
 *
 * 负责批次编号、上传令牌生成
 */
@Service
@Slf4j
public class BatchNumberGenerator {

    private static final DateTimeFormatter BATCH_NO_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 生成批次编号
     * <p>格式: COL-YYYYMMDD-XXX (XXX为随机数)</p>
     *
     * @return 批次编号
     */
    public String generateBatchNo() {
        String datePart = LocalDateTime.now().format(BATCH_NO_FORMATTER);
        String randomPart = String.format("%03d", (int)(Math.random() * 1000));
        return "COL-" + datePart + "-" + randomPart;
    }

    /**
     * 生成上传令牌
     * <p>注意: 生产环境应使用JWT</p>
     *
     * @param batchId 批次ID
     * @param userId 用户ID
     * @return 上传令牌
     */
    public String generateUploadToken(Long batchId, Long userId) {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
