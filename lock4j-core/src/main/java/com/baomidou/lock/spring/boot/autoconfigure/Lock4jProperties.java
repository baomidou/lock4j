/*
 *  Copyright (c) 2018-2022, baomidou (63976799@qq.com).
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

package com.baomidou.lock.spring.boot.autoconfigure;

import com.baomidou.lock.LockFailureStrategy;
import com.baomidou.lock.LockKeyBuilder;
import com.baomidou.lock.executor.LockExecutor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * lock4j配置
 *
 * @author zengzhihong TaoYu
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "lock4j")
public class Lock4jProperties {

    /**
     * 过期时间 单位：毫秒
     */
    private Long expire = 30000L;

    /**
     * 获取锁超时时间 单位：毫秒
     */
    private Long acquireTimeout = 3000L;

    /**
     * 获取锁失败时重试时间间隔 单位：毫秒
     */
    private Long retryInterval = 100L;

    /**
     * 默认执行器，不设置默认取容器第一个(默认注入顺序，redisson>redisTemplate>zookeeper)
     */
    private Class<? extends LockExecutor> primaryExecutor;

    /**
     * 默认失败策略，不设置存在多个时默认根据PriorityOrdered、Ordered排序规则选择|注入顺序选择
     */
    private Class<? extends LockFailureStrategy> primaryFailureStrategy;

    /**
     * 默认key生成策略，不设置存在多个时默认根据PriorityOrdered、Ordered排序规则选择|注入顺序选择
     */
    private Class<? extends LockKeyBuilder> primaryKeyBuilder;

    /**
     * 锁key前缀
     */
    private String lockKeyPrefix = "lock4j";

    /**
     * 是否允许将非可执行表达式作为字符串
     */
    private boolean allowedMakeNonExecutableExpressionsAsString = true;
}
