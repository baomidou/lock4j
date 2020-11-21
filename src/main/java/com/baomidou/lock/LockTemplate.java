package com.baomidou.lock;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.executor.LockExecutor;
import com.baomidou.lock.executor.LockExecutorFactory;
import com.baomidou.lock.util.LockUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Objects;


/**
 * <p>
 * 锁模板方法
 * </p>
 *
 * @author zengzhihong TaoYu
 */
@Slf4j
public class LockTemplate {

    private static final String PROCESS_ID = LockUtil.getLocalMAC() + LockUtil.getJvmPid();

    @Setter
    private LockExecutorFactory lockExecutorFactory;

    public LockTemplate(LockExecutorFactory lockExecutorFactory) {
        this.lockExecutorFactory = lockExecutorFactory;
    }

    public LockInfo lock(MethodInvocation invocation, Lock4j lock4j) throws Exception {
        Assert.isTrue(lock4j.acquireTimeout() > 0, "tryTimeout must more than 0");
        Method buildKeyMethod = ReflectionUtils.findMethod(lock4j.keyBuilder(), "buildKey", MethodInvocation.class, String[].class);
        String key = (String) ReflectionUtils.invokeMethod(Objects.requireNonNull(buildKeyMethod), lock4j.keyBuilder().newInstance(), invocation, lock4j.keys());
        LockExecutor lockExecutor = lockExecutorFactory.buildExecutor(lock4j);
        long start = System.currentTimeMillis();
        int acquireCount = 0;
        String value = PROCESS_ID + Thread.currentThread().getId();
        while (System.currentTimeMillis() - start < lock4j.acquireTimeout()) {
            acquireCount++;
            boolean result = lockExecutor.acquire(key, value, lock4j.acquireTimeout(), lock4j.expire());
            if (result) {
                return new LockInfo(key, value, lock4j.expire(), lock4j.acquireTimeout(), acquireCount, lockExecutor);
            }
            Thread.sleep(100);
        }
        log.info("lock failed, try {} times", acquireCount);
        Method onLockFailureMethod = ReflectionUtils.findMethod(lock4j.lockFailureStrategy(), "onLockFailure", long.class, int.class);
        ReflectionUtils.invokeMethod(Objects.requireNonNull(onLockFailureMethod), lock4j.lockFailureStrategy().newInstance(), lock4j.acquireTimeout(), acquireCount);
        return null;
    }

    public boolean releaseLock(LockInfo lockInfo) {
        return lockInfo.getLockExecutor().releaseLock(lockInfo);
    }
}
