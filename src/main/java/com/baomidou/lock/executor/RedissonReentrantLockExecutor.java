package com.baomidou.lock.executor;

import com.baomidou.lock.LockInfo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁原生RedisTemplate处理器
 *
 * @author zengzhihong TaoYu
 */
@Slf4j
public class RedissonReentrantLockExecutor implements LockExecutor {

    private RLock lock;

    private RedissonClient redissonClient;

    public RedissonReentrantLockExecutor(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean acquire(String lockKey, String lockValue, long timeout, long expire) {
        try {
            lock = redissonClient.getLock(lockKey);
            return lock.tryLock(timeout, expire, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean releaseLock(LockInfo lockInfo) {
        if (lock.isHeldByCurrentThread()) {
            try {
                return lock.forceUnlockAsync().get();
            } catch (ExecutionException | InterruptedException e) {
                return false;
            }
        }
        return false;
    }

}
