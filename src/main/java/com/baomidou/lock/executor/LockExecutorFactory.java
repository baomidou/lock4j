/*
 *  Copyright (c) 2018-2020, baomidou (63976799@qq.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.baomidou.lock.executor;

import com.baomidou.lock.annotation.Lock4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author zengzhihong
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
