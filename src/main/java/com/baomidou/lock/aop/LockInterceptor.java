package com.baomidou.lock.aop;

import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.annotation.Lock4j;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;


/**
 * 分布式锁aop处理器
 *
 * @author zengzhihong TaoYu
 */
@Slf4j
public class LockInterceptor implements MethodInterceptor {

    private final LockTemplate lockTemplate;

    public LockInterceptor(LockTemplate lockTemplate) {
        this.lockTemplate = lockTemplate;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        LockInfo lockInfo = null;
        try {
            Lock4j lock4j = invocation.getMethod().getAnnotation(Lock4j.class);
            lockInfo = lockTemplate.lock(invocation, lock4j);
            if (null != lockInfo) {
                return invocation.proceed();
            }
            return null;
        } finally {
            if (null != lockInfo) {
                lockTemplate.releaseLock(lockInfo);
            }
        }
    }

}
