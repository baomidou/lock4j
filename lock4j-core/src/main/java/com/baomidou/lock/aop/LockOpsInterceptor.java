package com.baomidou.lock.aop;

import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.spring.boot.autoconfigure.Lock4jProperties;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 基于{@link Lock4j}注解的锁操作拦截器
 *
 * @author huangchengxing
 */
@Slf4j
public class LockOpsInterceptor extends AbstractLockOpsInterceptor {

    private final LockTemplate lockTemplate;

    public LockOpsInterceptor(
        Lock4jProperties lock4jProperties, LockTemplate lockTemplate) {
        super(lock4jProperties);
        this.lockTemplate = lockTemplate;
    }

    /**
     * 进行加锁
     *
     * @param invocation 方法调用
     * @param lockOps    锁操作
     * @return 锁信息
     * @throws Throwable 调用异常
     */
    @Override
    protected Object doLock(MethodInvocation invocation, LockOps lockOps) throws Throwable {
        Lock4j annotation = lockOps.getAnnotation();
        LockInfo lockInfo = null;
        try {
            String key = resolveKey(invocation, lockOps);
            lockInfo = lockTemplate.lock(key, annotation.expire(), annotation.acquireTimeout(), annotation.executor());
            if (Objects.nonNull(lockInfo)) {
                return invocation.proceed();
            }
            // lock failure
            lockOps.getLockFailureStrategy()
                .onLockFailure(key, invocation.getMethod(), invocation.getArguments());
            return null;
        } finally {
            doUnlock(lockInfo, annotation);
        }
    }

    private void doUnlock(@Nullable LockInfo lockInfo, Lock4j annotation) {
        if (Objects.isNull(lockInfo) || annotation.autoRelease()) {
            return;
        }
        final boolean releaseLock = lockTemplate.releaseLock(lockInfo);
        if (!releaseLock) {
            log.error("releaseLock fail,lockKey={},lockValue={}", lockInfo.getLockKey(),
                lockInfo.getLockValue());
        }
    }

    private String resolveKey(MethodInvocation invocation, LockOps lockOps) {
        String prefix = lock4jProperties.getLockKeyPrefix() + ":";
        Method method = invocation.getMethod();
        Lock4j annotation = lockOps.getAnnotation();
        prefix += StringUtils.hasText(annotation.name()) ? annotation.name() :
            method.getDeclaringClass().getName() + method.getName();
        String key = prefix + "#" + lockOps.getLockKeyBuilder().buildKey(invocation, annotation.keys());
        if (log.isDebugEnabled()) {
            log.debug("generate lock key [{}] for invocation of [{}]", key, method);
        }
        return key;
    }
}
