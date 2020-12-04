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
import com.baomidou.lock.exception.LockException;
import com.baomidou.lock.executor.LockExecutor;
import com.baomidou.lock.executor.RedisTemplateLockExecutor;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.baomidou.lock.util.LockUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * <p>
 * 锁模板方法
 * </p>
 *
 * @author zengzhihong TaoYu
 */
@Slf4j
public class LockTemplate implements ApplicationListener<ApplicationEvent> {

    private static final String PROCESS_ID = LockUtil.getLocalMAC() + LockUtil.getJvmPid();

    @Setter
    private LockKeyBuilder lockKeyBuilder;
    @Setter
    private LockFailureStrategy lockFailureStrategy = new DefaultLockFailureStrategy();
    @Setter
    private Map<String, LockExecutor> executorMap = new LinkedHashMap<>();

    public LockTemplate() {
    }

    public LockInfo lock(MethodInvocation invocation, Lock4j lock4j) throws Exception {
        Assert.isTrue(lock4j.acquireTimeout() > 0, "tryTimeout must more than 0");
        String key = lockKeyBuilder.buildKey(invocation, lock4j.keys());
        LockExecutor lockExecutor = obtainExecutor(lock4j.executor());
        long start = System.currentTimeMillis();
        int acquireCount = 0;
        String value = PROCESS_ID + Thread.currentThread().getId();
        while (System.currentTimeMillis() - start < lock4j.acquireTimeout()) {
            acquireCount++;
            Object lockInstance = lockExecutor.acquire(key, value, lock4j.acquireTimeout(), lock4j.expire());
            if (null != lockInstance) {
                return new LockInfo(key, value, lock4j.expire(), lock4j.acquireTimeout(), acquireCount, lockInstance,
                        lockExecutor);
            }
            Thread.sleep(100);
        }
        if (null != lockFailureStrategy) {
            lockFailureStrategy.onLockFailure(lock4j.acquireTimeout(), acquireCount);
        }
        return null;
    }

    public boolean releaseLock(LockInfo lockInfo) {
        return lockInfo.getLockExecutor().releaseLock(lockInfo.getLockKey(), lockInfo.getLockValue(),
                lockInfo.getLockInstance());
    }

    protected LockExecutor obtainExecutor(String beanName) {
        boolean isEmptyBeanName = "".equals(beanName);
        if (isEmptyBeanName) {
            final String redissonBeanName = firstToLowerCase(RedissonLockExecutor.class.getSimpleName());
            final String redisTemplateBeanName = firstToLowerCase(RedisTemplateLockExecutor.class.getSimpleName());
            if (executorMap.containsKey(redissonBeanName)) {
                return executorMap.get(redissonBeanName);
            }
            if (executorMap.containsKey(redisTemplateBeanName)) {
                return executorMap.get(redisTemplateBeanName);
            }
            return executorMap.entrySet().iterator().next().getValue();
        }
        return executorMap.get(beanName);

    }

    private String firstToLowerCase(String param) {
        return param.substring(0, 1).toLowerCase() + param.substring(1);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            final ApplicationContext applicationContext = ((ContextRefreshedEvent) event).getApplicationContext();
            this.executorMap = applicationContext.getBeansOfType(LockExecutor.class);
            if (CollectionUtils.isEmpty(executorMap)) {
                throw new LockException("require least 1 bean of Type com.baomidou.lock.executor.LockExecutor");
            }
        }
    }
}
