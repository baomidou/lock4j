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

package com.baomidou.lock;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.executor.LockExecutor;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import com.baomidou.lock.util.LockUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * 锁模板方法
 * </p>
 *
 * @author zengzhihong TaoYu
 */
@Slf4j
public class LockTemplate implements InitializingBean {

    private static final String PROCESS_ID = LockUtil.getLocalMAC() + LockUtil.getJvmPid();
    private final Map<String, LockExecutor> executorMap = new LinkedHashMap<>();
    @Setter
    private Lock4jProperties properties;
    @Setter
    private LockKeyBuilder lockKeyBuilder;
    @Setter
    private LockFailureStrategy lockFailureStrategy;
    @Setter
    private List<LockExecutor> executors;
    private LockExecutor primaryExecutor;

    public LockTemplate() {
    }

    public LockInfo lock(MethodInvocation invocation, Lock4j lock4j) throws Exception {
        long timeout = lock4j.acquireTimeout() == 0 ? properties.getAcquireTimeout() : lock4j.acquireTimeout();
        long expire = lock4j.expire() == 0 ? properties.getExpireTime() : lock4j.expire();
        String key = lockKeyBuilder.buildKey(invocation, lock4j.keys());
        LockExecutor lockExecutor = obtainExecutor(lock4j.executor());
        long start = System.currentTimeMillis();
        int acquireCount = 0;
        String value = PROCESS_ID + Thread.currentThread().getId();
        while (System.currentTimeMillis() - start < timeout) {
            acquireCount++;
            Object lockInstance = lockExecutor.acquire(key, value, timeout, expire);
            if (null != lockInstance) {
                return new LockInfo(key, value, expire, timeout, acquireCount, lockInstance,
                        lockExecutor);
            }
            Thread.sleep(100);
        }
        if (null != lockFailureStrategy) {
            lockFailureStrategy.onLockFailure(timeout, acquireCount);
        }
        return null;
    }

    public boolean releaseLock(LockInfo lockInfo) {
        return lockInfo.getLockExecutor().releaseLock(lockInfo.getLockKey(), lockInfo.getLockValue(),
                lockInfo.getLockInstance());
    }

    protected LockExecutor obtainExecutor(String beanName) {
        return executorMap.getOrDefault(beanName, primaryExecutor);
    }

    private String firstToLowerCase(String param) {
        return param.substring(0, 1).toLowerCase() + param.substring(1, param.indexOf("Lock"));
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        Assert.isTrue(properties.getAcquireTimeout() > 0, "tryTimeout must more than 0");
        Assert.isTrue(properties.getExpireTime() > 0, "expireTime must more than 0");
        Assert.notEmpty(executors, "executors must have at least one");

        for (LockExecutor executor : executors) {
            Class<? extends LockExecutor> executorClass = executor.getClass();
            String simpleName = executorClass.getSimpleName();
            String key = firstToLowerCase(simpleName);
            executorMap.put(key, executor);
        }

        String primary = properties.getPrimary();
        if (StringUtils.isEmpty(primary)) {
            primaryExecutor = executors.get(0);
        } else {
            primaryExecutor = executorMap.get(primary);
            Assert.notNull(primaryExecutor, "primaryExecutor must not be null");
        }
    }
}
