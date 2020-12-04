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

import com.baomidou.lock.*;
import com.baomidou.lock.aop.LockAnnotationAdvisor;
import com.baomidou.lock.aop.LockInterceptor;
import com.baomidou.lock.condition.ZookeeperCondition;
import com.baomidou.lock.executor.LockExecutor;
import com.baomidou.lock.executor.RedisTemplateLockExecutor;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.baomidou.lock.executor.ZookeeperLockExecutor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;

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

    @Bean
    @ConditionalOnMissingBean
    public LockTemplate lockTemplate(LockKeyBuilder lockKeyBuilder, LockFailureStrategy lockFailureStrategy, List<LockExecutor> executors) {
        LockTemplate lockTemplate = new LockTemplate();
        lockTemplate.setProperties(properties);
        lockTemplate.setLockKeyBuilder(lockKeyBuilder);
        lockTemplate.setLockFailureStrategy(lockFailureStrategy);
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
    public LockInterceptor lockInterceptor() {
        return new LockInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockAnnotationAdvisor lockAnnotationAdvisor(LockInterceptor lockInterceptor) {
        return new LockAnnotationAdvisor(lockInterceptor, Ordered.HIGHEST_PRECEDENCE);
    }

    @Configuration
    @ConditionalOnClass(RedisAutoConfiguration.class)
    static class RedisExecutorAutoConfiguration {
        @Bean
        @Order(200)
        public RedisTemplateLockExecutor redisTemplateLockExecutor(StringRedisTemplate stringRedisTemplate) {
            return new RedisTemplateLockExecutor(stringRedisTemplate);
        }
    }

    @Configuration
    @ConditionalOnClass(RedissonAutoConfiguration.class)
    static class RedissonExecutorAutoConfiguration {
        @Bean
        @Order(100)
        public RedissonLockExecutor redissonLockExecutor(RedissonClient redissonClient) {
            return new RedissonLockExecutor(redissonClient);
        }
    }

    @Conditional(ZookeeperCondition.class)
    @ConfigurationProperties(prefix = "spring.coordinate.zookeeper")
    @Data
    static class CoordinateConfiguration {

        private String zkServers;

        private int sessionTimeout = 30000;

        private int connectionTimeout = 5000;

        private int baseSleepTimeMs = 1000;

        private int maxRetries = 3;

        @Bean(destroyMethod = "close")
        @Order(300)
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
        public ZookeeperLockExecutor zookeeperLockExecutor(CuratorFramework curatorFramework) {
            return new ZookeeperLockExecutor(curatorFramework);
        }
    }

}
