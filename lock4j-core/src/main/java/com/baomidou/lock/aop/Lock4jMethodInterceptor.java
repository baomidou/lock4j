package com.baomidou.lock.aop;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * 基于{@link com.baomidou.lock.annotation.Lock4j}注解的方法拦截器
 *
 * @author huangchengxing
 * @see LockInterceptor
 * @see LockOpsInterceptor
 */
public interface Lock4jMethodInterceptor extends MethodInterceptor {
}
