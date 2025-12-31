// Input: DistributedLockService 接口
// Output: 本地 ReentrantLock 实现
// Pos: nexusarchive-java 基础设施层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 本地分布式锁实现
 * 
 * [P2-FIX] 使用 JVM 内 ReentrantLock 实现，适用于单节点部署
 * 生产多节点环境应使用 Redis 实现
 */
@Slf4j
@Service
@ConditionalOnMissingBean(name = "redisDistributedLockService")
public class LocalDistributedLockService implements DistributedLockService {
    
    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    
    @Override
    public LockHandle tryLock(String lockKey, long timeout, TimeUnit unit) {
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        try {
            boolean acquired = lock.tryLock(timeout, unit);
            if (acquired) {
                log.debug("获取本地锁成功: {}", lockKey);
                return new LocalLockHandle(lockKey, lock);
            }
            log.warn("获取本地锁失败: {}", lockKey);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    @Override
    public <T> T executeWithLock(String lockKey, Duration timeout, Supplier<T> action) {
        LockHandle handle = tryLock(lockKey, timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (handle == null) {
            throw new LockAcquisitionException(lockKey);
        }
        try {
            return action.get();
        } finally {
            handle.close();
        }
    }
    
    @Override
    public void unlock(LockHandle handle) {
        if (handle != null) {
            handle.close();
        }
    }
    
    private class LocalLockHandle implements LockHandle {
        private final String lockKey;
        private final ReentrantLock lock;
        private volatile boolean locked = true;
        
        LocalLockHandle(String lockKey, ReentrantLock lock) {
            this.lockKey = lockKey;
            this.lock = lock;
        }
        
        @Override
        public String getLockKey() {
            return lockKey;
        }
        
        @Override
        public boolean isLocked() {
            return locked;
        }
        
        @Override
        public void close() {
            if (locked) {
                locked = false;
                lock.unlock();
                log.debug("释放本地锁: {}", lockKey);
            }
        }
    }
}
