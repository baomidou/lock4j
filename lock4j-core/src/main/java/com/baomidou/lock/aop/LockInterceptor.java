/*
 *  Copyright (c) 2018-2021, baomidou (63976799@qq.com).
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

package com.baomidou.lock.aop;

import com.baomidou.lock.LockFailureStrategy;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockKeyBuilder;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.util.StringUtils;


/**
 * 分布式锁aop处理器
 *
 * @author zengzhihong TaoYu
 */
@Slf4j
@RequiredArgsConstructor
public class LockInterceptor implements MethodInterceptor {

    private final LockTemplate lockTemplate;

    private final LockKeyBuilder lockKeyBuilder;

    private final LockFailureStrategy lockFailureStrategy;

    private final Lock4jProperties lock4jProperties;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        //fix 使用其他aop组件时,aop切了两次.
        Class<?> cls = AopProxyUtils.ultimateTargetClass(invocation.getThis());
        if (!cls.equals(invocation.getThis().getClass())) {
            return invocation.proceed();
        }
        Lock4j lock4j = invocation.getMethod().getAnnotation(Lock4j.class);
        LockInfo lockInfo = null;
        try {
            String prefix = lock4jProperties.getLockKeyPrefix() + ":";
            prefix += StringUtils.hasText(lock4j.name()) ? lock4j.name() :
                    invocation.getMethod().getDeclaringClass().getName() + invocation.getMethod().getName();
            String key = prefix + "#" + lockKeyBuilder.buildKey(invocation, lock4j.keys());
            lockInfo = lockTemplate.lock(key, lock4j.expire(), lock4j.acquireTimeout(), lock4j.executor());
            if (null != lockInfo) {
                return invocation.proceed();
            }
            // lock failure
            lockFailureStrategy.onLockFailure(key, invocation.getMethod(), invocation.getArguments());
            return null;
        } finally {
            if (null != lockInfo && lock4j.autoRelease()) {
                final boolean releaseLock = lockTemplate.releaseLock(lockInfo);
                if (!releaseLock) {
                    log.error("releaseLock fail,lockKey={},lockValue={}", lockInfo.getLockKey(),
                            lockInfo.getLockValue());
                }
            }
        }
    }

}
