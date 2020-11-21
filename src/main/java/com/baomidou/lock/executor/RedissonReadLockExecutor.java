package com.baomidou.lock.executor;

import com.baomidou.lock.LockInfo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁原生RedisTemplate处理器
 *
 * @author zengzhihong TaoYu
 */
@Slf4j
public class RedissonReadLockExecutor implements LockExecutor {

    private RReadWriteLock lock;

    private RedissonClient redissonClient;

    public RedissonReadLockExecutor(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean acquire(String lockKey, String lockValue, long timeout, long expire) {
        try {
            lock = redissonClient.getReadWriteLock(lockKey);
            return lock.readLock().tryLock(timeout, expire, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean releaseLock(LockInfo lockInfo) {
        if (lock.readLock().isHeldByCurrentThread()) {
            try {
                return lock.readLock().forceUnlockAsync().get();
            } catch (ExecutionException | InterruptedException e) {
                return false;
            }
        }
        return false;
    }

}
