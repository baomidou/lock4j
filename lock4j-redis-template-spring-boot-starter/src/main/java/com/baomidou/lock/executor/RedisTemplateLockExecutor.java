/*
 *  Copyright (c) 2018-2022, baomidou (63976799@qq.com).
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

import com.baomidou.lock.exception.LockFailureException;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

/**
 * 分布式锁原生RedisTemplate处理器
 *
 * @author zengzhihong TaoYu
 */
@Slf4j
@RequiredArgsConstructor
public class RedisTemplateLockExecutor extends AbstractLockExecutor<String> {

    private static final RedisScript<String> SCRIPT_LOCK = new DefaultRedisScript<>("return redis.call('set',KEYS[1]," +
            "ARGV[1],'NX','PX',ARGV[2])", String.class);
    private static final RedisScript<String> SCRIPT_UNLOCK = new DefaultRedisScript<>("if redis.call('get',KEYS[1]) " +
            "== ARGV[1] then return tostring(redis.call('del', KEYS[1])==1) else return 'false' end", String.class);
    private static final RedisScript<Boolean> SCRIPT_RENEWAL = new DefaultRedisScript<>("if redis.call('exists', KEYS[1], ARGV[1]) == 1  " +
            "then return redis.call('expire', KEYS[1], ARGV[2]) else  return 0  end", Boolean.class);
    private static final String LOCK_SUCCESS = "OK";

    private final StringRedisTemplate redisTemplate;
    private final Lock4jProperties lock4jProperties;

    @Override
    public boolean renewal() {
        return true;
    }

    @Override
    public String acquire(String lockKey, String lockValue, long expire, long acquireTimeout) {

       final long newExpire = expire > 0 ? expire : lock4jProperties.getExpire();

        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> redisTemplate.execute(SCRIPT_LOCK,
                redisTemplate.getStringSerializer(),
                redisTemplate.getStringSerializer(),
                Collections.singletonList(lockKey),
                lockValue, String.valueOf(newExpire)))
        .thenApply(acquired -> {
            //成功开始续期且传-1时开始续期
            if (LOCK_SUCCESS.equals(acquired)&&expire==-1) {
                renewExpiration(newExpire,lockKey,lockValue);
            }
            return acquired;
        });
        String lock;
        try {
            lock = cf.get();
        } catch (Exception e) {
            throw new LockFailureException("锁获取失败！");
        }
        final boolean locked = LOCK_SUCCESS.equals(lock);
        return obtainLockInstance(locked, lock);
    }

    @Override
    public boolean releaseLock(String key, String value, String lockInstance) {
        String releaseResult = redisTemplate.execute(SCRIPT_UNLOCK,
                redisTemplate.getStringSerializer(),
                redisTemplate.getStringSerializer(),
                Collections.singletonList(key), value);
        return Boolean.parseBoolean(releaseResult);
    }


    private void renewExpiration(long expire,String lockKey, String lockValue) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (Boolean.TRUE.equals(redisTemplate.execute(SCRIPT_RENEWAL,Collections.singletonList(lockKey),lockValue, String.valueOf(expire)))) {
                    renewExpiration(expire,lockKey,lockValue);
                }
            }
        }, expire * 1000 / 3);
    }


}
