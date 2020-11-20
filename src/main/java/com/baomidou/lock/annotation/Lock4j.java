/**
 * ﻿Copyright © 2018 organization 苞米豆
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.lock.annotation;

import com.baomidou.lock.DefaultLockFailureStrategy;
import com.baomidou.lock.DefaultLockKeyBuilder;
import com.baomidou.lock.LockClient;
import com.baomidou.lock.LockFailureStrategy;
import com.baomidou.lock.LockKeyBuilder;
import com.baomidou.lock.LockType;

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
