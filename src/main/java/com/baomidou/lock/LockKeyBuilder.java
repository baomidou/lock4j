package com.baomidou.lock;

import org.aopalliance.intercept.MethodInvocation;

/**
 * @author zengzhihong
 */
public interface LockKeyBuilder {

    /**
     * 构建key
     *
     * @param invocation     invocation
     * @param definitionKeys 定义
     * @return key
     */
    String buildKey(MethodInvocation invocation, String[] definitionKeys);
}
