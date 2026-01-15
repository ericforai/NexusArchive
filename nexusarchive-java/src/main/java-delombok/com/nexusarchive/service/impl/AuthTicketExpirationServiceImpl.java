// Input: AuthTicketExpirationService, AuthTicketMapper, DistributedLock
// Output: AuthTicketExpirationServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.entity.AuthTicket;
import com.nexusarchive.mapper.AuthTicketMapper;
import com.nexusarchive.service.AuthTicketExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 授权票据过期处理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTicketExpirationServiceImpl implements AuthTicketExpirationService {
    
    private final AuthTicketMapper authTicketMapper;
    private final StringRedisTemplate redisTemplate;
    
    private static final String LOCK_KEY = "auth_ticket:expiration:scan";
    private static final int LOCK_TIMEOUT = 3600; // 1小时
    
    @Override
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    @Transactional(rollbackFor = Exception.class)
    public int scanAndMarkExpired() {
        // 1. 获取分布式锁
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(LOCK_KEY, "locked", LOCK_TIMEOUT, TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(lockAcquired)) {
            log.info("授权票据过期扫描任务正在其他实例执行，跳过本次执行");
            return 0;
        }
        
        try {
            // 2. 查询过期的授权票据
            LocalDateTime now = LocalDateTime.now();
            List<AuthTicket> expiredTickets = authTicketMapper.findExpiredTickets(now);
            
            if (expiredTickets.isEmpty()) {
                log.info("未发现过期的授权票据");
                return 0;
            }
            
            // 3. 批量更新状态为 EXPIRED
            int count = 0;
            for (AuthTicket ticket : expiredTickets) {
                ticket.setStatus("EXPIRED");
                ticket.setLastModifiedTime(now);
                authTicketMapper.updateById(ticket);
                count++;
            }
            
            log.info("授权票据过期扫描完成: 共标记 {} 个过期票据", count);
            return count;
            
        } finally {
            // 4. 释放分布式锁
            redisTemplate.delete(LOCK_KEY);
        }
    }
}





