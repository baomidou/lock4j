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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

/**
 * 分布式锁原生RedisTemplate处理器
 *
 * @author zengzhihong TaoYu
 */
@Slf4j
@AllArgsConstructor
public class RedisTemplateLockExecutor implements LockExecutor {

    private static final RedisScript<String> SCRIPT_LOCK = new DefaultRedisScript<>("return redis.call('set',KEYS[1]," +
            "ARGV[1],'NX','PX',ARGV[2])", String.class);
    private static final RedisScript<String> SCRIPT_UNLOCK = new DefaultRedisScript<>("if redis.call('get',KEYS[1]) " +
            "== ARGV[1] then return tostring(redis.call('del', KEYS[1])==1) else return 'false' end", String.class);
    private static final String LOCK_SUCCESS = "OK";

    private final RedisTemplate redisTemplate;


    @Override
    public boolean acquire(String lockKey, String lockValue, long timeout, long expire) {
        Object lockResult = redisTemplate.execute(SCRIPT_LOCK,
                redisTemplate.getStringSerializer(),
                redisTemplate.getStringSerializer(),
                Collections.singletonList(lockKey),
                lockValue, String.valueOf(expire));
        return LOCK_SUCCESS.equals(lockResult);
    }

    @Override
    public boolean releaseLock(LockInfo lockInfo) {
        Object releaseResult = redisTemplate.execute(SCRIPT_UNLOCK,
                redisTemplate.getStringSerializer(),
                redisTemplate.getStringSerializer(),
                Collections.singletonList(lockInfo.getLockKey()),
                lockInfo.getLockValue());
        return Boolean.valueOf(releaseResult.toString());
    }

}
