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

import com.baomidou.lock.DefaultLockFailureStrategy;
import com.baomidou.lock.DefaultLockKeyBuilder;
import com.baomidou.lock.LockFailureStrategy;
import com.baomidou.lock.LockKeyBuilder;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.aop.LockAnnotationAdvisor;
import com.baomidou.lock.aop.LockInterceptor;
import com.baomidou.lock.executor.LockExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * 分布式锁自动配置器
 *
 * @author zengzhihong TaoYu
 */
@Configuration
@EnableConfigurationProperties(Lock4jProperties.class)
@RequiredArgsConstructor
public class LockAutoConfiguration {

    private final Lock4jProperties properties;

    @SuppressWarnings("rawtypes")
    @Bean
    @ConditionalOnMissingBean
    public LockTemplate lockTemplate(List<LockExecutor> executors) {
        LockTemplate lockTemplate = new LockTemplate();
        lockTemplate.setProperties(properties);
        lockTemplate.setExecutors(executors);
        return lockTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public LockKeyBuilder lockKeyBuilder() {
        return new DefaultLockKeyBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockFailureStrategy lockFailureStrategy() {
        return new DefaultLockFailureStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockInterceptor lockInterceptor(@Lazy LockTemplate lockTemplate, LockKeyBuilder lockKeyBuilder,
                                           LockFailureStrategy lockFailureStrategy) {
        return new LockInterceptor(lockTemplate, lockKeyBuilder, lockFailureStrategy, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockAnnotationAdvisor lockAnnotationAdvisor(LockInterceptor lockInterceptor) {
        return new LockAnnotationAdvisor(lockInterceptor, Ordered.HIGHEST_PRECEDENCE);
    }

}
