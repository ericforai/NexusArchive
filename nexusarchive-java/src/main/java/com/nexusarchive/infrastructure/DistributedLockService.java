// Input: Redis (可选)、Spring 注入
// Output: 分布式锁服务接口
// Pos: nexusarchive-java 基础设施层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.infrastructure;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务接口
 * 
 * [P2-FIX] 保护关键操作（销毁、冻结等）免受并发问题
 * 
 * 支持两种实现模式：
 * 1. Redis 分布式锁（生产环境推荐）
 * 2. 本地 ReentrantLock（开发/单节点环境）
 */
public interface DistributedLockService {
    
    /**
     * 尝试获取锁
     * 
     * @param lockKey 锁键名
     * @param timeout 等待超时时间
     * @param unit 时间单位
     * @return 锁句柄，获取失败返回 null
     */
    LockHandle tryLock(String lockKey, long timeout, TimeUnit unit);
    
    /**
     * 使用锁执行操作
     * 
     * @param lockKey 锁键名
     * @param timeout 等待超时
     * @param action 执行动作
     * @return 执行结果
     * @throws LockAcquisitionException 获取锁失败
     */
    <T> T executeWithLock(String lockKey, Duration timeout, Supplier<T> action);
    
    /**
     * 释放锁
     */
    void unlock(LockHandle handle);
    
    /**
     * 锁句柄
     */
    interface LockHandle extends AutoCloseable {
        String getLockKey();
        boolean isLocked();
        @Override
        void close(); // 释放锁
    }
    
    /**
     * 获取锁失败异常
     */
    class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String lockKey) {
            super("无法获取分布式锁: " + lockKey);
        }
    }
}
