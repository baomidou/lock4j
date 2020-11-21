package com.baomidou.lock.annotation;

import com.baomidou.lock.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解
 *
 * @author zengzhihong TaoYu
 */
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Lock4j {

    /**
     * lock client
     */
    LockClient client() default LockClient.REDISSON;

    /**
     * 锁类型 目前就redisson支持
     *
     * @see LockType
     */
    LockType type() default LockType.REENTRANT;

    /**
     * KEY 默认包名+方法名
     */
    String[] keys() default "";

    /**
     * key构建器
     */
    Class<? extends LockKeyBuilder> keyBuilder() default DefaultLockKeyBuilder.class;

    /**
     * 过期时间 单位：毫秒
     * <pre>
     *     过期时间一定是要长于业务的执行时间.
     * </pre>
     */
    long expire() default 30000;

    /**
     * 获取锁超时时间 单位：毫秒
     * <pre>
     *     结合业务,建议该时间不宜设置过长,特别在并发高的情况下.
     * </pre>
     */
    long acquireTimeout() default 3000;

    /**
     * 锁失败策略 默认是抛异常 {@link DefaultLockFailureStrategy#onLockFailure(long, int)}
     */
    Class<? extends LockFailureStrategy> lockFailureStrategy() default DefaultLockFailureStrategy.class;

}
