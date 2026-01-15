// Input: MyBatis-Plus、BorrowingMapper、StringRedisTemplate、Scheduled
// Output: BorrowExpirationServiceImpl 类
// Pos: 服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nexusarchive.modules.borrowing.domain.Borrowing;
import com.nexusarchive.modules.borrowing.domain.BorrowingStatus;
import com.nexusarchive.modules.borrowing.infra.mapper.BorrowingMapper;
import com.nexusarchive.service.BorrowExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 借阅到期管理服务实现
 *
 * 实现要点：
 * 1. 使用分布式锁（Redis）防止多实例重复执行
 * 2. 每小时执行一次扫描任务
 * 3. 将过期借阅状态变更为 EXPIRED
 * 4. 支持到期前提醒功能
 *
 * PRD 来源: 2026-01-10-query-user-borrow-design.md 第7.3节
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowExpirationServiceImpl implements BorrowExpirationService {

    private static final String LOCK_KEY = "borrow:expiration:scan:lock";
    private static final int LOCK_TIMEOUT_SECONDS = 3600; // 1小时超时
    private static final int DEFAULT_REMINDER_DAYS = 3; // 默认提前3天提醒

    private final BorrowingMapper borrowingMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 定时任务：每小时执行一次
     * 使用分布式锁确保仅单实例执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scheduledScan() {
        if (tryLock()) {
            try {
                log.info("开始执行借阅到期扫描任务");
                int count = scanAndMarkExpired();
                if (count > 0) {
                    log.info("借阅到期扫描完成，发现 {} 条过期记录", count);
                } else {
                    log.debug("借阅到期扫描完成，无过期记录");
                }

                // 检查即将到期的借阅
                long upcoming = getUpcomingExpirations(DEFAULT_REMINDER_DAYS);
                if (upcoming > 0) {
                    log.info("发现 {} 条即将到期({}天内)的借阅记录", upcoming, DEFAULT_REMINDER_DAYS);
                    // TODO: 发送提醒通知（邮件/站内信等）
                }
            } finally {
                releaseLock();
            }
        } else {
            log.debug("其他实例正在执行借阅到期扫描任务，跳过本次执行");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanAndMarkExpired() {
        // 查询所有已批准但已过期的借阅记录
        LambdaQueryWrapper<Borrowing> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Borrowing::getStatus, List.of(
                BorrowingStatus.APPROVED.getCode(),
                BorrowingStatus.BORROWED.getCode()
        ));
        queryWrapper.lt(Borrowing::getExpectedReturnDate, LocalDate.now());

        List<Borrowing> expiredBorrowings = borrowingMapper.selectList(queryWrapper);

        if (expiredBorrowings.isEmpty()) {
            return 0;
        }

        // 批量更新状态为 EXPIRED
        int updatedCount = 0;
        for (Borrowing borrowing : expiredBorrowings) {
            LambdaUpdateWrapper<Borrowing> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Borrowing::getId, borrowing.getId());
            // 确保状态未被并发修改
            updateWrapper.in(Borrowing::getStatus, List.of(
                    BorrowingStatus.APPROVED.getCode(),
                    BorrowingStatus.BORROWED.getCode()
            ));
            updateWrapper.set(Borrowing::getStatus, BorrowingStatus.OVERDUE.getCode());

            int updated = borrowingMapper.update(null, updateWrapper);
            if (updated > 0) {
                updatedCount++;
                log.debug("借阅记录 {} 已标记为过期（用户: {}, 档案: {}）",
                        borrowing.getId(), borrowing.getUserId(), borrowing.getArchiveId());
            }
        }

        return updatedCount;
    }

    @Override
    public int manualScan() {
        return scanAndMarkExpired();
    }

    @Override
    public long getUpcomingExpirations(int days) {
        LocalDate reminderDate = LocalDate.now().plusDays(days);

        LambdaQueryWrapper<Borrowing> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Borrowing::getStatus, BorrowingStatus.APPROVED.getCode());
        queryWrapper.ge(Borrowing::getExpectedReturnDate, LocalDate.now());
        queryWrapper.le(Borrowing::getExpectedReturnDate, reminderDate);

        return borrowingMapper.selectCount(queryWrapper);
    }

    /**
     * 尝试获取分布式锁
     */
    private boolean tryLock() {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, "locked", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock() {
        redisTemplate.delete(LOCK_KEY);
    }
}
