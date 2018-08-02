/**
 * ﻿Copyright © 2018 organization 苞米豆
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.lock;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁原生Redisson处理器
 *
 * @author zengzh TaoYu
 * @since 1.1.0
 */
@Slf4j
public class RedissonLockExecutor implements LockExecutor {

    RedissonClient redissonClient;

    public RedissonLockExecutor(@NonNull RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean acquireLock(String key, long acquireTimeout, long expire)throws  Exception {
        return redissonClient.getLock(key).tryLock(acquireTimeout, expire, TimeUnit.MILLISECONDS);
    }

    @Override
    public void releaseLock(String key) {
        RLock rLock = redissonClient.getLock(key);
        if (rLock.isHeldByCurrentThread()) {
            rLock.unlockAsync();
        }
    }

}