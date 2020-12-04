/*
 *  Copyright (c) 2018-2020, baomidou (63976799@qq.com).
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

import com.baomidou.lock.DefaultLockKeyBuilder;
import com.baomidou.lock.LockKeyBuilder;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.aop.LockAnnotationAdvisor;
import com.baomidou.lock.aop.LockInterceptor;
import com.baomidou.lock.condition.ZookeeperCondition;
import com.baomidou.lock.executor.RedisTemplateLockExecutor;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.baomidou.lock.executor.ZookeeperLockExecutor;
import lombok.Data;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.redisson.api.RedissonClient;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 分布式锁自动配置器
 *
 * @author zengzhihong TaoYu
 */
@Configuration
@AutoConfigureAfter({RedisAutoConfiguration.class, RedissonConfiguration.class})
public class LockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LockAnnotationAdvisor lockAnnotationAdvisor(LockInterceptor lockInterceptor) {
        return new LockAnnotationAdvisor(lockInterceptor, Ordered.HIGHEST_PRECEDENCE);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockKeyBuilder lockKeyBuilder() {
        return new DefaultLockKeyBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockTemplate lockTemplate(LockKeyBuilder lockKeyBuilder) {
        return new LockTemplate(lockKeyBuilder);
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public RedissonLockExecutor redissonLockExecutor() {
        return new RedissonLockExecutor();
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisTemplateLockExecutor redisTemplateLockExecutor() {
        return new RedisTemplateLockExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockInterceptor lockInterceptor(LockTemplate lockTemplate) {
        return new LockInterceptor(lockTemplate);
    }

    @Conditional(ZookeeperCondition.class)
    @ConfigurationProperties(prefix = "spring.coordinate.zookeeper")
    @Data
    public static class CoordinateConfiguration {

        private String zkServers;

        private int sessionTimeout = 30000;

        private int connectionTimeout = 5000;

        private int baseSleepTimeMs = 1000;

        private int maxRetries = 3;

        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean(CuratorFramework.class)
        public CuratorFramework curatorFramework() {
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(this.baseSleepTimeMs, this.maxRetries);
            CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                    .connectString(this.zkServers)
                    .sessionTimeoutMs(this.sessionTimeout)
                    .connectionTimeoutMs(this.connectionTimeout)
                    .retryPolicy(retryPolicy)
                    .build();
            curatorFramework.start();
            return curatorFramework;
        }

        @Bean
        public ZookeeperLockExecutor zookeeperLockExecutor() {
            return new ZookeeperLockExecutor();
        }
    }
}
