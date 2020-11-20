/**
 * ﻿Copyright © 2018 organization 苞米豆
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
public class RedissonFairLockExecutor implements LockExecutor {

    private RLock lock;

    private RedissonClient redissonClient;

    public RedissonFairLockExecutor(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean acquire(String lockKey, String lockValue, long timeout, long expire) {
        try {
            lock = redissonClient.getFairLock(lockKey);
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
