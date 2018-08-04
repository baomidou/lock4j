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
package com.baomidou.lock.aop;

import com.baomidou.lock.LockExecutor;
import com.baomidou.lock.LockKeyGenerator;
import com.baomidou.lock.annotation.Lock4j;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 分布式锁aop处理器
 *
 * @author zengzh TaoYu
 * @since 1.0.0
 */
@Slf4j
public class LockInterceptor implements MethodInterceptor {

    private static final LockKeyGenerator LOCK_KEY_GENERATOR = new LockKeyGenerator();

    private LockExecutor lockExecutor;

    public LockInterceptor(@NonNull LockExecutor lockExecutor) {
        this.lockExecutor = lockExecutor;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        boolean locked = false;
        String lockKey = null;
        try {
            Lock4j lock4j = invocation.getMethod().getAnnotation(Lock4j.class);
            lockKey = LOCK_KEY_GENERATOR.getKeyName(invocation, lock4j);
            try {
                locked = lockExecutor.acquireLock(lockKey, lock4j.acquireTimeout(), lock4j.expire());
            } catch (Exception e) {
                log.warn("lock failed", e);
            }
            if (locked) {
                log.debug("acquireLock success");
                return invocation.proceed();
            } else {
                log.warn("lock timeout");
            }
            return null;
        } finally {
            if (locked) {
                lockExecutor.releaseLock(lockKey);
            }
        }
    }

}
