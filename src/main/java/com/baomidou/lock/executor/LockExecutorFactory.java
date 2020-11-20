package com.baomidou.lock.executor;

import com.baomidou.lock.annotation.Lock4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author zengzhihong
 * @date 2020-01-11
 */
@Slf4j
public class LockExecutorFactory {

    @Autowired(required = false)
    private RedisTemplate redisTemplate;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    @Autowired(required = false)
    private CuratorFramework curatorFramework;

    public LockExecutor buildExecutor(Lock4j lock4j) {
        switch (lock4j.client()) {
            case ZOOKEEPER:
                return new ZookeeperLockExecutor(curatorFramework);
            case REDIS_TEMPLATE:
                return new RedisTemplateLockExecutor(redisTemplate);
            case REDISSON:
                switch (lock4j.type()) {
                    case REENTRANT:
                        return new RedissonReentrantLockExecutor(redissonClient);
                    case READ:
                        return new RedissonReadLockExecutor(redissonClient);
                    case WRITE:
                        return new RedissonWriteLockExecutor(redissonClient);
                    case Fair:
                        return new RedissonFairLockExecutor(redissonClient);
                    default:
                        throw new IllegalArgumentException("error lock4j type argument");
                }
            default:
                throw new IllegalArgumentException("error lock4j client argument");
        }
    }

}
