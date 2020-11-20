package com.baomidou.lock;

import org.aopalliance.intercept.MethodInvocation;

/**
 * @author zengzhihong
 */
public interface LockKeyBuilder {

    /**
     * 构建key
     *
     * @param invocation
     * @param definitionKeys
     * @return
     */
    String buildKey(MethodInvocation invocation, String[] definitionKeys);
}
