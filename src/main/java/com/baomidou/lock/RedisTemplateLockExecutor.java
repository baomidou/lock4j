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

import com.baomidou.lock.util.LockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

/**
 * 分布式锁原生RedisTemplate处理器
 *
 * @author zengzh TaoYu
 * @since 1.0.0
 */
@Slf4j
public class RedisTemplateLockExecutor implements LockExecutor {

    private static final RedisScript<String> SCRIPT_LOCK = new DefaultRedisScript<>("return redis.call('set',KEYS[1],ARGV[1],'NX','PX',ARGV[2])", String.class);
    private static final RedisScript<Void> SCRIPT_UNLOCK = new DefaultRedisScript<>("if redis.call('get',KEYS[1]) == ARGV[1] then redis.call('del', KEYS[1]) end", Void.class);
    private static final String LOCK_SUCCESS = "OK";
    private static final String PROCESS_ID = LockUtil.getLocalMAC() + "_" + LockUtil.getJvmPid();

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public boolean acquireLock(String key, long acquireTimeout, long expire) throws Exception {
        long start = System.currentTimeMillis();
        String lockId = PROCESS_ID + "_" + Thread.currentThread().getId();
        while (System.currentTimeMillis() - start < acquireTimeout) {
            Object lockResult = redisTemplate.execute(SCRIPT_LOCK,
                    redisTemplate.getStringSerializer(),
                    redisTemplate.getStringSerializer(),
                    Collections.singletonList(key),
                    lockId, String.valueOf(expire));
            if (LOCK_SUCCESS.equals(lockResult)) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }


    @Override
    public void releaseLock(String key) {
        String lockId = PROCESS_ID + Thread.currentThread().getId();
        redisTemplate.execute(SCRIPT_UNLOCK,
                redisTemplate.getStringSerializer(),
                redisTemplate.getStringSerializer(),
                Collections.singletonList(key),
                lockId);
    }

}
